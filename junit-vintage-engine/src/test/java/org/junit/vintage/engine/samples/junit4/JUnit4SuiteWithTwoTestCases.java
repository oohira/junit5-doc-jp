/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine.samples.junit4;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * @since 4.12
 */
@RunWith(Suite.class)
@SuiteClasses({ PlainJUnit4TestCaseWithTwoTestMethods.class, PlainJUnit4TestCaseWithSingleTestWhichFails.class })
public class JUnit4SuiteWithTwoTestCases {
}
