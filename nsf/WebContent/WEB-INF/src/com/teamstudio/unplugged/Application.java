package com.teamstudio.unplugged;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Vector;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import com.ibm.commons.util.StringUtil;
import com.ibm.xsp.extlib.util.ExtLibUtil;

import eu.linqed.debugtoolbar.DebugToolbar;
import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.Item;
import lotus.domino.Name;
import lotus.domino.Session;

public class Application implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private String unid;
	private boolean isNew;
	
	private String path;
	private String server;
	private boolean exists;
	private String name;
	
	private Vector<String> profiles;
	private String newProfile;
	
	private Vector<String> users;
	private boolean enabled;
	private String selectionFormula;
	private boolean sendToAllDevices;		
	private Vector<String> deviceTypesEnabled;
	private boolean sendAttachments;
	private Vector<String> attachmentExtensions;
	private boolean showOnWorkspace;
	private boolean autoLaunch;
	
	private boolean editable;
	
	/*
	 * Constructor called when opening a new or existing application
	 */
	@SuppressWarnings("unchecked")
	public Application() {
		
		DebugToolbar.get().debug("init", "Application");
		
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
					users				= doc.getItemValue("UserName");
					enabled				= doc.getItemValueString("Active").equals("1");
					server				= doc.getItemValueString("Server");
					path				= doc.getItemValueString("Path");
					selectionFormula 	= doc.getItemValueString("ReplFormula");
					sendToAllDevices 	= doc.getItemValueString("ReplLimitByDevice").equals("0");		//all=0, selected=1
					deviceTypesEnabled 	= doc.getItemValue("ReplDevices");			
					sendAttachments 	= doc.getItemValueString("ReplOmitAttach").equals("0");			//0=send, 1=do not send
					attachmentExtensions = doc.getItemValue("ReplAttachmentExts");
					showOnWorkspace 	= !doc.getItemValueString("ShowOnWS").equals("no");				//no/ blank
					autoLaunch			= doc.getItemValueString("AutoLaunchApp").equals("yes");		//yes/ blank
					
				}
				
		
			} else {
				
				//set defaults for a new application
				isNew = true;
				enabled = true;
				sendAttachments = true;
				
				String[] defaultExtensions = {"jpg", "gif", "png"};
				attachmentExtensions = new Vector<String>(Arrays.asList(defaultExtensions));
				
				sendToAllDevices = true;
				String[] defaultDevices = {"3", "2", "1"};
				deviceTypesEnabled = new Vector<String>(Arrays.asList(defaultDevices));
				
				showOnWorkspace = true;
				autoLaunch = false;
				
			}
			
		} catch (Exception e) {
			
			DebugToolbar.get().error(e);
			
		} finally {
			
			Utils.recycle(doc);
			
		}
		
		
	}
	
	public Application(String server, String path, boolean enabled, String unid) {
		this.server = server;
		this.path = path;
		this.enabled = enabled;
		this.unid = unid;
	}

	private boolean validate() {
		
		ArrayList<String> errors = new ArrayList<String>();
		
		if (StringUtil.isEmpty(path) ) {
			errors.add("- enter the path to the application");
		}

		if (users.size()==0 && profiles.size()==0) {
			errors.add("- assign this application to at least one user or profile");
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
				doc.replaceItemValue("Form", "UserDatabase");
				
			} else {
				
				doc = Utils.getDocument(unid);
				
			}

			//combine selected profiles and new profile
			if ( StringUtil.isNotEmpty(newProfile) && !Utils.containsIgnoreCase( profiles, newProfile ) ) {
				profiles.add( StringUtil.trim(newProfile) );
			}
			
			doc.replaceItemValue("Profiles", profiles);
			
			//set username and make it a names field
			Item itUserName = doc.replaceItemValue("UserName", users);
			itUserName.setNames(true);
			itUserName.recycle();
			
			doc.replaceItemValue("Active", (enabled ? "1" : "0"));
			doc.replaceItemValue("Server", server);
			doc.replaceItemValue("Path", path);
			doc.replaceItemValue("ReplFormula", selectionFormula);
			
			doc.replaceItemValue("ReplLimitByDevice", (sendToAllDevices ? "0" : "1"));
			doc.replaceItemValue("ReplDevices", deviceTypesEnabled);
			
			doc.replaceItemValue("ReplOmitAttach", (sendAttachments ? "0" : "1"));
			doc.replaceItemValue("ReplAttachmentExts", attachmentExtensions);
			
			doc.replaceItemValue("ShowOnWS", (showOnWorkspace ? "" : "no"));
			doc.replaceItemValue("AutoLaunchApp", (autoLaunch ? "yes" : ""));
			
			success = doc.save();
			
			if (success) {
				
				String appName = "";
				if ( !this.getName().equals( Configuration.TITLE_NOT_FOUND ) ) {
					appName = "for \"" + this.getName() + "\" ";
				}
				
				if (!isExistingApplication()) {
					
					Utils.addWarningMessage("Application document saved. The application you specified (" + path + ") could not be found on the current server");
				
				} else {
					
					Utils.addInfoMessage( "Application document " + appName + (isNew ? "has been created" : "has been updated") );
					
				}
				
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
	 * Checks if this application exists (on the current server only)
	 */
	private boolean isExistingApplication() {
		
		Database dbTarget = null;
		boolean exists = true;
		
		try {
			Session session = ExtLibUtil.getCurrentSession();
			String canonicalServerName = "";
			
			if ( !StringUtil.isEmpty(server) ) {
				Name nmServer = session.createName(server);
				canonicalServerName = nmServer.getCanonical();
				Utils.recycle(nmServer);
			}
			
			if ( StringUtil.isNotEmpty(server) && !canonicalServerName.equalsIgnoreCase(Configuration.get().getServer() ) ) {
				//different server: can't check if it exists: abort
				return exists;
			}
			
			//try to open the database
			dbTarget = session.getDatabase(null, path, false);
				
			if (dbTarget == null) {
				exists = false;
			}
			
		} catch (Exception e) {
			DebugToolbar.get().error(e);
		} finally {
			Utils.recycle(dbTarget);
		}
			
		return exists;
	}
	
	/*
	 * Removes an application document
	 */
	public boolean remove() {
		
		Document doc = null;
		boolean success = false;
		
		try {
			
			doc = Utils.getDocument(unid);
			success = doc.remove(true);
			
			if (success) {
				
				String appName = "";
				if ( !this.getName().equals( Configuration.TITLE_NOT_FOUND ) ) {
					appName = "for \"" + this.getName() + "\" ";
				}
				
				Configuration.get().reloadStatistics();
				
				Utils.addInfoMessage( "Application document" + appName + "has been removed" );
			}
			
		} catch (Exception e) {
			DebugToolbar.get().error(e);
		} finally {
				
			Utils.recycle(doc);
				
		}
		
		return success;
	}
	
	
	public String getName() {
		if (name == null) {
			name = Configuration.get().getAppName(server, path);
		}
		return name;
	}

	public String getPath() {
		return path;
	}

	public String getServer() {
		return server;
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

	public Object getUsers() {
		return users;
	}

	@SuppressWarnings("unchecked")
	public void setUsers( Object to) {
		users = Utils.objectToVector( to );
	}

	public String getSelectionFormula() {
		return selectionFormula;
	}

	public void setSelectionFormula(String selectionFormula) {
		this.selectionFormula = selectionFormula;
	}

	public String getSendToAllDevices() {
		return new Boolean(sendToAllDevices).toString();
	}

	public void setSendToAllDevices(String to) {
		this.sendToAllDevices = new Boolean(to);
	}

	public Object getDeviceTypesEnabled() {
		return deviceTypesEnabled;
	}

	@SuppressWarnings("unchecked")
	public void setDeviceTypesEnabled( Object to) {
		deviceTypesEnabled = Utils.objectToVector( to );
	}

	public String getSendAttachments() {
		return new Boolean(sendAttachments).toString();
	}
	public void setSendAttachments(String to) {
		this.sendAttachments = new Boolean(to);
	}

	public Object getAttachmentExtensions() {
		return attachmentExtensions;
	}
	@SuppressWarnings("unchecked")
	public void setAttachmentExtensions( Object to) {
		this.attachmentExtensions = Utils.objectToVector( to );
	}

	public String getShowOnWorkspace() {
		return new Boolean(showOnWorkspace).toString();
	}
	public void setShowOnWorkspace(String to) {
		this.showOnWorkspace = new Boolean(to);
	}

	public String getAutoLaunch() {
		return new Boolean(autoLaunch).toString();
	}

	public void setAutoLaunch(String to) {
		this.autoLaunch = new Boolean(to);
	}

	public void setPath(String path) {
		this.path = path;
	}

	public void setServer(String server) {
		this.server = server;
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
	
	public String getUnid() {
		return unid;
	}
	
}
