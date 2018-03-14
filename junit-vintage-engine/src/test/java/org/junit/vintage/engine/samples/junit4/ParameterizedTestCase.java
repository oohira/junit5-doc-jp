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

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * @since 4.12
 */
@RunWith(Parameterized.class)
public class ParameterizedTestCase {

	@Parameters(name = "{0}")
	public static Iterable<String> primes() {
		return asList("foo", "bar");
	}

	@Parameter
	public String value;

	@Test
	public void test() {
		assertEquals("foo", value);
	}

}
