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

import static org.junit.Assert.fail;
import static org.junit.runners.MethodSorters.NAME_ASCENDING;

import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @since 4.12
 */
@Ignore("complete class is ignored")
@FixMethodOrder(NAME_ASCENDING)
public class IgnoredJUnit4TestCase {

	@Test
	public void failingTest() {
		fail("this test is discovered, but skipped");
	}

	@Test
	public void succeedingTest() {
	}

}
