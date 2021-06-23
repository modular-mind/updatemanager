package com.modumind.updatemanager.example.ui;

import org.osgi.service.component.annotations.Component;

import com.modumind.updatemanager.service.UpdateManagerLogger;

@Component(service = UpdateManagerLogger.class, immediate = true)
public class ExampleUpdateManagerLogger implements UpdateManagerLogger {

	@Override
	public void log(String message) {
		System.out.println("Logging from example logger: " + message);
	}

	@Override
	public void log(String message, Throwable e) {
		System.out.println("Logging from example logger: " + message);
		e.printStackTrace();		
	}
}
