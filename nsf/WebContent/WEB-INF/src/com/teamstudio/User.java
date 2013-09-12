/*
 * Class representing the current application user
 */

package com.teamstudio;

import java.io.Serializable;
import java.util.List;

import lotus.domino.Session;

import com.ibm.designer.runtime.directory.DirectoryUser;
import com.ibm.xsp.designer.context.XSPContext;
import com.ibm.xsp.extlib.util.ExtLibUtil;
import com.teamstudio.unplugged.Configuration;
import com.teamstudio.unplugged.Utils;

import eu.linqed.debugtoolbar.DebugToolbar;

public class User implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private static final String BEAN_NAME = "currentUserBean";
	
	private String userName;
	private String selectedMenuOption;
	
	private boolean isAdmin;
	private boolean isDebug;
	
	private String activeDataSyncLogTab;
	private String activeErrorLogTab;
	private String activeMailLogTab;

	private int accessLevel;
	
	@SuppressWarnings("unchecked")
	public User() {
		
		Session session = null;
		
		try {
			
			session = ExtLibUtil.getCurrentSession();
			
			String currentUser = session.getEffectiveUserName();
			
			if ( this.userName==null || !currentUser.equals(this.userName)  ) {
				
				//different user
				this.userName = currentUser;
				
				this.selectedMenuOption = "dashboard";
				
				this.activeDataSyncLogTab = "tabPanel1";
				this.activeErrorLogTab = "tabPanel1";
				this.activeMailLogTab = "tabPanel1";
				
				XSPContext context = ExtLibUtil.getXspContext();
				DirectoryUser dirUser = context.getUser();
				List<String> roles = dirUser.getRoles();
				
				this.isAdmin = roles.contains( Configuration.ROLE_ADMIN);
				this.isDebug = roles.contains( Configuration.ROLE_DEBUG);
				
				this.accessLevel = ExtLibUtil.getCurrentDatabase().getCurrentAccessLevel();

			}
			
		} catch (Exception e) {
			DebugToolbar.get().error(e);
		}
		
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

	public boolean isAdmin() {
		return isAdmin;
	}

	public boolean isDebug() {
		return isDebug;
	}

	public int getAccessLevel() {
		return accessLevel;
	}
	
}
