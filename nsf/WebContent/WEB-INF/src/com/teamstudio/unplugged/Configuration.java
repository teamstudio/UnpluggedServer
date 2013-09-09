package com.teamstudio.unplugged;

import java.io.Serializable;
import java.util.HashMap;

import org.openntf.domino.impl.DocumentCollection;

import com.ibm.xsp.extlib.util.ExtLibUtil;

import eu.linqed.debugtoolbar.DebugToolbar;

import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.NotesException;
import lotus.domino.View;
import lotus.domino.ViewEntryCollection;
import lotus.domino.ViewNavigator;

public class Configuration implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private HashMap<String, Application> applications;
	
	private String server;
	private String dbPath;
	private String dbUrl;
	
	private final String APP_VERSION = "v. 2.4.22.374";
	
	private String unpluggedLogPath;
	
	private int numUsers;
	private int numUsersActive;
	
	private int numDevices;
	private int numDevicesActive;
	
	private int licensedUsers;
	
	private int numApps;
	private int numAppsActive;
	
	public Configuration() {
		this.reload();
	}
	
	public String getAppVersion() {
		return APP_VERSION;
	}
	
	public static Configuration get() {
		return (Configuration) Utils.resolveVariable("configBean");
	}
	
	public void reload() {
		
		Database dbCurrent = null;
		View vwStats = null;
		
		try {
			applications = new HashMap<String,Application>();
			
			dbCurrent = (Database) Utils.resolveVariable("database");
			
			server = dbCurrent.getServer();
			dbPath = dbCurrent.getFilePath();
			dbUrl = "/" + dbPath.replaceAll("\\\\", "/");
			
			unpluggedLogPath = "UnpluggedLog.nsf";
			
			reloadStatistics();
			
		} catch (Exception e) {
			DebugToolbar.get().error(e);
		} finally {
			
		}
		
	}
	
	/*
	 * Read statistics to show on the dashboard
	 */
	public void reloadStatistics( ) {
		
		View vwStats = null;
		ViewNavigator nav;
		
		try {
			
			Settings settings = new Settings();
			licensedUsers = settings.getLicenseEnabledUsers();
			
			Database dbCurrent = ExtLibUtil.getCurrentDatabase();
			
			vwStats = dbCurrent.getView("StatsLU");
			//vwStats.refresh();
			
			nav = vwStats.createViewNavFromCategory("User");
			numUsers = nav.getCount();
			nav.recycle();
			nav = vwStats.createViewNavFromCategory("User-1");
			numUsersActive = nav.getCount();
			nav.recycle();
			
			nav = vwStats.createViewNavFromCategory("Device");
			numDevices = nav.getCount();
			nav.recycle();
			nav = vwStats.createViewNavFromCategory("Device-1");
			numDevicesActive = nav.getCount();
			nav.recycle();
			
			nav = vwStats.createViewNavFromCategory("UserDatabase");
			numApps = nav.getCount();
			nav.recycle();
			nav = vwStats.createViewNavFromCategory("UserDatabase-1");
			numAppsActive = nav.getCount();
			nav.recycle();
			
		} catch (Exception e) {
			DebugToolbar.get().error(e);
		} finally {
			
			Utils.recycle(vwStats);
			
		}
		
	}
		
	/*
	 * Toggle the active state of a user, device, application
	 */
	public boolean toggleActive(Document doc) {
		
		boolean success = false;
		
		try {
			
			String to = (doc.getItemValueString("Active").equals("0") ? "1" : "0");
			doc.replaceItemValue("Active", to);
			success = doc.save(); 
			
			this.reloadStatistics();
			
		} catch (Exception e) {
			DebugToolbar.get().error(e);
		}
		
		return success;
	}
	
	/*
	 * Figure out the device type based on a user agent string
	 */
	public String getDeviceType(String userAgent) {
		
		final String TYPE_IOS = "iOS";
		final String TYPE_BLACKBERRY = "Blackberry";
		final String TYPE_ANDROID = "Android";
		final String TYPE_JAVA = "Java";

		String u = userAgent.toLowerCase();
		if ( u.indexOf("dalvik") > -1 ) {
			return TYPE_ANDROID;
		} else if ( u.indexOf("darwin") > -1 ) {
			return TYPE_IOS;
		} else if ( u.indexOf("blackberry") > -1 ) {
			return TYPE_BLACKBERRY;
		} else if ( u.indexOf("java") > -1 ) {
			return TYPE_JAVA;
		} else {
			return "Unknown";
		}
		
	}
	
	//retrieve the name of an application based on it's path
	public String getAppName(String server, String path) {
		
		String key = server + "!!" + path;
		
		if ( !applications.containsKey(key) ) {
			Application app = new Application(server, path);
			applications.put( app.getKey(), app);
		}
		
		return applications.get(key).getName();
		
	}
	
	public int getNumUsers() {
		return numUsers;
	}
	public int getNumUsersActive() {
		return numUsersActive;
	}

	public int getNumDevices() {
		return numDevices;
	}
	public int getNumDevicesActive() {
		return numDevicesActive;
	}
	
	public int getNumApps() {
		return numApps;
	}
	public int getNumAppsActive() {
		return numAppsActive;
	}
	
	public String getDbUrl() {
		return dbUrl;
	}

	public String getUnpluggedLogPath() {
		return unpluggedLogPath;
	}
	
	public int getLicensedUsers() {
		return licensedUsers;
	}
}
