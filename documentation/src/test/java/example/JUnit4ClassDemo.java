/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package example;

// tag::user_guide[]
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

@RunWith(JUnitPlatform.class)
public class JUnit4ClassDemo {

	@Test
	void succeedingTest() {
		/* no-op */
	}

	// end::user_guide[]
	@extensions.ExpectToFail
	// tag::user_guide[]
	@Test
	void failingTest() {
		fail("Failing for failing's sake.");
	}

}
// end::user_guide[]
