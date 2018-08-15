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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.function.Executable;
import org.junit.platform.commons.support.ReflectionSupport;
import org.opentest4j.AssertionFailedError;

/**
 * @since 5.0
 */
class DynamicTestTests {

	private static final Executable nix = () -> {
	};

	private final List<String> assertedValues = new ArrayList<>();

	@Test
	void streamFromIterator() throws Throwable {
		Stream<DynamicTest> stream = DynamicTest.stream(Arrays.asList("foo", "bar", "baz").iterator(),
			String::toUpperCase, this::throwingConsumer);
		List<DynamicTest> dynamicTests = stream.collect(Collectors.toList());

		assertThat(dynamicTests).hasSize(3).extracting(DynamicTest::getDisplayName).containsExactly("FOO", "BAR",
			"BAZ");

		assertThat(assertedValues).isEmpty();

		dynamicTests.get(0).getExecutable().execute();
		assertThat(assertedValues).containsExactly("foo");

		dynamicTests.get(1).getExecutable().execute();
		assertThat(assertedValues).containsExactly("foo", "bar");

		Throwable t = assertThrows(Throwable.class, () -> dynamicTests.get(2).getExecutable().execute());
		assertThat(t).hasMessage("Baz!");
		assertThat(assertedValues).containsExactly("foo", "bar");
	}

	private void throwingConsumer(String str) throws Throwable {
		if ("baz".equals(str)) {
			throw new Throwable("Baz!");
		}
		this.assertedValues.add(str);
	}

	@Test
	void reflectiveOperationsThrowingAssertionFailedError() {
		Throwable t48 = assertThrows(AssertionFailedError.class,
			() -> dynamicTest("1 == 48", this::assert1Equals48Directly).getExecutable().execute());
		assertThat(t48).hasMessage("expected: <1> but was: <48>");

		Throwable t49 = assertThrows(AssertionFailedError.class, () -> dynamicTest("1 == 49",
			this::assert1Equals49ReflectivelyAndUnwrapInvocationTargetException).getExecutable().execute());
		assertThat(t49).hasMessage("expected: <1> but was: <49>");
	}

	@Test
	void reflectiveOperationThrowingInvocationTargetException() {
		Throwable t50 = assertThrows(InvocationTargetException.class,
			() -> dynamicTest("1 == 50", this::assert1Equals50Reflectively).getExecutable().execute());
		assertThat(t50.getCause()).hasMessage("expected: <1> but was: <50>");
	}

	@Test
	void testSourceUriIsNotPresentByDefault() {
		DynamicTest test = dynamicTest("foo", nix);
		assertThat(test.getTestSourceUri()).isNotPresent();
		assertThat(test.toString()).isEqualTo("DynamicTest [displayName = 'foo', testSourceUri = null]");
		DynamicContainer container = dynamicContainer("bar", Stream.of(test));
		assertThat(container.getTestSourceUri()).isNotPresent();
		assertThat(container.toString()).isEqualTo("DynamicContainer [displayName = 'bar', testSourceUri = null]");
	}

	@Test
	void testSourceUriIsReturnedWhenSupplied() {
		URI testSourceUri = URI.create("any://test");
		DynamicTest test = dynamicTest("foo", testSourceUri, nix);
		URI containerSourceUri = URI.create("other://container");
		DynamicContainer container = dynamicContainer("bar", containerSourceUri, Stream.of(test));

		assertThat(test.getTestSourceUri().get()).isSameAs(testSourceUri);
		assertThat(test.toString()).isEqualTo("DynamicTest [displayName = 'foo', testSourceUri = any://test]");
		assertThat(container.getTestSourceUri().get()).isSameAs(containerSourceUri);
		assertThat(container.toString()).isEqualTo(
			"DynamicContainer [displayName = 'bar', testSourceUri = other://container]");
	}

	private void assert1Equals48Directly() {
		Assertions.assertEquals(1, 48);
	}

	private void assert1Equals49ReflectivelyAndUnwrapInvocationTargetException() throws Throwable {
		Method method = Assertions.class.getMethod("assertEquals", int.class, int.class);
		ReflectionSupport.invokeMethod(method, null, 1, 49);
	}

	private void assert1Equals50Reflectively() throws Throwable {
		Method method = Assertions.class.getMethod("assertEquals", int.class, int.class);
		method.invoke(null, 1, 50);
	}

}
