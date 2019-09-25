/*
 * Copyright 2015-2019 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.commons.util.StringUtils.containsIsoControlCharacter;
import static org.junit.platform.commons.util.StringUtils.containsWhitespace;
import static org.junit.platform.commons.util.StringUtils.doesNotContainIsoControlCharacter;
import static org.junit.platform.commons.util.StringUtils.doesNotContainWhitespace;
import static org.junit.platform.commons.util.StringUtils.isBlank;
import static org.junit.platform.commons.util.StringUtils.isNotBlank;
import static org.junit.platform.commons.util.StringUtils.nullSafeToString;
import static org.junit.platform.commons.util.StringUtils.replaceIsoControlCharacters;
import static org.junit.platform.commons.util.StringUtils.replaceWhitespaceCharacters;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.PreconditionViolationException;

/**
 * Unit tests for {@link StringUtils}.
 *
 * @since 1.0
 */
class StringUtilsTests {

	@Test
	void blankness() {
		// @formatter:off
		assertAll("Blankness",
			() -> assertTrue(isBlank(null)),
			() -> assertTrue(isBlank("")),
			() -> assertTrue(isBlank(" \t\n\r")),
			() -> assertTrue(isNotBlank("."))
		);
		// @formatter:on
	}

	@Test
	void whitespace() {
		// @formatter:off
		assertAll("Whitespace",
			() -> shouldContainWhitespace("   "),
			() -> shouldContainWhitespace("\u005Ct"), // horizontal tab
			() -> shouldContainWhitespace("\t"),
			() -> shouldContainWhitespace("\u005Cn"), // line feed
			() -> shouldContainWhitespace("\n"),
			() -> shouldContainWhitespace("\u005Cf"), // form feed
			() -> shouldContainWhitespace("\f"),
			() -> shouldContainWhitespace("\u005Cr"), // carriage return
			() -> shouldContainWhitespace("\r"),
			() -> shouldContainWhitespace("hello world"),
			() -> shouldNotContainWhitespace(null),
			() -> shouldNotContainWhitespace(""),
			() -> shouldNotContainWhitespace("hello-world"),
			() -> shouldNotContainWhitespace("0123456789"),
			() -> shouldNotContainWhitespace("$-_=+!@.,")
		);
		// @formatter:on
	}

	@Test
	void controlCharacters() {
		// @formatter:off
		assertAll("ISO Control Characters",
			() -> shouldContainIsoControlCharacter("\u005Ct"), // horizontal tab
			() -> shouldContainIsoControlCharacter("\t"),
			() -> shouldContainIsoControlCharacter("\u005Cn"), // line feed
			() -> shouldContainIsoControlCharacter("\n"),
			() -> shouldContainIsoControlCharacter("\u005Cf"), // form feed
			() -> shouldContainIsoControlCharacter("\f"),
			() -> shouldContainIsoControlCharacter("\u005Cr"), // carriage return
			() -> shouldContainIsoControlCharacter("\r"),
			() -> shouldNotContainIsoControlCharacter(null),
			() -> shouldNotContainIsoControlCharacter(""),
			() -> shouldNotContainIsoControlCharacter("hello-world"),
			() -> shouldNotContainIsoControlCharacter("0123456789"),
			() -> shouldNotContainIsoControlCharacter("$-_=+!@.,"),
			() -> shouldNotContainIsoControlCharacter("   "),
			() -> shouldNotContainIsoControlCharacter("hello world")
		);
		// @formatter:on
	}

	@Test
	void replaceControlCharacters() {
		assertNull(replaceIsoControlCharacters(null, ""));
		assertEquals("", replaceIsoControlCharacters("", "."));
		assertEquals("", replaceIsoControlCharacters("\t\n\r", ""));
		assertEquals("...", replaceIsoControlCharacters("\t\n\r", "."));
		assertEquals("...", replaceIsoControlCharacters("\u005Ct\u005Cn\u005Cr", "."));
		assertEquals("abc", replaceIsoControlCharacters("abc", "?"));
		assertEquals("...", replaceIsoControlCharacters("...", "?"));

		assertThrows(PreconditionViolationException.class, () -> replaceIsoControlCharacters("", null));
	}

	@Test
	void replaceWhitespaces() {
		assertNull(replaceWhitespaceCharacters(null, ""));
		assertEquals("", replaceWhitespaceCharacters("", "."));
		assertEquals("", replaceWhitespaceCharacters("\t\n\r", ""));
		assertEquals("...", replaceWhitespaceCharacters("\t\n\r", "."));
		assertEquals("...", replaceWhitespaceCharacters("\u005Ct\u005Cn\u005Cr", "."));
		assertEquals("abc", replaceWhitespaceCharacters("abc", "?"));
		assertEquals("...", replaceWhitespaceCharacters("...", "?"));
		assertEquals(" ", replaceWhitespaceCharacters(" ", " "));
		assertEquals(" ", replaceWhitespaceCharacters("\u000B", " "));
		assertEquals(" ", replaceWhitespaceCharacters("\f", " "));

		assertThrows(PreconditionViolationException.class, () -> replaceWhitespaceCharacters("", null));
	}

	@Test
	void nullSafeToStringChecks() {
		assertEquals("null", nullSafeToString(null));
		assertEquals("", nullSafeToString(""));
		assertEquals("\t", nullSafeToString("\t"));
		assertEquals("foo", nullSafeToString("foo"));
		assertEquals("3.14", nullSafeToString(Double.valueOf("3.14")));
		assertEquals("[1, 2, 3]", nullSafeToString(new int[] { 1, 2, 3 }));
		assertEquals("[a, b, c]", nullSafeToString(new char[] { 'a', 'b', 'c' }));
		assertEquals("[foo, bar]", nullSafeToString(new String[] { "foo", "bar" }));
		assertEquals("[34, 42]", nullSafeToString(new Integer[] { 34, 42 }));
		assertEquals("[[2, 4], [3, 9]]", nullSafeToString(new Integer[][] { { 2, 4 }, { 3, 9 } }));
	}

	@Test
	void nullSafeToStringForObjectWhoseToStringImplementationThrowsAnException() {
		assertThat(nullSafeToString(new ToStringThrowsException()))//
				.startsWith(ToStringThrowsException.class.getName() + "@");
	}

	private void shouldContainWhitespace(String str) {
		assertTrue(containsWhitespace(str), () -> String.format("'%s' should contain whitespace", str));
		assertFalse(doesNotContainWhitespace(str), () -> String.format("'%s' should contain whitespace", str));
	}

	private void shouldNotContainWhitespace(String str) {
		assertTrue(doesNotContainWhitespace(str), () -> String.format("'%s' should not contain whitespace", str));
		assertFalse(containsWhitespace(str), () -> String.format("'%s' should not contain whitespace", str));
	}

	private void shouldContainIsoControlCharacter(String str) {
		assertTrue(containsIsoControlCharacter(str),
			() -> String.format("'%s' should contain ISO control character", str));
		assertFalse(doesNotContainIsoControlCharacter(str),
			() -> String.format("'%s' should contain ISO control character", str));
	}

	private void shouldNotContainIsoControlCharacter(String str) {
		assertTrue(doesNotContainIsoControlCharacter(str),
			() -> String.format("'%s' should not contain ISO control character", str));
		assertFalse(containsIsoControlCharacter(str),
			() -> String.format("'%s' should not contain ISO control character", str));
	}

	private static class ToStringThrowsException {

		@Override
		public String toString() {
			throw new RuntimeException("Boom!");
		}
	}

}
