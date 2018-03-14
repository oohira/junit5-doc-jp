/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.core;

import static java.util.Arrays.asList;

import org.junit.platform.engine.TestEngine;

/**
 * @since 1.0
 */
public class LauncherFactoryForTestingPurposesOnly {

	public static DefaultLauncher createLauncher(TestEngine... engines) {
		return new DefaultLauncher(asList(engines));
	}

}
