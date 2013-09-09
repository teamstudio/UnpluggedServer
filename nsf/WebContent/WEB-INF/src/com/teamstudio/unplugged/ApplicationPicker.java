package com.teamstudio.unplugged;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import com.ibm.commons.util.StringUtil;

import eu.linqed.debugtoolbar.DebugToolbar;

public class ApplicationPicker implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private String dominoDataDir;
	
	public ApplicationPicker() {
		
		dominoDataDir = com.ibm.xsp.model.domino.DominoUtils.getEnvironmentString("Directory") + File.separator;
	}
	
	//retrieve a list of objects representing the contents of a folder
	public ArrayList<ApplicationPickerEntry> getEntries( String folder ) {
		
		ArrayList<ApplicationPickerEntry> entries = new ArrayList<ApplicationPickerEntry>();
		
		try {
			File targetFolder = null;
			
			if (StringUtil.isEmpty(folder) || folder.equals("root")) {
				
				targetFolder = new File( dominoDataDir );
				
			} else {
				
				targetFolder = new File( dominoDataDir + folder );

			}
			
			ArrayList<File> files = new ArrayList<File>(Arrays.asList(targetFolder.listFiles()));
			
			for (File file : files) {
				
				String name = file.getName();
				String ext = name.substring( name.lastIndexOf(".")+1);
				
				String relativePath = file.getAbsolutePath().replace(dominoDataDir, "");
				
				//add only directories and applications
				if (ext.equalsIgnoreCase("nsf") || file.isDirectory()) {
					
					ApplicationPickerEntry entry = new ApplicationPickerEntry( name, relativePath, name, file.isDirectory() );
					
					entries.add( entry );
				}
			
				
			}
			
			//sort the entries
			Collections.sort(entries);
			
		} catch (Exception e) {
			
			DebugToolbar.get().error(e);
			
		}
		
		return entries;
		
		
	}

}
