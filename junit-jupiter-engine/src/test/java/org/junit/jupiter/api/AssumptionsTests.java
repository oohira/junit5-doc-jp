/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assumptions.assumingThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.function.Executable;
import org.opentest4j.TestAbortedException;

/**
 * Unit tests for JUnit Jupiter {@link Assumptions}.
 *
 * @since 5.0
 */
class AssumptionsTests {

	// --- assumeTrue ----------------------------------------------------

	@Test
	void assumeTrueWithBooleanTrue() {
		String foo = null;
		try {
			assumeTrue(true);
			assumeTrue(true, "message");
			assumeTrue(true, () -> "message");
			foo = "foo";
		}
		finally {
			assertNotNull(foo);
		}
	}

	@Test
	void assumeTrueWithBooleanSupplierTrue() {
		String foo = null;
		try {
			assumeTrue(() -> true);
			assumeTrue(() -> true, "message");
			assumeTrue(() -> true, () -> "message");
			foo = "foo";
		}
		finally {
			assertNotNull(foo);
		}
	}

	@Test
	void assumeTrueWithBooleanFalse() {
		assertAssumptionFailure("assumption is not true", () -> assumeTrue(false));
	}

	@Test
	void assumeTrueWithBooleanSupplierFalse() {
		assertAssumptionFailure("assumption is not true", () -> assumeTrue(() -> false));
	}

	@Test
	void assumeTrueWithBooleanFalseAndStringMessage() {
		assertAssumptionFailure("test", () -> assumeTrue(false, "test"));
	}

	@Test
	void assumeTrueWithBooleanFalseAndNullStringMessage() {
		assertAssumptionFailure(null, () -> assumeTrue(false, (String) null));
	}

	@Test
	void assumeTrueWithBooleanSupplierFalseAndStringMessage() {
		assertAssumptionFailure("test", () -> assumeTrue(() -> false, "test"));
	}

	@Test
	void assumeTrueWithBooleanSupplierFalseAndMessageSupplier() {
		assertAssumptionFailure("test", () -> assumeTrue(() -> false, () -> "test"));
	}

	@Test
	void assumeTrueWithBooleanFalseAndMessageSupplier() {
		assertAssumptionFailure("test", () -> assumeTrue(false, () -> "test"));
	}

	// --- assumeFalse ----------------------------------------------------

	@Test
	void assumeFalseWithBooleanFalse() {
		String foo = null;
		try {
			assumeFalse(false);
			assumeFalse(false, "message");
			assumeFalse(false, () -> "message");
			foo = "foo";
		}
		finally {
			assertNotNull(foo);
		}
	}

	@Test
	void assumeFalseWithBooleanSupplierFalse() {
		String foo = null;
		try {
			assumeFalse(() -> false);
			assumeFalse(() -> false, "message");
			assumeFalse(() -> false, () -> "message");
			foo = "foo";
		}
		finally {
			assertNotNull(foo);
		}
	}

	@Test
	void assumeFalseWithBooleanTrue() {
		assertAssumptionFailure("assumption is not false", () -> assumeFalse(true));
	}

	@Test
	void assumeFalseWithBooleanSupplierTrue() {
		assertAssumptionFailure("assumption is not false", () -> assumeFalse(() -> true));
	}

	@Test
	void assumeFalseWithBooleanTrueAndStringMessage() {
		assertAssumptionFailure("test", () -> assumeFalse(true, "test"));
	}

	@Test
	void assumeFalseWithBooleanSupplierTrueAndMessage() {
		assertAssumptionFailure("test", () -> assumeFalse(() -> true, "test"));
	}

	@Test
	void assumeFalseWithBooleanSupplierTrueAndMessageSupplier() {
		assertAssumptionFailure("test", () -> assumeFalse(() -> true, () -> "test"));
	}

	@Test
	void assumeFalseWithBooleanTrueAndMessageSupplier() {
		assertAssumptionFailure("test", () -> assumeFalse(true, () -> "test"));
	}

	// --- assumingThat --------------------------------------------------

	@Test
	void assumingThatWithBooleanTrue() {
		List<String> list = new ArrayList<>();
		assumingThat(true, () -> list.add("test"));
		assertEquals(1, list.size());
		assertEquals("test", list.get(0));
	}

	@Test
	void assumingThatWithBooleanSupplierTrue() {
		List<String> list = new ArrayList<>();
		assumingThat(() -> true, () -> list.add("test"));
		assertEquals(1, list.size());
		assertEquals("test", list.get(0));
	}

	@Test
	void assumingThatWithBooleanFalse() {
		List<String> list = new ArrayList<>();
		assumingThat(false, () -> list.add("test"));
		assertEquals(0, list.size());
	}

	@Test
	void assumingThatWithBooleanSupplierFalse() {
		List<String> list = new ArrayList<>();
		assumingThat(() -> false, () -> list.add("test"));
		assertEquals(0, list.size());
	}

	@Test
	void assumingThatWithFailingExecutable() {
		assertThrows(EnigmaThrowable.class, () -> assumingThat(true, () -> {
			throw new EnigmaThrowable();
		}));
	}

	// -------------------------------------------------------------------

	private static void assertAssumptionFailure(String msg, Executable executable) {
		try {
			executable.execute();
			expectTestAbortedException();
		}
		catch (Throwable ex) {
			assertTrue(ex instanceof TestAbortedException);
			assertMessageEquals((TestAbortedException) ex,
				msg == null ? "Assumption failed" : "Assumption failed: " + msg);
		}
	}

	private static void expectTestAbortedException() {
		throw new AssertionError("Should have thrown a " + TestAbortedException.class.getName());
	}

	private static void assertMessageEquals(TestAbortedException ex, String msg) throws AssertionError {
		if (!msg.equals(ex.getMessage())) {
			throw new AssertionError(
				"Message in TestAbortedException should be [" + msg + "], but was [" + ex.getMessage() + "].");
		}
	}

}
