/*
 * Copyright 2015-2019 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.PreconditionViolationException;

/**
 * Unit tests for {@link TestTag}.
 *
 * @since 1.0
 */
class TestTagTests {

	@Test
	void validSyntax() {
		// @formatter:off
		assertAll("Valid Tag Syntax",
			() -> yep("fast"),
			() -> yep("super_fast"),
			() -> yep("unit-test"),
			() -> yep("integration.test"),
			() -> yep("org.example.CustomTagClass"),
			() -> yep("  surrounded-by-whitespace\t\n"),
			() -> nope(null),
			() -> nope(""),
			() -> nope("     "),
			() -> nope("\t"),
			() -> nope("\f"),
			() -> nope("\r"),
			() -> nope("\n"),
			() -> nope("custom tag"), // internal space
			() -> nope(","),          // comma
			() -> nope("("),          // opening parenthesis
			() -> nope(")"),          // closing parenthesis
			() -> nope("&"),          // boolean AND
			() -> nope("|"),          // boolean OR
			() -> nope("!")           // boolean NOT
		);
		// @formatter:on
	}

	@Test
	void factory() {
		assertEquals("foo", TestTag.create("foo").getName());
		assertEquals("foo.tag", TestTag.create("foo.tag").getName());
		assertEquals("foo-tag", TestTag.create("foo-tag").getName());
		assertEquals("foo-tag", TestTag.create("    foo-tag    ").getName());
		assertEquals("foo-tag", TestTag.create("\t  foo-tag  \n").getName());
	}

	@Test
	void factoryPreconditions() {
		assertSyntaxViolation(null);
		assertSyntaxViolation("");
		assertSyntaxViolation("   ");
		assertSyntaxViolation("X\tX");
		assertSyntaxViolation("X\nX");
		assertSyntaxViolation("XXX\u005CtXXX");
	}

	@Test
	void tagEqualsOtherTagWithSameName() {
		assertEquals(TestTag.create("fast"), TestTag.create("fast"));
		assertEquals(TestTag.create("fast").hashCode(), TestTag.create("fast").hashCode());
		assertNotEquals(null, TestTag.create("fast"));
		assertNotEquals(TestTag.create("fast"), null);
	}

	@Test
	void toStringPrintsName() {
		assertEquals("fast", TestTag.create("fast").toString());
	}

	private static void yep(String tag) {
		assertTrue(TestTag.isValid(tag), () -> String.format("'%s' should be a valid tag", tag));
	}

	private static void nope(String tag) {
		assertFalse(TestTag.isValid(tag), () -> String.format("'%s' should not be a valid tag", tag));
	}

	private void assertSyntaxViolation(String tag) {
		PreconditionViolationException exception = assertThrows(PreconditionViolationException.class,
			() -> TestTag.create(tag));
		assertThat(exception).hasMessageStartingWith("Tag name");
		assertThat(exception).hasMessageEndingWith("must be syntactically valid");
	}

}
