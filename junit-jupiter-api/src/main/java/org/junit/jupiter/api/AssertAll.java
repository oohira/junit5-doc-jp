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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.function.Executable;
import org.junit.platform.commons.util.BlacklistedExceptions;
import org.junit.platform.commons.util.Preconditions;
import org.opentest4j.MultipleFailuresError;

/**
 * {@code AssertAll} is a collection of utility methods that support asserting
 * multiple conditions in tests at once.
 *
 * @since 5.0
 */
class AssertAll {

	///CLOVER:OFF
	private AssertAll() {
		/* no-op */
	}
	///CLOVER:ON

	static void assertAll(Executable... executables) {
		assertAll(null, executables);
	}

	static void assertAll(String heading, Executable... executables) {
		Preconditions.notEmpty(executables, "executables array must not be null or empty");
		Preconditions.containsNoNullElements(executables, "individual executables must not be null");
		assertAll(heading, Arrays.stream(executables));
	}

	static void assertAll(Collection<Executable> executables) {
		assertAll(null, executables);
	}

	static void assertAll(String heading, Collection<Executable> executables) {
		Preconditions.notNull(executables, "executables collection must not be null");
		Preconditions.containsNoNullElements(executables, "individual executables must not be null");
		assertAll(heading, executables.stream());
	}

	static void assertAll(Stream<Executable> executables) {
		assertAll(null, executables);
	}

	static void assertAll(String heading, Stream<Executable> executables) {
		Preconditions.notNull(executables, "executables stream must not be null");

		List<Throwable> failures = new ArrayList<>();
		executables//
				.peek(executable -> Preconditions.notNull(executable, "individual executables must not be null"))//
				.forEach(executable -> {
					try {
						executable.execute();
					}
					catch (Throwable t) {
						BlacklistedExceptions.rethrowIfBlacklisted(t);
						failures.add(t);
					}
				});

		if (!failures.isEmpty()) {
			throw new MultipleFailuresError(heading, failures);
		}
	}

}
