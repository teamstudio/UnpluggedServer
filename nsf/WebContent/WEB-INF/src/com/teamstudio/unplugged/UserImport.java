package com.teamstudio.unplugged;

import java.io.File;
import java.io.Serializable;

import com.ibm.xsp.http.IUploadedFile;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import lotus.domino.Agent;
import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.Item;
import lotus.domino.Session;

import com.ibm.xsp.component.UIFileuploadEx.UploadedFile;
import com.ibm.xsp.extlib.util.ExtLibUtil;


import eu.linqed.debugtoolbar.DebugToolbar;

public class UserImport implements Serializable {

	private static final long serialVersionUID = 1L;
	private UploadedFile batchFile;

	private String log;

	public UserImport() {

	}

	@SuppressWarnings("unchecked")
	public boolean processBatchFile() {
		
		DebugToolbar.get().debug("start processing batch file");

		boolean success = false;
		
		Database dbCurrent = null;

		try {

			if (batchFile == null) {
				
				Utils.addErrorMessage("Select a CSV file to process");
				return false;
				
			}

			// batch file uploaded
			IUploadedFile iuploadedFile = batchFile.getUploadedFile();

			String fileName = iuploadedFile.getClientFileName();
			
			
			String extension = fileName.substring(fileName.lastIndexOf(".") + 1);

			String validExtensions = "csv";
			List<String> validExtensionsList = Arrays.asList(validExtensions.split(","));

			// check for a valid extension
			if (!validExtensionsList.contains(extension)) {

				Utils.addErrorMessage("Invalid file extension: only CSV files are supported");
				return false;

			}

			DebugToolbar.get().debug("start processing uploade file " + iuploadedFile.getClientFileName());
			
			File uploadedFile = iuploadedFile.getServerFile();
			
			File correctedFile = new File( uploadedFile.getParentFile().getAbsolutePath() + java.io.File.separator + fileName );
		 	boolean renameSuccess = uploadedFile.renameTo(correctedFile);
		 	
		 	DebugToolbar.get().debug("file renamed?" + success + " to " + correctedFile.getAbsolutePath() );
		 	
		 	if (renameSuccess) {
		 	
				Session session = ExtLibUtil.getCurrentSession();
				dbCurrent = session.getCurrentDatabase();
				Document docPM = dbCurrent.createDocument();
				
				docPM.replaceItemValue("Path", correctedFile.getAbsolutePath());
				
				Agent importAgent = dbCurrent.getAgent("CSVImport");
				importAgent.runWithDocumentContext(docPM);

				Utils.addInfoMessage("Succesfully processed  from batch file");
				DebugToolbar.get().debug("done");
				
				StringBuffer sb = new StringBuffer();
				
				Vector<Item> items = docPM.getItems();
				
				for (Item i : items) {
					
					sb.append(i.getName() + " = ");
					
					switch (i.getType()) {
					
						case lotus.domino.Item.TEXT:
							sb.append( i.getValueString());
							break;
						case lotus.domino.Item.NUMBERS:
							sb.append( i.getValueInteger());
							break;
						case lotus.domino.Item.DATETIMES:
							sb.append( i.getDateTimeValue().getLocalTime() );
							break;
					}
					
					sb.append("<br />");
					
				}
				
				log = sb.toString();

				success = true;
			
				//rename the temporary file back to its original name so it's automatically
			 	//removed from the os' file system.
			 	//we could also do:
			 	//correctedFile.delete()
			 	//but for some reason the Javascript source editor doesn't allow that... 	
			 	correctedFile.renameTo(uploadedFile);
		 	
		 	}
			
			

		} catch (Exception e) {

			DebugToolbar.get().error(e);

		} finally {


		}

		return success;
	}

	// getter/setter for batch files
	public UploadedFile getBatchFile() {
		return this.batchFile;
	}

	public void setBatchFile(UploadedFile to) {
		this.batchFile = to;
	}
	
	public String getLog() {
		return log;
	}
	

}
