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
 * Provides a way to implement your own logging during the auto-update process.
 * 
 * If no logging support is provided, all information is written to sysout.
 */
public interface UpdateManagerLogger {

	void log(String message);
}
