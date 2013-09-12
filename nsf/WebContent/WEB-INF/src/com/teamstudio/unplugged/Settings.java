package com.teamstudio.unplugged;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;

import lotus.domino.Name;
import lotus.domino.Item;
import lotus.domino.Agent;
import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.Session;

import com.ibm.commons.util.StringUtil;
import com.ibm.xsp.extlib.util.ExtLibUtil;

import eu.linqed.debugtoolbar.DebugToolbar;

public class Settings implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private boolean isEditable;
	
	private String licenseNumber;
	private String licenseKey;
	
	private String licenseNumber_in;
	private String licenseKey_in;
	
	private int licenseEnabledUsers;
	private Date licenseExpirationDate;
	boolean editLicense;
	
	private Vector<String> licensedServer;
	
	private Double maxResponseSize;
	
	private String logLogin;
	private Vector<String> logLoginTypes;
	
	private String logPush;
	private Vector<String> logPushTypes;
	private boolean logPushNotifyUser;
	
	private String logPull;
	private Vector<String> logPullTypes;
	private boolean logPullCopyNoteOnFailure;
	
	private String logMail;

	/*
	 * Read all values from the local database 
	 */
	@SuppressWarnings("unchecked")
	public Settings() {
		
		Database dbCurrent = null;
		Document docSettings = null;
		
		isEditable = false;
		
		try {
			
			dbCurrent = ExtLibUtil.getCurrentDatabase();
			docSettings = dbCurrent.getProfileDocument("(settings)", "(unpluggedconfig)");
			
			licensedServer = docSettings.getItemValue("LicensedServers");
			
			maxResponseSize = docSettings.getItemValueDouble("MaxRespSize");
			
			logLogin = docSettings.getItemValueString("LogOpts_Login");
			logLoginTypes = docSettings.getItemValue("LogTypes_Login");
			
			logPush = docSettings.getItemValueString("LogOpts_Push");
			logPushTypes = docSettings.getItemValue("LogTypes_Push");
			logPushNotifyUser = docSettings.getItemValueString("RepConfOpts_Push").equals("1");
			
			logPull = docSettings.getItemValueString("LogOpts_Pull");
			logPullTypes = docSettings.getItemValue("LogTypes_Pull");
			logPullCopyNoteOnFailure = docSettings.getItemValueString("LogErrorNote").equals("1");
			
			logMail = docSettings.getItemValueString("LogOpts_Mail");
			
			//retrieve license settings
			Document docPM = dbCurrent.createDocument();
			Agent settingsAgent = dbCurrent.getAgent("websettings");
			
			settingsAgent.runWithDocumentContext(docPM);
			
			readLicenseSettings(docPM);
			
			
		} catch (Exception e) {
			DebugToolbar.get().error(e);
		} finally {
			
			Utils.recycle(docSettings);
		}
		
		
	}
	
	private void readLicenseSettings(Document docPM) {
		
		try {
			
			licenseNumber = docPM.getItemValueString("LicenseNumber");
			licenseKey = docPM.getItemValueString("Key");
			licenseExpirationDate = Utils.readDate(docPM, "Expires");
			
			//fields use to check if the license settings have changed... 
			licenseNumber_in = docPM.getItemValueString("LicenseNumber");
			licenseKey_in = docPM.getItemValueString("Key");
			
			licenseEnabledUsers = docPM.getItemValueInteger("LicensedUsers");
			
		} catch (Exception e) {
			
			DebugToolbar.get().error(e);
		
		}
		
	}
	
	private boolean validate() {
		
		ArrayList<String> errors = new ArrayList<String>();
		
		if (editLicense) {
			
			if (StringUtil.isEmpty(licenseKey)) {
				errors.add("- enter a license key");
			}
			
			if (StringUtil.isEmpty(licenseNumber)) {
				errors.add("- enter a license number");
			}

		}
		
		if ( licensedServer.size() ==0 ) {
			errors.add("- enter the server(s) where Unplugged may run");
		}
		
		if (errors.size()>0) {
			Utils.addErrorMessage("Please correct the following errors:<br />" + Utils.join(errors, "<br />"));
		}
		
		return errors.size()==0;
	}
	
	/*
	 * Save the settings back to the database
	 */
	public boolean save() {
		
		boolean saved = false;
		
		boolean licenseUpdated = false;
		
		Database dbCurrent = null;
		Document docSettings = null;
		Document docPM = null;
		Agent settingsAgent = null;
		
		try {
			
			//perform validations
			if ( !validate() ) {
				return false;
			}
			
			Session session = ExtLibUtil.getCurrentSession();
			dbCurrent = session.getCurrentDatabase();
			
			if (editLicense) {
				
				if ( !licenseKey.equals(licenseKey_in) || !licenseNumber.equals( licenseNumber_in) ) {
					
					//license changed - store new license...
					docPM = dbCurrent.createDocument();
					settingsAgent = dbCurrent.getAgent("websettings");
					
					docPM.replaceItemValue("NewKey", licenseKey);
					docPM.replaceItemValue("NewLicenseNumber", licenseNumber);
					
					settingsAgent.runWithDocumentContext(docPM);

					//check for errors and read updated settings
					if (docPM.getItemValueString("Status").equals("Invalid") ) {
						
						Utils.addErrorMessage("The license key and/or number you have entered are not correct. Settings not saved.");
						return false;
						
					} else {
						
						licenseUpdated = true;
					}
					
					readLicenseSettings(docPM);
					
				}
				
				editLicense = false;
			}
			
			
			//convert server names to canonical form
			docSettings = dbCurrent.getProfileDocument("(settings)", "(unpluggedconfig)");
			
			Vector<String> licensedServersCanonical = new Vector<String>();
			for (String serverName : licensedServer) {
				Name n = session.createName(serverName);
				licensedServersCanonical.add( n.getCanonical());
				n.recycle();
			}
			
			Item s = docSettings.replaceItemValue("LicensedServers", licensedServersCanonical);
			s.setNames(true);
			Utils.recycle(s);
			
			docSettings.replaceItemValue("MaxRespSize", maxResponseSize);
			
			docSettings.replaceItemValue("LogOpts_Login", logLogin);
			docSettings.replaceItemValue("LogTypes_Login", logLoginTypes);
			
			docSettings.replaceItemValue("LogOpts_Push", logPush );
			docSettings.replaceItemValue("LogTypes_Push", logPushTypes );
			docSettings.replaceItemValue("RepConfOpts_Push", (logPushNotifyUser ? "1" : "0") );
			
			docSettings.replaceItemValue("LogOpts_Pull", logPull);
			docSettings.replaceItemValue("LogTypes_Pull", logPullTypes);
			docSettings.replaceItemValue("LogErrorNote", (logPullCopyNoteOnFailure ? "1" : "0" ) );
			
			docSettings.replaceItemValue("LogOpts_Mail", logMail);
			
			Utils.setDate(docSettings, "UISaved", new Date() );
			
			docSettings.computeWithForm(false, false);
		
			saved = docSettings.save();
			
			if (saved) {
				
				if (licenseUpdated ) {
					Utils.addInfoMessage( "License has been updated. Settings have been saved" );
				} else {
					Utils.addInfoMessage( "Settings have been saved" );
				}
				
				//reload the configuration bean (we read information from it on the dashboard)
				Configuration.get().reload();
				
			}
			
			
		} catch (Exception e) {
			
			DebugToolbar.get().error(e);
		} finally {
			
			Utils.recycle(docSettings, settingsAgent, docPM);
		}
		
		return saved;
		
	}

	public String getLicenseKey() {
		return licenseKey;
	}
	public void setLicenseKey(String to) {
		this.licenseKey = to;
	}
	
	public String getLicenseNumber() {
		return licenseNumber;
	}
	public void setLicenseNumber(String to) {
		this.licenseNumber = to;
	}

	public int getLicenseEnabledUsers() {
		return licenseEnabledUsers;
	}

	public void setLicenseEnabledUsers(int licenseEnabledUsers) {
		this.licenseEnabledUsers = licenseEnabledUsers;
	}

	public Date getLicenseExpirationDate() {
		return licenseExpirationDate;
	}

	public void setLicenseExpirationDate(Date licenseExpirationDate) {
		this.licenseExpirationDate = licenseExpirationDate;
	}

	public Object getLicensedServer() {
		return licensedServer;
	}

	@SuppressWarnings("unchecked")
	public void setLicensedServer(Object licensedServer) {
		this.licensedServer = Utils.objectToVector( licensedServer);
	}

	public Double getMaxResponseSize() {
		return Double.valueOf(maxResponseSize);
	}
	public void setMaxResponseSize(Double maxResponseSize) {
		this.maxResponseSize = maxResponseSize;
	}

	public String getLogLogin() {
		return logLogin;
	}

	public void setLogLogin(String logLogin) {
		this.logLogin = logLogin;
	}

	public Object getLogLoginTypes() {
		return logLoginTypes;
	}

	@SuppressWarnings("unchecked")
	public void setLogLoginTypes( Object logLoginTypes) {
		this.logLoginTypes = Utils.objectToVector( logLoginTypes );;
	}

	public String getLogPush() {
		return logPush;
	}

	public void setLogPush(String logPush) {
		this.logPush = logPush;
	}

	public Object getLogPushTypes() {
		return logPushTypes;
	}

	@SuppressWarnings("unchecked")
	public void setLogPushTypes(Object logPushTypes) {
		this.logPushTypes = Utils.objectToVector( logPushTypes );
	}

	public String getLogPushNotifyUser() {
		return (logPushNotifyUser ? "1" : "0");
	}

	public void setLogPushNotifyUser(String logPushNotifyUser) {
		this.logPushNotifyUser = (logPushNotifyUser.equals("1") ? true : false);
	}

	public String getLogPull() {
		return logPull;
	}

	public void setLogPull(String logPull) {
		this.logPull = logPull;
	}

	public Object getLogPullTypes() {
		return logPullTypes;
	}
	@SuppressWarnings("unchecked")
	public void setLogPullTypes(Object logPullTypes) {
		this.logPullTypes = Utils.objectToVector( logPullTypes );;
	}

	public String getLogPullCopyNoteOnFailure() {
		return (logPullCopyNoteOnFailure ? "1" : "0");
	}

	public void setLogPullCopyNoteOnFailure(String logPullCopyNoteOnFailure) {
		this.logPullCopyNoteOnFailure = (logPullCopyNoteOnFailure.equals("1") ? true : false);
	}

	public String getLogMail() {
		return logMail;
	}

	public void setLogMail(String logMail) {
		this.logMail = logMail;
	}

	public boolean isEditable() {
		return isEditable;
	}

	public void setEditable(boolean isEditable) {
		this.isEditable = isEditable;
	}

	public boolean isEditLicense() {
		return editLicense;
	}

	public void setEditLicense(boolean editLicense) {
		this.editLicense = editLicense;
	}
	
}
