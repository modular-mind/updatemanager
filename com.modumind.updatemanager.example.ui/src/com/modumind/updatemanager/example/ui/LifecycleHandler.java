package com.modumind.updatemanager.example.ui;

import org.eclipse.e4.ui.workbench.lifecycle.PostContextCreate;

import com.modumind.updatemanager.service.UpdateManager;

public class LifecycleHandler {

	@PostContextCreate
	public boolean postContextCreate(UpdateManager updateManager) {
		if (!updateManager.performAutoUpdate())
			return false;
		
		return true;
	}
}
