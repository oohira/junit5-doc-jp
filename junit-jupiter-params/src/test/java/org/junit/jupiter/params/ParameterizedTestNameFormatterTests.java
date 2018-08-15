/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.ParameterizedTest.ARGUMENTS_PLACEHOLDER;
import static org.junit.jupiter.params.ParameterizedTest.DEFAULT_DISPLAY_NAME;
import static org.junit.jupiter.params.ParameterizedTest.DISPLAY_NAME_PLACEHOLDER;
import static org.junit.jupiter.params.ParameterizedTest.INDEX_PLACEHOLDER;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.JUnitException;

/**
 * @since 5.0
 */
class ParameterizedTestNameFormatterTests {

	@Test
	void formatsDisplayName() {
		ParameterizedTestNameFormatter formatter = new ParameterizedTestNameFormatter(DISPLAY_NAME_PLACEHOLDER,
			"enigma");

		assertEquals("enigma", formatter.format(1));
		assertEquals("enigma", formatter.format(2));
	}

	@Test
	void formatsInvocationIndex() {
		ParameterizedTestNameFormatter formatter = new ParameterizedTestNameFormatter(INDEX_PLACEHOLDER, "enigma");

		assertEquals("1", formatter.format(1));
		assertEquals("2", formatter.format(2));
	}

	@Test
	void formatsIndividualArguments() {
		ParameterizedTestNameFormatter formatter = new ParameterizedTestNameFormatter("{0} -> {1}", "enigma");

		assertEquals("foo -> 42", formatter.format(1, "foo", 42));
	}

	@Test
	void formatsCompleteArgumentsList() {
		ParameterizedTestNameFormatter formatter = new ParameterizedTestNameFormatter(ARGUMENTS_PLACEHOLDER, "enigma");

		// @formatter:off
		assertEquals("42, 99, enigma, null, [1, 2, 3], [foo, bar], [[2, 4], [3, 9]]",
			formatter.format(1,
				Integer.valueOf(42),
				99,
				"enigma",
				null,
				new int[] { 1, 2, 3 },
				new String[] { "foo", "bar" },
				new Integer[][] { { 2, 4 }, { 3, 9 } }
			));
		// @formatter:on
	}

	@Test
	void formatsInvocationIndexAndCompleteArgumentsListUsingDefaultPattern() {
		ParameterizedTestNameFormatter formatter = new ParameterizedTestNameFormatter(DEFAULT_DISPLAY_NAME, "enigma");

		// Explicit test for https://github.com/junit-team/junit5/issues/814
		assertEquals("[1] [foo, bar]", formatter.format(1, (Object) new String[] { "foo", "bar" }));

		assertEquals("[1] [foo, bar], 42, true", formatter.format(1, new String[] { "foo", "bar" }, 42, true));
	}

	@Test
	void formatsEverythingUsingCustomPattern() {
		String pattern = DISPLAY_NAME_PLACEHOLDER + " :: " + DEFAULT_DISPLAY_NAME + " :: {1}";
		ParameterizedTestNameFormatter formatter = new ParameterizedTestNameFormatter(pattern, "enigma");

		assertEquals("enigma :: [1] foo, bar :: bar", formatter.format(1, "foo", "bar"));
		assertEquals("enigma :: [2] foo, 42 :: 42", formatter.format(2, "foo", 42));
	}

	@Test
	void formatDoesNotAlterArgumentsArray() {
		ParameterizedTestNameFormatter formatter = new ParameterizedTestNameFormatter(ARGUMENTS_PLACEHOLDER, "enigma");
		Object[] actual = { 1, "two", Byte.valueOf("-128"), new Integer[][] { { 2, 4 }, { 3, 9 } } };
		Object[] expected = Arrays.copyOf(actual, actual.length);
		assertEquals("1, two, -128, [[2, 4], [3, 9]]", formatter.format(1, actual));
		assertArrayEquals(expected, actual);
	}

	@Test
	void throwsReadableExceptionForInvalidPattern() {
		ParameterizedTestNameFormatter formatter = new ParameterizedTestNameFormatter("{index", "enigma");

		JUnitException exception = assertThrows(JUnitException.class, () -> formatter.format(1));
		assertNotNull(exception.getCause());
		assertEquals(IllegalArgumentException.class, exception.getCause().getClass());
	}

	@Test
	void formattingDoesNotFailIfArgumentToStringImplementationThrowsAnException() {
		ParameterizedTestNameFormatter formatter = new ParameterizedTestNameFormatter(DEFAULT_DISPLAY_NAME, "enigma");

		String formattedName = formatter.format(1, new Object[] { new ToStringThrowsException(), "foo" });

		assertThat(formattedName).startsWith("[1] " + ToStringThrowsException.class.getName() + "@");
		assertThat(formattedName).endsWith("foo");
	}

	private static class ToStringThrowsException {

		@Override
		public String toString() {
			throw new RuntimeException("Boom!");
		}
	}

}
