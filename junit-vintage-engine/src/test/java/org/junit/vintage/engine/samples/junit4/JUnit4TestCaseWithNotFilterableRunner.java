/*
 * Copyright 2015-2019 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine.samples.junit4;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.vintage.engine.samples.junit4.ConfigurableRunner.ChildCount;

/**
 * @since 5.1
 */
@RunWith(NotFilterableRunner.class)
@ChildCount(2)
@Category(Categories.Successful.class)
public class JUnit4TestCaseWithNotFilterableRunner {

	@Test
	public void someTest() {
	}

}
