package com.teamstudio.unplugged;

import java.io.Serializable;
import java.util.HashMap;

import com.ibm.xsp.extlib.util.ExtLibUtil;

import eu.linqed.debugtoolbar.DebugToolbar;

import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.Session;
import lotus.domino.View;
import lotus.domino.ViewNavigator;

public class Configuration implements Serializable {

	private static final long serialVersionUID = 1L;
	
	public static final String ROLE_ADMIN = "[Admin]";
	public static final String ROLE_DEBUG = "[Debug]";
	
	private HashMap<String, String> appNames;
	
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
	private int licensesRemaining;
	private int numApps;
	private int numAppsActive;
	private int numProfiles;
	
	public static final String TITLE_NOT_FOUND = "(title not found)";
	
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
	
		try {
			appNames = new HashMap<String,String>();
			
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
			
			//number of profiles
			numProfiles = dbCurrent.getView("Profiles").getColumnValues(0).size();
			
			//remaining licenses
			licensesRemaining = licensedUsers - numDevices;
			
		} catch (Exception e) {
			DebugToolbar.get().error(e);
		} finally {
			
			Utils.recycle(vwStats);
			
		}
		
	}
	
	public boolean toggleActive( String id) {
		
		try {
			Document doc = Utils.getDocument(id);
			
			if (doc==null) {
				throw(new Exception("cannot find document with id " + id ));
			}
			
			return this.toggleActive(doc);
			
		} catch (Exception e) {
			DebugToolbar.get().error(e);
		}
		
		return false;
		
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
	
	//retrieve the name of an application based on it's path
	public String getAppName(String appServer, String appPath) {
		
		String key = appServer + "!!" + appPath;
		
		if ( !appNames.containsKey(key) ) {
			
			if (appServer.toLowerCase().indexOf("current") >-1) {
				appServer = server;
			}
			
			Database db = null;
			String name = TITLE_NOT_FOUND;
			
			try {

				Session session = (Session) Utils.resolveVariable("session");
				db = session.getDatabase( appServer, appPath);
				
				if ( db != null ) {
					name = db.getTitle();
				}
				
			} catch (Exception e) {
				DebugToolbar.get().error(e);
			} finally {
				Utils.recycle(db);
			}
			
			appNames.put( key, name );
		}
		
		return appNames.get(key);
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
	public int getNumProfiles() {
		return numProfiles;
	}
	public int getNumLicensesRemaining() {
		return licensesRemaining;
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
	public int getLicensesRemaining() {
		return licensesRemaining;
	}
	
	public String getServer() {
		return server;
	}
	

}
