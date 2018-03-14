/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.condition;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.condition.EnabledOnJreIntegrationTests.onJava10;
import static org.junit.jupiter.api.condition.EnabledOnJreIntegrationTests.onJava11;
import static org.junit.jupiter.api.condition.EnabledOnJreIntegrationTests.onJava8;
import static org.junit.jupiter.api.condition.EnabledOnJreIntegrationTests.onJava9;
import static org.junit.jupiter.api.condition.JRE.JAVA_10;
import static org.junit.jupiter.api.condition.JRE.JAVA_11;
import static org.junit.jupiter.api.condition.JRE.JAVA_8;
import static org.junit.jupiter.api.condition.JRE.JAVA_9;
import static org.junit.jupiter.api.condition.JRE.OTHER;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link DisabledOnJre}.
 *
 * @since 5.1
 */
class DisabledOnJreIntegrationTests {

	@Test
	@Disabled("Only used in a unit test via reflection")
	void enabledBecauseAnnotationIsNotPresent() {
	}

	@Test
	@Disabled("Only used in a unit test via reflection")
	@DisabledOnJre({})
	void missingJreDeclaration() {
	}

	@Test
	@DisabledOnJre({ JAVA_8, JAVA_9, JAVA_10, JAVA_11, OTHER })
	void disabledOnAllJavaVersions() {
		fail("should be disabled");
	}

	@Test
	@DisabledOnJre(JAVA_8)
	void java8() {
		assertFalse(onJava8());
	}

	@Test
	@DisabledOnJre(JAVA_9)
	void java9() {
		assertFalse(onJava9());
	}

	@Test
	@DisabledOnJre(JAVA_10)
	void java10() {
		assertFalse(onJava10());
	}

	@Test
	@DisabledOnJre(JAVA_11)
	void java11() {
		assertFalse(onJava11());
	}

	@Test
	@DisabledOnJre(OTHER)
	void other() {
		assertTrue(onJava8() || onJava9() || onJava10() || onJava11());
	}

}
