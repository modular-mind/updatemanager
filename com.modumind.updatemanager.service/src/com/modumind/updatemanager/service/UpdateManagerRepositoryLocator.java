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

/**
 * Mechanism to return the location of the p2 repository to query for updates
 * and new features.
 * 
 * If no locator is provided, the default is to specify a p2 repository URI as a
 * command line parameter:
 * 
 * -Drepository=https://my.p2.repo
 */
public interface UpdateManagerRepositoryLocator {

	/**
	 * @return repository location as a string. The framework will evaluate whether
	 *         it's a valid URI.
	 */
	String getRepositoryLocation();
}
