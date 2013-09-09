package com.teamstudio.unplugged;

import java.io.Serializable;

import javax.faces.context.FacesContext;

import com.ibm.xsp.designer.context.XSPContext;

public class User implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private static final String BEAN_NAME = "currentUserBean";
	
	private String userName;
	private String selectedMenuOption;
	
	private String activeDataSyncLogTab;
	private String activeErrorLogTab;
	private String activeMailLogTab;
	
	private boolean isAdmin;
	
	public User() {
		selectedMenuOption = "dashboard";
		
		activeDataSyncLogTab = "tabPanel1";
		activeErrorLogTab = "tabPanel1";
		activeMailLogTab = "tabPanel1";
		
		/*FacesContext facesContext = FacesContext.getCurrentInstance();
		facesContext.getExternalContext().getU
		
		XSPContext context = XSPContext.getXSPContext();*/
		
		//TODO
		//isAdmin
		
	}

	//return the current bean instance of this class 
	public static User get() {
		return (User) Utils.resolveVariable(BEAN_NAME);
	}

	public String getUserName() {
		return userName;
	}

	public String getSelectedMenuOption() {
		return selectedMenuOption;
	}

	public void setSelectedMenuOption(String selectedMenuOption) {
		this.selectedMenuOption = selectedMenuOption;
	}

	public String getActiveDataSyncLogTab() {
		return activeDataSyncLogTab;
	}

	public void setActiveDataSyncLogTab(String activeDataSyncLogTab) {
		this.activeDataSyncLogTab = activeDataSyncLogTab;
	}

	public String getActiveErrorLogTab() {
		return activeErrorLogTab;
	}

	public void setActiveErrorLogTab(String activeErrorLogTab) {
		this.activeErrorLogTab = activeErrorLogTab;
	}

	public String getActiveMailLogTab() {
		return activeMailLogTab;
	}

	public void setActiveMailLogTab(String activeMailLogTab) {
		this.activeMailLogTab = activeMailLogTab;
	}
	
	

}
