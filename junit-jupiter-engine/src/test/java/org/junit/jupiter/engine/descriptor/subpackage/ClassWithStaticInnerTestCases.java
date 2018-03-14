/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.descriptor.subpackage;

import org.junit.jupiter.api.Test;

/**
 * @since 5.0
 */
public class ClassWithStaticInnerTestCases {

	public static class ShouldBeDiscovered {

		@Test
		void test1() {
		}
	}

	@SuppressWarnings("unused")
	private static class ShouldNotBeDiscovered {

		@Test
		void test2() {
		}
	}

}
