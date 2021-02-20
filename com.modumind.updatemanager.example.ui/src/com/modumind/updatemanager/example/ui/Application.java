package com.modumind.updatemanager.example.ui;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import com.modumind.updatemanager.service.UpdateManager;

/**
 * This class controls all aspects of the application's execution
 */
public class Application implements IApplication {

	@Override
	public Object start(IApplicationContext context) throws Exception {
		Display display = PlatformUI.createDisplay();
		try {
			/*
			 * In RCP 3 we have no dependency injection so using a service tracker on the
			 * activator. In RCP 4 you could inject into a life cycle handler but restarting
			 * the workbench is a little tricky.
			 * 
			 * https://stackoverflow.com/questions/28507982/eclipse-rcp-how-to-shutdown-
			 * before-workbench-initializes
			 * 
			 * Would be nice if we had something like this:
			 * 
			 * https://tomsondev.bestsolution.at/2014/11/03/efxclipse-1-1-new-features-api-
			 * to-restart-your-e4-app-on-startup/
			 * 
			 */
			UpdateManager updateManager = Activator.getDefault().getUpdateManager();
			if (updateManager.performAutoUpdate()) {
				System.out.println("Returned IApplication.EXIT_RESTART to framework");
				return IApplication.EXIT_RESTART;
			}
			
			int returnCode = PlatformUI.createAndRunWorkbench(display, new ApplicationWorkbenchAdvisor());
			if (returnCode == PlatformUI.RETURN_RESTART)
				return IApplication.EXIT_RESTART;
			else
				return IApplication.EXIT_OK;
		} finally {
			display.dispose();
		}

	}

	@Override
	public void stop() {
		if (!PlatformUI.isWorkbenchRunning())
			return;
		final IWorkbench workbench = PlatformUI.getWorkbench();
		final Display display = workbench.getDisplay();
		display.syncExec(() -> {
			if (!display.isDisposed())
				workbench.close();
		});
	}
}
