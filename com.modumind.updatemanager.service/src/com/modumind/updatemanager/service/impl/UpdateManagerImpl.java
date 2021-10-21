/*******************************************************************************
 * Copyright (c) 2021, Modular Mind, Ltd.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Patrick Paulin - initial API and implementation
 *******************************************************************************/
package com.modumind.updatemanager.service.impl;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.InstallOperation;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.Update;
import org.eclipse.equinox.p2.operations.UpdateOperation;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.prefs.BackingStoreException;

import com.modumind.updatemanager.service.UpdateManager;
import com.modumind.updatemanager.service.UpdateManagerInstallFilter;
import com.modumind.updatemanager.service.UpdateManagerLogger;
import com.modumind.updatemanager.service.UpdateManagerRepositoryLocator;

@Component(service = UpdateManager.class, immediate = true)
public class UpdateManagerImpl implements UpdateManager {

	private static final String JUSTUPDATED = "justUpdated";
	private static final String PLUGIN_ID = "com.modumind.updatemanager.service";
	private static final String REPOSITORY_ARG_NAME = "repository";
	
	private IMetadataRepository metadataRepository = null;
	private IProvisioningAgentProvider provisioingAgentProvider = null;
	private UpdateManagerInstallFilter installFilter = null;
	private UpdateManagerRepositoryLocator repositoryLocator = null;
	private UpdateManagerLogger logger = null;
	
	/* Binding method for Declarative Services */

	@Reference
	protected void setProvisioningAgentProvider(IProvisioningAgentProvider provisioningAgentProvider) {
		this.provisioingAgentProvider = provisioningAgentProvider;
	}

	@Reference(cardinality=ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY)
	protected void setUpdateManagerInstallFilter(UpdateManagerInstallFilter installFilter) {
		this.installFilter = installFilter;
	}
	
	@Reference(cardinality=ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY)
	protected void setUpdateManagerRepositoryLocator(UpdateManagerRepositoryLocator repositoryLocator) {
		this.repositoryLocator = repositoryLocator;
	}

	@Reference(cardinality=ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY)
	protected void setUpdateManagerLogger(UpdateManagerLogger logger) {
		this.logger = logger;
	}

	/* Public methods */

	@Override
	public boolean performAutoUpdate() {
		this.log("Starting P2 auto-update process");

		/*
		 * Check to make sure that we are not restarting after an update. If we are,
		 * then there is no need to check for updates again.
		 */
		final IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(PLUGIN_ID);
		if (preferences.getBoolean(JUSTUPDATED, false)) {
			setJustUpdated(false);
			this.log("Restarting after auto update, skipping auto update");
			return false;
		}

		/*
		 * Get access to the profile to be updated and create a provisioning session.
		 */
		IProvisioningAgent agent = null;
		try {
			agent = this.provisioingAgentProvider.createAgent(null);
		} catch (ProvisionException e) {
			this.log("Error creating provisioning agent - " + e.getMessage(), e);
			return false;
		}

		this.log("Provisioning agent created");

		IProfileRegistry profileRegistry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
		if (profileRegistry == null) {
			this.log("Could not locate the Profile Registry, ending auto update process");
			return false;
		}

		IProfile profile = profileRegistry.getProfile(IProfileRegistry.SELF);
		if (profile == null) {
			this.log("No profile found for this installation, ending auto update process. Profiles are not available when running in Eclipse.");
			return false;
		}

		ProvisioningSession session = new ProvisioningSession(agent);
		this.log("Provisioning session created");

		/*
		 * Create the jobs to update and install features.
		 */
		final ProvisioningJob updateJob = getUpdateJob(session, profile);
		final ProvisioningJob installJob = getInstallJob(session, profile);

		/*
		 * Create a runnable to execute the update. We'll show a dialog during the
		 * process and then return when the runnable is complete.
		 */
		final boolean[] restartRequired = new boolean[] { false };
		IRunnableWithProgress runnable = new IRunnableWithProgress() {

			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				SubMonitor subMon = SubMonitor.convert(monitor, 200);
				IStatus status = null;

				if (updateJob != null) {
					status = updateJob.runModal(subMon.newChild(100));

					dumpStatus(status);
				} else {
					subMon.worked(100);
				}

				if (updateJob != null) {
					if (status.getSeverity() != IStatus.ERROR) {
						setJustUpdated(true);
						restartRequired[0] = true;
						log("Updates installed, restart required");
					} else {
						log("Update failed, skipping installation step");
						log(status.getException().getMessage(), status.getException());
						return;
					}
				}

				if (installJob != null) {
					status = installJob.runModal(subMon.newChild(100));

					dumpStatus(status);
				} else {
					subMon.worked(100);
				}

				if (installJob != null) {
					if (status.getSeverity() != IStatus.ERROR) {
						setJustUpdated(true);
						restartRequired[0] = true;
						log("New artifacts installed, restart required");
					} else {
						log("Install failed");
						log(status.getException().getMessage(), status.getException());
					}
				}
			}
		};

		/*
		 * Execute the runnable and wait for it to complete.
		 */
		try {
			new ProgressMonitorDialog(null).run(true, false, runnable);
			return restartRequired[0];
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
		} finally {
			agent.stop();
		}

		return false;
	}

	/* Private methods */
	
	/**
	 * Load the p2 repository. This should only be done once as the operation is
	 * expensive.
	 * 
	 * Note that only the metadata repository is saved as this is all we need to do
	 * the provisioning operations. Other use cases might require storing the
	 * artifact repository as well.
	 * 
	 * @param provisioningAgent
	 * @return whether the p2 repository was loaded successfully
	 */
	private IStatus loadP2Repository(IProvisioningAgent provisioningAgent) {
		if (metadataRepository == null) {
			String repositoryUri = getRepositoryUri();
			if (repositoryUri == null || repositoryUri.isEmpty())
				return new Status(Status.ERROR, PLUGIN_ID,
						"No repository specified. Add -D" + REPOSITORY_ARG_NAME + " to your INI file.");

			IMetadataRepositoryManager manager = (IMetadataRepositoryManager) provisioningAgent
					.getService(IMetadataRepositoryManager.SERVICE_NAME);

			try {
				metadataRepository = manager.loadRepository(new URI(repositoryUri), new NullProgressMonitor());
			} catch (Exception e) {
				this.log("Failed to load metadata repository at location " + repositoryUri, e);
				return new Status(Status.ERROR, PLUGIN_ID, e.getMessage(), e);
			}

			this.log("Metadata repository loaded: " + repositoryUri);

			IArtifactRepositoryManager artifactManager = (IArtifactRepositoryManager) provisioningAgent
					.getService(IArtifactRepositoryManager.SERVICE_NAME);
			try {
				/* Loading repository but not saving a reference */
				artifactManager.loadRepository(new URI(repositoryUri), new NullProgressMonitor());
			} catch (Exception e) {
				this.log("Failed to load artifact repository at location " + repositoryUri);
				return new Status(Status.ERROR, PLUGIN_ID, e.getMessage(), e);
			}

			this.log("Artifact repository loaded: " + repositoryUri);
		}

		return Status.OK_STATUS;
	}

	private ProvisioningJob getUpdateJob(ProvisioningSession provisioningSession, IProfile profile) {
		IStatus loadStatus;

		if ((loadStatus = loadP2Repository(provisioningSession.getProvisioningAgent())) != Status.OK_STATUS) {
			dumpStatus(loadStatus);
			return null;
		}

		UpdateOperation updateOperation = new UpdateOperation(provisioningSession);
		IStatus updateOperationStatus = updateOperation.resolveModal(new NullProgressMonitor());

		if (updateOperationStatus.getCode() == UpdateOperation.STATUS_NOTHING_TO_UPDATE) {
			this.log("Nothing to update");
			return null;
		}

		if (updateOperation.hasResolved()) {
			this.log("Update operation resolved successfully");

			Update[] possibleUpdates = updateOperation.getPossibleUpdates();

			this.log("Possible updates:");

			for (Update update : possibleUpdates) {
				this.log(" - " + update);
			}

			return updateOperation.getProvisioningJob(null);
		}

		this.log("Update operation not resolved.");
		dumpStatus(updateOperationStatus);

		return null;
	}

	private ProvisioningJob getInstallJob(ProvisioningSession provisioningSession, IProfile profile) {
		IStatus loadStatus;

		if ((loadStatus = loadP2Repository(provisioningSession.getProvisioningAgent())) != Status.OK_STATUS) {
			dumpStatus(loadStatus);
			return null;
		}

		/*
		 * Query the metadata repository for the latest features to install.
		 */
		Collection<IInstallableUnit> iusInRepo = metadataRepository
				.query(QueryUtil.createLatestIUQuery(), new NullProgressMonitor()).toUnmodifiableSet();

		List<IInstallableUnit> iusToInstall = new ArrayList<IInstallableUnit>();
		this.log("IUs to install (possibly " + iusInRepo.size() + "):");

		/*
		 * Search for IUs that end in feature.feature.group and that are not in the
		 * current profile.
		 */
		for (IInstallableUnit iu : iusInRepo) {
			if (iu.getId().endsWith(".feature.feature.group")) {
				if (profile.query(QueryUtil.createIUQuery(iu.getId()), null).isEmpty()) {
					this.log("Found IU to install: " + iu.getId());
					if (shouldFeatureBeInstalled(iu))
						iusToInstall.add(iu);
				}
			}
		}

		if (iusToInstall.size() == 0) {
			this.log(" - nothing to install");
			return null;
		}

		InstallOperation operation = new InstallOperation(provisioningSession, iusToInstall);
		IStatus status = operation.resolveModal(new NullProgressMonitor());

		if (operation.hasResolved()) {
			this.log("Install operation resolved successfully");

			return operation.getProvisioningJob(null);
		}

		this.log("Install operation not resolved.");
		dumpStatus(status);

		return null;
	}

	private void dumpStatus(IStatus status) {
		this.log(status.toString(), status.getException());
		if (status.isMultiStatus())
			for (IStatus child : status.getChildren())
				dumpStatus(child);
	}
	
	private void setJustUpdated(boolean justUpdated) {
		final IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(PLUGIN_ID);
		preferences.putBoolean(JUSTUPDATED, justUpdated);
		try {
			preferences.flush();
		} catch (BackingStoreException e) {
			this.log("Error setting just updated flag - " + e.getMessage(), e);
		}
	}
	
	private String getRepositoryUri() {
		if (this.repositoryLocator == null) 
			return System.getProperty(REPOSITORY_ARG_NAME);
		
		return this.repositoryLocator.getRepositoryLocation();
	}

	private boolean shouldFeatureBeInstalled(IInstallableUnit iu) {
		if (this.installFilter == null)
			return true;
		
		return this.installFilter.shouldFeatureBeInstalled(iu);
	}

	private void log(String message) {
		UpdateManagerLogger logger = this.logger;
		if (logger == null) {
			System.out.println(message);
			return;
		}
		
		logger.log(message);
	}
	
	private void log(String message, Throwable e) {
		/* Exception may be null when logging a Status object */
		if (e == null) {
			log(message);
			return;
		}
		
		UpdateManagerLogger logger = this.logger;
		if (logger == null) {
			System.out.println(message);
			e.printStackTrace();
			return;
		}
		
		logger.log(message, e);
	}
}
