package com.teamstudio.unplugged;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
import java.util.Vector;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import lotus.domino.Item;

import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.Name;
import lotus.domino.View;
import lotus.domino.ViewEntry;
import lotus.domino.ViewEntryCollection;

import com.ibm.commons.util.StringUtil;
import com.ibm.xsp.extlib.util.ExtLibUtil;

import eu.linqed.debugtoolbar.DebugToolbar;

public class User implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private String userName;
	
	private boolean enabled;
	private Vector<String> profiles;
	private boolean syncXPagesEngineLog;
	private boolean canCreateDevices;

	private boolean isNew;

	private String newProfile;

	private String unid;

	private boolean exists;

	private boolean editable;
	
	/*
	 * Constructor called when opening a new or existing User
	 */
	@SuppressWarnings("unchecked")
	public User() {
		
		DebugToolbar.get().debug("init", "User");
		
		isNew = true;
		editable = true;
		
		Document doc = null;
		
		try {
			FacesContext facesContext = FacesContext.getCurrentInstance();
			
			String documentId = null;
			String action = "openDocument";
			
			ExternalContext extCon = facesContext.getExternalContext();
			
			Map requestMap = extCon.getRequestMap();
			
			if (requestMap != null) {
				if ( requestMap.containsKey("documentId") ) {
					documentId = (String) requestMap.get("documentId");
				}
				if ( requestMap.containsKey("action") ) {
					action = (String) requestMap.get("action");
				}
			}
				
			DebugToolbar.get().debug("doc id: " + documentId + ", action " + action);
			
			if (StringUtil.isNotEmpty(documentId) ) {
				
				isNew = false;
				if (action.equals("openDocument")) {
					editable = false;
				}
				
				//retrieve document from database
				doc = Utils.getDocument(documentId);
				
				if (null != doc) {
					
					//read values from document
					unid				= doc.getUniversalID();
					
					profiles			= doc.getItemValue("Profiles");
					userName			= doc.getItemValueString("UserName");
					enabled				= doc.getItemValueString("Active").equals("1");		//1 = active/ enabled
					
					canCreateDevices 	= doc.getItemValueString("CanCreateDevices").equals("1");	//1 = true, 0=false	
					syncXPagesEngineLog	= doc.getItemValueString("DevicePushRepLog").equals("1");	//1 = true, blank = false		

				}
				
		
			} else {
				
				//set defaults for a new User
				isNew = true;
				enabled = true;
				canCreateDevices = true;
				syncXPagesEngineLog = false;
												
			}
			
		} catch (Exception e) {
			
			DebugToolbar.get().error(e);
			
		} finally {
			
			Utils.recycle(doc);
			
		}
		
		
	}
	
	private boolean validate() {
		
		ArrayList<String> errors = new ArrayList<String>();
		
		if (StringUtil.isEmpty(userName) ) {
			errors.add("- select a user or enter a username");
		}

		if (errors.size()>0) {
			Utils.addErrorMessage("Please correct the following errors:<br />" + Utils.join(errors, "<br />"));
		}
		
		return errors.size()==0;
	}
	
	public boolean save() {
		
		Document doc = null;
		boolean success = false;
		
		try {
			
			//perform validations
			if ( !validate() ) {
				return false;
			}
			
			if (isNew) {
				
				doc = ExtLibUtil.getCurrentDatabase().createDocument();
				doc.replaceItemValue("Form", "User");
				
			} else {
				
				doc = Utils.getDocument(unid);
				
			}

			//combine selected profiles and new profile
			if ( StringUtil.isNotEmpty(newProfile) && !Utils.containsIgnoreCase( profiles, newProfile ) ) {
				profiles.add( StringUtil.trim(newProfile) );
			}
			
			doc.replaceItemValue("Profiles", profiles);
			
			//store username and make it a names field
			Name nmUser = doc.getParentDatabase().getParent().createName(userName);
			Item itUserName = doc.replaceItemValue("UserName", nmUser.getCanonical());
			itUserName.setNames(true);
			Utils.recycle(itUserName, nmUser);
			
			doc.replaceItemValue("Active", (enabled ? "1" : "0"));
			
			doc.replaceItemValue("CanCreateDevices", (canCreateDevices ? "1" : "0"));
			
			doc.replaceItemValue("DevicePushRepLog", (syncXPagesEngineLog ? "1" : ""));
			
			success = doc.save();
			
			if (success) {

				Utils.addInfoMessage( "User document for \"" + getAbbreviatedName() + "\" " + (isNew ? "has been created" : "has been updated") );
					
				isNew = false;
			}
			
		} catch (Exception e) {
			
			DebugToolbar.get().error(e);
			
		} finally {
			
			Utils.recycle(doc);
			
		}
		
		return success;
		
	}
	
	
	
	/*
	 * Removes a user document
	 */
	public boolean remove() {
		
		Document doc = null;
		boolean success = false;
		
		try {
			
			doc = Utils.getDocument(unid);
			success = doc.remove(true);
			
			if (success) {
				
				Configuration.get().reloadStatistics();
				
				Utils.addInfoMessage( "User document for \"" + getAbbreviatedName() + "\" has been removed" );
			}
			
		} catch (Exception e) {
			DebugToolbar.get().error(e);
		} finally {
				
			Utils.recycle(doc);
				
		}
		
		return success;
	}
	
	public ArrayList<Application> getApplications() {
		
		View vwApps = null;
		
		try {

			Database dbCurrent = ExtLibUtil.getCurrentDatabase();
			vwApps = dbCurrent.getView("Dbs");
			
			return User.getApplications( getAbbreviatedName(), vwApps, profiles);
			
		} catch (Exception e) {
			DebugToolbar.get().error(e);
		}
	
		return null;
		
	}
	
	/*
	 * Creates a list of all applications a user has
	 */
	@SuppressWarnings("unchecked")
	public static ArrayList<Application> getApplications(String abbreviatedName, View vwApps, Object profiles) {
		
		ArrayList<Application> apps = new ArrayList<Application>();
		
		//DebugToolbar.get().debug("get apps (direct)");
		
		ViewEntry ve = null;
		ViewEntryCollection vec = null;
			
		try {

			vec = vwApps.getAllEntriesByKey( abbreviatedName, true);
			
			//DebugToolbar.get().debug("- found " + vec.getCount() + " for " + abbreviatedName );
			
			ve = vec.getFirstEntry();
			while (null != ve) {
		
				Vector colValues = ve.getColumnValues();
				String server = (String) colValues.get(2);
				String path = (String) colValues.get(3);
				boolean enabled = ((String)colValues.get(1)).indexOf("enabled")>-1;
				
				apps.add( new Application(server, path, enabled, ve.getUniversalID()) );
				
				ViewEntry tmp = vec.getNextEntry(ve);
				ve.recycle();
				ve = tmp;
			}
		
		vec.recycle();

		//DebugToolbar.get().debug("get apps (profile)");
		
		Vector<String> profilesVector = Utils.objectToVector(profiles);
		
		//retrieve all applications for this user based on the assigned profiles
		for (String profile : profilesVector) {
		
			//DebugToolbar.get().debug("get for profile " + profile );
		
			vec = vwApps.getAllEntriesByKey( profile, true);
			
			//DebugToolbar.get().debug("- found " + vec.getCount() + " for " + profile );
			
			ve = vec.getFirstEntry();
			while (null != ve) {
			
				Vector colValues = ve.getColumnValues();
				String server = (String) colValues.get(2);
				String path = (String) colValues.get(3);
				boolean enabled = ((String)colValues.get(1)).indexOf("enabled")>-1;
				
				apps.add( new Application(server, path, enabled, ve.getUniversalID()) );
			
				ViewEntry tmp = vec.getNextEntry(ve);
				ve.recycle();
				ve = tmp;
			}
			
			vec.recycle();
		}
		
		} catch (Exception e) {
			DebugToolbar.get().error(e);
		}
	
		return apps;
		
	}
	
	public boolean isExists() {
		return exists;
	}
	
	public boolean isEditable() {
		return editable;
	}

	public Object getProfiles() {
		return profiles;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	@SuppressWarnings("unchecked")
	public void setProfiles( Object to ) {
		profiles = Utils.objectToVector( to );
	}

	public String getNewProfile() {
		return newProfile;
	}

	public void setNewProfile(String newProfile) {
		this.newProfile = newProfile;
	}

	public String getCanCreateDevices() {
		return new Boolean(canCreateDevices).toString();
	}

	public void setCanCreateDevices(String to) {
		this.canCreateDevices = new Boolean(to);
	}

	/*
	 * Returns a users' abbreviated name
	 */
	public String getAbbreviatedName() {
		
		String abbrName = "";
		
		if (StringUtil.isNotEmpty(userName) ) {
			
			abbrName = Utils.getAbbreviatedName(userName);

		}
		
		return abbrName;
		
	}
	

	public String getSyncXPagesEngineLog() {
		return new Boolean(syncXPagesEngineLog).toString();
	}
	public void setSyncXPagesEngineLog(String to) {
		this.syncXPagesEngineLog = new Boolean(to);
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	public String getEnabled() {
		return new Boolean(enabled).toString();
	}
	public void setEnabled(String to) {
		this.enabled = new Boolean(to);
	}
	
	public boolean isNew() {
		return isNew;
	}
	
	
	
	
}
