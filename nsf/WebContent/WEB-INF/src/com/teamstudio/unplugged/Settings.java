package com.teamstudio.unplugged;

import java.io.Serializable;
import java.util.Date;
import java.util.Vector;

import lotus.domino.Database;
import lotus.domino.Document;

import com.ibm.xsp.extlib.util.ExtLibUtil;

import eu.linqed.debugtoolbar.DebugToolbar;

public class Settings implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private boolean isEditable;
	
	private String licenseKey;
	private int licenseEnabledUsers;
	private Date licenseExpirationDate;
	
	private Vector<String> licensedServer;
	
	private int maxResponseSize;
	
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
			
			licenseKey = docSettings.getItemValueString("SKILN");
			licenseEnabledUsers = docSettings.getItemValueInteger("LicensedUsers");
			
			licenseExpirationDate = Utils.readDate(docSettings, "Expires");
			
			licensedServer = docSettings.getItemValue("LicensedServers");
			
			maxResponseSize = docSettings.getItemValueInteger("MaxRespSize");
			
			logLogin = docSettings.getItemValueString("LogOpts_Login");
			logLoginTypes = docSettings.getItemValue("LogTypes_Login");
			
			
			logPush = docSettings.getItemValueString("LogOpts_Push");
			logPushTypes = docSettings.getItemValue("LogTypes_Push");
			logPushNotifyUser = docSettings.getItemValueString("RepConfOpts_Push").equals("1");
			
			logPull = docSettings.getItemValueString("LogOpts_Pull");
			logPullTypes = docSettings.getItemValue("LogTypes_Pull");
			logPullCopyNoteOnFailure = docSettings.getItemValueString("LogErrorNote").equals("1");
			
			logMail = docSettings.getItemValueString("LogOpts_Mail");
			
			
		} catch (Exception e) {
			DebugToolbar.get().error(e);
		} finally {
			
			Utils.recycle(docSettings);
		}
		
		
	}
	
	/*
	 * Save the settings back to the database
	 */
	public boolean save() {
		
		boolean saved = false;
		
		Database dbCurrent = null;
		Document docSettings = null;
		
		try {
			
			dbCurrent = ExtLibUtil.getCurrentDatabase();
			docSettings = dbCurrent.getProfileDocument("(settings)", "(unpluggedconfig)");
			
			docSettings.replaceItemValue("LicensedServers", licensedServer);
			
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
			
			//reload the configuration bean (we read information from it on the dashboard
			Configuration.get().reload();
			
			
		} catch (Exception e) {
			
			DebugToolbar.get().error(e);
			
		} finally {
			
			Utils.recycle(docSettings);
		}
		
		return saved;
		
	}

	public String getLicenseKey() {
		return licenseKey;
	}

	public void setLicenseKey(String licenseNumber) {
		this.licenseKey = licenseNumber;
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

	public Vector<String> getLicensedServer() {
		return licensedServer;
	}

	public void setLicensedServer(Vector<String> licensedServer) {
		this.licensedServer = licensedServer;
	}

	public int getMaxResponseSize() {
		return maxResponseSize;
	}

	public void setMaxResponseSize(int maxResponseSize) {
		this.maxResponseSize = maxResponseSize;
	}

	public String getLogLogin() {
		return logLogin;
	}

	public void setLogLogin(String logLogin) {
		this.logLogin = logLogin;
	}

	public Vector<String> getLogLoginTypes() {
		return logLoginTypes;
	}

	public void setLogLoginTypes(Vector<String> logLoginTypes) {
		this.logLoginTypes = logLoginTypes;
	}

	public String getLogPush() {
		return logPush;
	}

	public void setLogPush(String logPush) {
		this.logPush = logPush;
	}

	public Vector<String> getLogPushTypes() {
		return logPushTypes;
	}

	public void setLogPushTypes(Vector<String> logPushTypes) {
		this.logPushTypes = logPushTypes;
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

	public Vector<String> getLogPullTypes() {
		return logPullTypes;
	}

	public void setLogPullTypes(Vector<String> logPullTypes) {
		this.logPullTypes = logPullTypes;
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
	
}
