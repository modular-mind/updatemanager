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
package com.modumind.updatemanager.service;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;

/**
 * Determine whether a feature should be installed or not. This method will only
 * be called for features found in the remote p2 repository that are not already
 * installed locally.
 * 
 * One strategy is to compare the feature id to the permissions for the current
 * user. This allows you to install separate feature-sets for users based on
 * their role.
 * 
 * If no filter is provided, new features are ignored by default.
 */
public interface UpdateManagerInstallFilter {

	/**
	 * Respond to a request to install a particular feature.
	 * 
	 * @param iu feature requested for installation
	 * @return whether feature should be installed or not, currently set to install
	 *         all features found in the repository. Override to perform any logic
	 *         you like.
	 */
	boolean shouldFeatureBeInstalled(IInstallableUnit iu);
}
