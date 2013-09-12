package com.teamstudio.unplugged;

public class ApplicationPickerEntry implements Comparable<ApplicationPickerEntry> {

	private boolean isFolder;
	private String path;
	private String name;

	public ApplicationPickerEntry(String title, String path, String name, boolean isFolder) {
		this.path = path;
		this.name = name;
		this.isFolder = isFolder;
	}
	
	public boolean isFolder() {
		return isFolder;
	}

	public String getPath() {
		return path;
	}
	public String getEscapedPath() {
		return path.replace("\\", "\\\\");
	}
	public String getName() {
		return name;
	}

	public int compareTo(ApplicationPickerEntry compareEntry ) {
		return new Boolean( compareEntry.isFolder() ).compareTo( new Boolean( this.isFolder ) );
	}
	
}
