package com.teamstudio.unplugged;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import com.ibm.commons.util.StringUtil;
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
	
	/*
	 * Read the value from a DateTime field from a document. Returns 0 if the
	 * field wasn't field/ doesn't contain a date 
	 */
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
	 * Retrieve a document from the current database using either a Note- or Universal ID 
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
	public static Vector objectToVector(Object in) {
		
		Vector result = null;
		
		if (in instanceof String) {
		
			result = new Vector();
			
			if (StringUtil.isNotEmpty( (String) in) ) {
				result.add(in);
			}

		} else if (in instanceof List) {
			
			result = new Vector( (List) in);
		
		} else {

			DebugToolbar.get().warn("unsupported type: " + in.getClass().getCanonicalName(), "objectToVector" );
		
		}

		return result; 
	
	}
	
	/*
	 * Join the elements of a collection into a string
	 */
	@SuppressWarnings("unchecked")
	public static String join( Collection in, String delimiter ) {
		
		StringBuffer sb = new StringBuffer();
		
		Iterator it = in.iterator();
		while( it.hasNext() ) {
			sb.append( it.next() );
			if (it.hasNext()) {
				sb.append(delimiter);
			}
			
		}
		
		return sb.toString();
		
	}
	
	/*
	 * Case insensitve contains function for Lists of String
	 */
	public static boolean containsIgnoreCase(List<String> in, String searchValue) {
		Iterator<String> it = in.iterator();
		while (it.hasNext()) {
			if (it.next().equalsIgnoreCase(searchValue)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Checks if a pager should be visible (has more than 1 page).
	 */
	public static boolean isPagerVisible( com.ibm.xsp.component.xp.XspPager pager ) {
		com.ibm.xsp.component.UIPager.PagerState state = pager.createPagerState();
		return state.getRowCount() > state.getRows(); 
	}
	
	/*
	 * Abbreviates a notes or LDAP name (string with syntax XX=value/XX=value/XX=value
	 */
	public static String getAbbreviatedName(String in) {
		
		if (in.indexOf("/") == -1 || in.indexOf("=") ==-1) {
			return in;
		}
		
		String[] comps = in.split("/");
		StringBuilder sb = new StringBuilder();
		int els = comps.length;
		
		for (int i=0; i<els; i++) {
			sb.append( comps[i].substring( comps[i].indexOf("=")+1 ) );
			if (i<els-1) {
				sb.append("/");
			}
		}
		
		return sb.toString();
	}

	
}
