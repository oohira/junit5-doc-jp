/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.console.subpackage;

import org.junit.jupiter.api.Test;

// Even though our "example test classes" should be named *TestCase
// according to team policy, this class must be named *Tests in order
// for ConsoleLauncherIntegrationTests to pass, but that's not a
// problem since the test method here can never fail.
class ContainerForStaticNestedTests {

	static class NestedTests {

		@Test
		void test() {
		}
	}

}
