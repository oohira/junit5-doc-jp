/*
 * Copyright 2015-2019 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.AssertionTestUtils.assertMessageEquals;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.function.Executable;
import org.junit.platform.commons.PreconditionViolationException;
import org.opentest4j.AssertionFailedError;
import org.opentest4j.MultipleFailuresError;

/**
 * Unit tests for JUnit Jupiter {@link Assertions}.
 *
 * @since 5.0
 */
class AssertAllAssertionsTests {

	@Test
	void assertAllWithNullExecutableArray() {
		assertPrecondition("executables array must not be null or empty", () -> assertAll((Executable[]) null));
	}

	@Test
	void assertAllWithNullExecutableCollection() {
		assertPrecondition("executables collection must not be null", () -> assertAll((Collection<Executable>) null));
	}

	@Test
	void assertAllWithNullExecutableStream() {
		assertPrecondition("executables stream must not be null", () -> assertAll((Stream<Executable>) null));
	}

	@Test
	void assertAllWithNullInExecutableArray() {
		assertPrecondition("individual executables must not be null", () -> assertAll((Executable) null));
	}

	@Test
	void assertAllWithNullInExecutableCollection() {
		assertPrecondition("individual executables must not be null", () -> assertAll(asList((Executable) null)));
	}

	@Test
	void assertAllWithNullInExecutableStream() {
		assertPrecondition("individual executables must not be null", () -> assertAll(Stream.of((Executable) null)));
	}

	@Test
	void assertAllWithExecutablesThatDoNotThrowExceptions() {
		// @formatter:off
		assertAll(
			() -> assertTrue(true),
			() -> assertFalse(false)
		);
		assertAll("heading",
			() -> assertTrue(true),
			() -> assertFalse(false)
		);
		assertAll(asList(
			() -> assertTrue(true),
			() -> assertFalse(false)
		));
		assertAll("heading", asList(
			() -> assertTrue(true),
			() -> assertFalse(false)
		));
		assertAll(Stream.of(
				() -> assertTrue(true),
				() -> assertFalse(false)
		));
		assertAll("heading", Stream.of(
				() -> assertTrue(true),
				() -> assertFalse(false)
		));
		// @formatter:on
	}

	@Test
	void assertAllWithExecutablesThatThrowAssertionErrors() {
		// @formatter:off
		MultipleFailuresError multipleFailuresError = assertThrows(MultipleFailuresError.class, () ->
			assertAll(
				() -> assertFalse(true),
				() -> assertFalse(true)
			)
		);
		// @formatter:on

		assertExpectedExceptionTypes(multipleFailuresError, AssertionFailedError.class, AssertionFailedError.class);
	}

	@Test
	void assertAllWithCollectionOfExecutablesThatThrowAssertionErrors() {
		// @formatter:off
		MultipleFailuresError multipleFailuresError = assertThrows(MultipleFailuresError.class, () ->
			assertAll(asList(
				() -> assertFalse(true),
				() -> assertFalse(true)
			))
		);
		// @formatter:on

		assertExpectedExceptionTypes(multipleFailuresError, AssertionFailedError.class, AssertionFailedError.class);
	}

	@Test
	void assertAllWithStreamOfExecutablesThatThrowAssertionErrors() {
		// @formatter:off
		MultipleFailuresError multipleFailuresError = assertThrows(MultipleFailuresError.class, () ->
			assertAll(Stream.of(
				() -> assertFalse(true),
				() -> assertFalse(true)
			))
		);
		// @formatter:on

		assertExpectedExceptionTypes(multipleFailuresError, AssertionFailedError.class, AssertionFailedError.class);
	}

	@Test
	void assertAllWithExecutableThatThrowsThrowable() {
		MultipleFailuresError multipleFailuresError = assertThrows(MultipleFailuresError.class, () -> assertAll(() -> {
			throw new EnigmaThrowable();
		}));

		assertExpectedExceptionTypes(multipleFailuresError, EnigmaThrowable.class);
	}

	@Test
	void assertAllWithExecutableThatThrowsCheckedException() {
		MultipleFailuresError multipleFailuresError = assertThrows(MultipleFailuresError.class, () -> assertAll(() -> {
			throw new IOException();
		}));

		assertExpectedExceptionTypes(multipleFailuresError, IOException.class);
	}

	@Test
	void assertAllWithExecutableThatThrowsRuntimeException() {
		MultipleFailuresError multipleFailuresError = assertThrows(MultipleFailuresError.class, () -> assertAll(() -> {
			throw new IllegalStateException();
		}));

		assertExpectedExceptionTypes(multipleFailuresError, IllegalStateException.class);
	}

	@Test
	void assertAllWithExecutableThatThrowsError() {
		MultipleFailuresError multipleFailuresError = assertThrows(MultipleFailuresError.class,
			() -> assertAll(AssertionTestUtils::recurseIndefinitely));

		assertExpectedExceptionTypes(multipleFailuresError, StackOverflowError.class);
	}

	@Test
	void assertAllWithExecutableThatThrowsBlacklistedException() {
		OutOfMemoryError outOfMemoryError = assertThrows(OutOfMemoryError.class,
			() -> assertAll(AssertionTestUtils::runOutOfMemory));

		assertEquals("boom", outOfMemoryError.getMessage());
	}

	@Test
	void assertAllWithParallelStream() {
		Executable executable = () -> {
			throw new RuntimeException();
		};
		MultipleFailuresError multipleFailuresError = assertThrows(MultipleFailuresError.class,
			() -> assertAll(Stream.generate(() -> executable).parallel().limit(100)));

		assertThat(multipleFailuresError.getFailures()).hasSize(100).doesNotContainNull();
	}

	private void assertPrecondition(String msg, Executable executable) {
		PreconditionViolationException exception = assertThrows(PreconditionViolationException.class, executable);
		assertMessageEquals(exception, msg);
	}

	@SafeVarargs
	static void assertExpectedExceptionTypes(MultipleFailuresError multipleFailuresError,
			Class<? extends Throwable>... exceptionTypes) {

		assertNotNull(multipleFailuresError, "MultipleFailuresError");
		List<Throwable> failures = multipleFailuresError.getFailures();
		assertEquals(exceptionTypes.length, failures.size(), "number of failures");

		// Verify that exceptions are also present as suppressed exceptions.
		// https://github.com/junit-team/junit5/issues/1602
		Throwable[] suppressed = multipleFailuresError.getSuppressed();
		assertEquals(exceptionTypes.length, suppressed.length, "number of suppressed exceptions");

		for (int i = 0; i < exceptionTypes.length; i++) {
			assertEquals(exceptionTypes[i], failures.get(i).getClass(), "exception type [" + i + "]");
			assertEquals(exceptionTypes[i], suppressed[i].getClass(), "suppressed exception type [" + i + "]");
		}
	}

	@SuppressWarnings("serial")
	private static class EnigmaThrowable extends Throwable {
	}

}
