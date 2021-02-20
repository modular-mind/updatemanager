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

import org.eclipse.equinox.p2.core.ProvisionException;

public interface UpdateManager {

	/**
	 * Main update method that runs auto-update process. This is usually called very
	 * early in the startup process, usually in a life cycle handler or splash
	 * handler.
	 * 
	 * @return whether an update or install occurred
	 * @throws ProvisionException
	 */
	public boolean performAutoUpdate();
}
