package com.teamstudio.unplugged;

import java.util.Date;
import java.util.List;
import java.util.Vector;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import com.ibm.xsp.extlib.util.ExtLibUtil;

import eu.linqed.debugtoolbar.DebugToolbar;

import lotus.domino.Base;
import lotus.domino.Database;
import lotus.domino.DateTime;
import lotus.domino.Document;
import lotus.domino.Item;
import lotus.domino.NotesException;

public class Utils {
	
	/**
	 * Resolve a global variable
	 * 
	 * @param varName name of the variable
	 * @return resolved variable
	 */
	public static Object resolveVariable( String varName ) {
		
		FacesContext facesContext = FacesContext.getCurrentInstance();
		
		return facesContext
			.getApplication()
			.getVariableResolver()
			.resolveVariable(facesContext, varName);
		
	}
	
	/**
	 * 
	 * Recycles a set of objects
	 * 
	 * @param dominoObjects one or more Domino object (e.g. NotesDocument, NotesView, ...)
	 */
	public static void recycle(Object... dominoObjects) {
	    for (Object dominoObject : dominoObjects) {
	        if (null != dominoObject) {
	        	
	            if (dominoObject instanceof Base) {
	                try {
	                    ( (Base) dominoObject).recycle();
	                } catch (NotesException ne) { }
	            }
	        }
	    }
	}
	
	/*
	 * Add a standard facesmessage to the current context
	 */
	public static void addInfoMessage( String message) {
		FacesContext.getCurrentInstance().addMessage(null, 
			new FacesMessage( FacesMessage.SEVERITY_INFO, message, message) );
	}
	public static void addErrorMessage( String message) {
		FacesContext.getCurrentInstance().addMessage(null, 
			new FacesMessage( FacesMessage.SEVERITY_ERROR, message, message) );
	}
	public static void addWarningMessage( String message) {
		FacesContext.getCurrentInstance().addMessage(null, 
			new FacesMessage( FacesMessage.SEVERITY_WARN, message, message) );
	}
	
	/*
	 * Read a DateTime value from a document and return it as a java.util.Date
	 */
	public static void setDate( Document doc, String itemName, Date date) {
		
		if (date==null) {
			return;
		}
		
		DateTime dt = null;
		
		try {
			
			dt = ExtLibUtil.getCurrentSession().createDateTime(date);
			doc.replaceItemValue(itemName, dt);
			
		} catch (NotesException e) {
			DebugToolbar.get().debug(e);
		} finally {
			Utils.recycle(dt);
		}
		
	}
	
	public static Date readDate( Document doc, String itemName) {
		
		Item itDate = null;
		Date result = null;
		
		try {
		
			itDate = doc.getFirstItem(itemName);
			
			if (itDate != null) {
				if (itDate.getDateTimeValue() != null) {

					//DateTime dt = itDate.getDateTimeValue();
					//Logger.debug("zolne=" + dt.getTimeZone() );
					//Logger.debug( dt.toString() );
					
					result = itDate.getDateTimeValue().toJavaDate();
				}
			}
		
		} catch (NotesException e) {
			DebugToolbar.get().debug(e);
		} finally {
			Utils.recycle(itDate);
		}
		
		return result;
		
	}
	
	/*
	 * Retrieve a documennt from the current database using either a Note- or Universal ID 
	 */
	public static Document getDocument(String id) {
		
		Database dbCurrent = ExtLibUtil.getCurrentDatabase();
		Document doc = null;
		
		try {
			
			if (id.length()==32) {
				doc = dbCurrent.getDocumentByUNID(id);
			} else {
				doc = dbCurrent.getDocumentByID(id);
			}
			
		} catch (Exception e) {
			DebugToolbar.get().debug(e);
		}
		
		return doc;
		
	}
	
	/*
	 * Function to deal with multi-value fields and bean-bindings
	 */
	@SuppressWarnings("unchecked")
	public static Vector objectToVector(Object in, String t) {
		
		DebugToolbar.get().debug("converting a " + t);
		
		if (in instanceof String) {
			DebugToolbar.get().debug("string");
			
			Vector list = new Vector();
			list.add(in);
			return list;
		} 

		if (in instanceof List) {
			
			DebugToolbar.get().debug("list");
			return (Vector) in;
		}

		DebugToolbar.get().debug("its a..." + in.getClass().getCanonicalName() );
		
		return null;
	}
	
}
