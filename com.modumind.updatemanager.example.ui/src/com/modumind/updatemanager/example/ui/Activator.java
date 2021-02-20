package com.modumind.updatemanager.example.ui;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.modumind.updatemanager.service.UpdateManager;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.modumind.updatemanager.example.ui"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;
	
	private ServiceTracker<UpdateManager, UpdateManager> updateManagerServiceTracker;
	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		
		updateManagerServiceTracker = new ServiceTracker<UpdateManager, UpdateManager>(context, UpdateManager.class.getName(), null);
		updateManagerServiceTracker.open();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		updateManagerServiceTracker.close();
		
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}
	
	public UpdateManager getUpdateManager() {
		return updateManagerServiceTracker.getService();
	}
}
