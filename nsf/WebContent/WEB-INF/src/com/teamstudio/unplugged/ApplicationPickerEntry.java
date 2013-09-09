package com.teamstudio.unplugged;

public class ApplicationPickerEntry implements Comparable<ApplicationPickerEntry> {

	private String title;
	private boolean isFolder;
	private String path;
	private String fileName;
	
	public ApplicationPickerEntry(String title, String path, String fileName, boolean isFolder) {
		
		//DebugToolbar.get().debug("adding:" + title + ", " + path + ", " + fileName + ", " + isFolder);
		
		this.title = title;
		this.path = path;
		this.fileName = fileName;
		this.isFolder = isFolder;
		
	}

	public String getTitle() {
		return title;
	}

	public boolean isFolder() {
		return isFolder;
	}

	public String getPath() {
		return path;
	}

	public String getFileName() {
		return fileName;
	}
	
	
	public int compareTo(ApplicationPickerEntry compareEntry ) {
		return new Boolean( compareEntry.isFolder() ).compareTo( new Boolean( this.isFolder ) );
	}
	
}
