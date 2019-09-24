/*
 * Copyright 2015-2019 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.PreconditionViolationException;

/**
 * @since 1.0
 */
class PackageNameFilterTests {

	@Test
	void includePackageChecksPreconditions() {
		assertThatThrownBy(() -> PackageNameFilter.includePackageNames((String[]) null)) //
				.isInstanceOf(PreconditionViolationException.class) //
				.hasMessage("packageNames array must not be null or empty");
		assertThatThrownBy(() -> PackageNameFilter.includePackageNames(new String[0])) //
				.isInstanceOf(PreconditionViolationException.class) //
				.hasMessage("packageNames array must not be null or empty");
		assertThatThrownBy(() -> PackageNameFilter.includePackageNames(new String[] { null })) //
				.isInstanceOf(PreconditionViolationException.class) //
				.hasMessage("packageNames array must not contain null elements");
	}

	@Test
	void includePackageWithMultiplePackages() {
		String includedPackage1 = "java.lang";
		String includedPackage2 = "java.util";
		PackageNameFilter filter = PackageNameFilter.includePackageNames(includedPackage1, includedPackage2);

		assertThat(filter).hasToString(
			"IncludePackageNameFilter that includes packages whose names are either equal to or start with one of the following: '"
					+ includedPackage1 + "' OR '" + includedPackage2 + "'");

		assertTrue(filter.apply("java.lang.String").included());
		assertTrue(filter.toPredicate().test("java.lang.String"));
		assertThat(filter.apply("java.lang.String").getReason()).contains(
			"Package name [java.lang.String] matches included name: '" + includedPackage1 + "'");

		assertTrue(filter.apply("java.util.Collection").included());
		assertTrue(filter.toPredicate().test("java.util.Collection"));
		assertThat(filter.apply("java.util.Collection").getReason()).contains(
			"Package name [java.util.Collection] matches included name: '" + includedPackage2 + "'");

		assertTrue(filter.apply("java.util.function.Consumer").included());
		assertTrue(filter.toPredicate().test("java.util.function.Consumer"));
		assertThat(filter.apply("java.util.function.Consumer").getReason()).contains(
			"Package name [java.util.function.Consumer] matches included name: '" + includedPackage2 + "'");

		assertFalse(filter.apply("java.time.Instant").included());
		assertFalse(filter.toPredicate().test("java.time.Instant"));
		assertThat(filter.apply("java.time.Instant").getReason()).contains(
			"Package name [java.time.Instant] does not match any included names: '" + includedPackage1 + "' OR '"
					+ includedPackage2 + "'");

		assertFalse(filter.apply("java.language.Test").included());
		assertFalse(filter.toPredicate().test("java.language.Test"));
		assertThat(filter.apply("java.language.Test").getReason()).contains(
			"Package name [java.language.Test] does not match any included names: '" + includedPackage1 + "' OR '"
					+ includedPackage2 + "'");
	}

	@Test
	void excludePackageChecksPreconditions() {
		assertThatThrownBy(() -> PackageNameFilter.excludePackageNames((String[]) null)) //
				.isInstanceOf(PreconditionViolationException.class) //
				.hasMessage("packageNames must not be null or empty");
		assertThatThrownBy(() -> PackageNameFilter.excludePackageNames(new String[0])) //
				.isInstanceOf(PreconditionViolationException.class) //
				.hasMessage("packageNames must not be null or empty");
		assertThatThrownBy(() -> PackageNameFilter.excludePackageNames(new String[] { null })) //
				.isInstanceOf(PreconditionViolationException.class) //
				.hasMessage("packageNames must not contain null elements");
	}

	@Test
	void excludePackageWithMultiplePackages() {
		String excludedPackage1 = "java.lang";
		String excludedPackage2 = "java.util";
		PackageNameFilter filter = PackageNameFilter.excludePackageNames(excludedPackage1, excludedPackage2);

		assertThat(filter).hasToString(
			"ExcludePackageNameFilter that excludes packages whose names are either equal to or start with one of the following: '"
					+ excludedPackage1 + "' OR '" + excludedPackage2 + "'");

		assertTrue(filter.apply("java.lang.String").excluded());
		assertFalse(filter.toPredicate().test("java.lang.String"));
		assertThat(filter.apply("java.lang.String").getReason()).contains(
			"Package name [java.lang.String] matches excluded name: '" + excludedPackage1 + "'");

		assertTrue(filter.apply("java.util.Collection").excluded());
		assertFalse(filter.toPredicate().test("java.util.Collection"));
		assertThat(filter.apply("java.util.Collection").getReason()).contains(
			"Package name [java.util.Collection] matches excluded name: '" + excludedPackage2 + "'");

		assertTrue(filter.apply("java.util.function.Consumer").excluded());
		assertFalse(filter.toPredicate().test("java.util.function.Consumer"));
		assertThat(filter.apply("java.util.function.Consumer").getReason()).contains(
			"Package name [java.util.function.Consumer] matches excluded name: '" + excludedPackage2 + "'");

		assertTrue(filter.apply("java.time.Instant").included());
		assertTrue(filter.toPredicate().test("java.time.Instant"));
		assertThat(filter.apply("java.time.Instant").getReason()).contains(
			"Package name [java.time.Instant] does not match any excluded names: '" + excludedPackage1 + "' OR '"
					+ excludedPackage2 + "'");

		assertTrue(filter.apply("java.language.Test").included());
		assertTrue(filter.toPredicate().test("java.language.Test"));
		assertThat(filter.apply("java.language.Test").getReason()).contains(
			"Package name [java.language.Test] does not match any excluded names: '" + excludedPackage1 + "' OR '"
					+ excludedPackage2 + "'");
	}
}
