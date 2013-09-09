package com.teamstudio.unplugged;

import java.io.Serializable;
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
import lotus.domino.Session;

public class Application implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private String unid;
	private boolean isNew;
	
	private String name;
	private String path;
	private String server;
	private boolean exists;
	
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
	
	private static final String NOT_FOUND = "(title not found)";
	
	public Application(String server, String path) {
		
		this.server = server;
		this.path = path;
		
		Database db = null;
		
		try {
			
			name = NOT_FOUND;
			
			Session session = (Session) Utils.resolveVariable("session");
			db = session.getDatabase( (server.indexOf("Current Server") > -1 ? "" : server), path);
			
			if (db != null && db.isOpen() ) {

				name = db.getTitle();
				exists = true;

			}
			
		} catch (Exception e) {
			
			DebugToolbar.get().error(e);
			
		} finally {
			
			Utils.recycle(db);
			
		}
		
	}
	
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
					enabled				= doc.getItemValue("Active").equals("1");
					server				= doc.getItemValueString("Server");
					path				= doc.getItemValueString("Path");
					selectionFormula 	= doc.getItemValueString("ReplFormula");
					sendToAllDevices 	= doc.getItemValueString("ReplLimitByDevice").equals("0");		//all=0, selected=1
					deviceTypesEnabled 	= doc.getItemValue("ReplDevices");			
					sendAttachments 	= doc.getItemValueString("ReplOmitAttach").equals("0");				//0=send, 1=do not send
					attachmentExtensions = doc.getItemValue("ReplAttachmentExts");
					showOnWorkspace 	= !doc.getItemValue("ShowOnWS").equals("no");						//no/ blank
					autoLaunch			= doc.getItemValueString("AutoLaunchApp").equals("yes");			//yes/ blank
					
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
	
	public boolean save() {
		
		Document doc = null;
		boolean success = false;
		
		try {
			
			if (isNew) {
				
				doc = ExtLibUtil.getCurrentDatabase().createDocument();
				doc.replaceItemValue("Form", "UserDatabase");
				
				isNew = false;
			} else {
				
				doc = Utils.getDocument(unid);
				
			}

			doc.replaceItemValue("Profiles", profiles);
			doc.replaceItemValue("UserName", users);
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
			
		} catch (Exception e) {
			
			DebugToolbar.get().error(e);
			
		} finally {
			
			Utils.recycle(doc);
			
		}
		
		return success;
		
	}
	
	/*
	 * Removes an application document
	 */
	public boolean remove() {
		
		Document doc = null;
		boolean success = false;
		
		try {
			
			doc = Utils.getDocument(unid);
			doc.remove(true);
			
		} catch (Exception e) {
				
			DebugToolbar.get().error(e);
				
		} finally {
				
			Utils.recycle(doc);
				
		}
		
		return success;
	}
	
	
	public String getKey() {
		return this.server + "!!" + this.path;
	}

	public String getName() {
		return name;
	}

	public String getPath() {
		DebugToolbar.get().debug("get it: " + path);
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
		profiles = Utils.objectToVector( to, "profiles" );
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
		users = Utils.objectToVector( to, "users" );
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

	public Vector<String> getDeviceTypesEnabled() {
		return deviceTypesEnabled;
	}

	public void setDeviceTypesEnabled(Vector<String> deviceTypesEnabled) {
		this.deviceTypesEnabled = deviceTypesEnabled;
	}

	public String getSendAttachments() {
		return new Boolean(sendAttachments).toString();
	}
	public void setSendAttachments(String to) {
		this.sendAttachments = new Boolean(to);
	}

	public Vector<String> getAttachmentExtensions() {
		return attachmentExtensions;
	}

	public void setAttachmentExtensions(Vector<String> attachmentExtensions) {
		this.attachmentExtensions = attachmentExtensions;
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
		DebugToolbar.get().debug("path set to " + path);
		
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
	
	
}
