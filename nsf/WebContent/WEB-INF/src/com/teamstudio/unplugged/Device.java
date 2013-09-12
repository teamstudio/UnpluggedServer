package com.teamstudio.unplugged;

public class Device {

	/*
	 * Figure out the device type based on a user agent string
	 */
	public static String getType(String userAgent) {
		
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
	
	
	public static String getShortId( String id) {
		return "*** " + id.substring( id.length()-4);
	}
	
}
