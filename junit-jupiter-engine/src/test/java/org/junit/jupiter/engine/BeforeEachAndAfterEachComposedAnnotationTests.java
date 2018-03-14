/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.test.event.ExecutionEventRecorder;

/**
 * Integration tests that verify support for {@link BeforeEach} and {@link AfterEach}
 * when used as meta-annotations in the {@link JupiterTestEngine}.
 *
 * @since 5.0
 * @see BeforeAllAndAfterAllComposedAnnotationTests
 */
class BeforeEachAndAfterEachComposedAnnotationTests extends AbstractJupiterTestEngineTests {

	private static final List<String> methodsInvoked = new ArrayList<>();

	@Test
	void beforeEachAndAfterEachAsMetaAnnotations() {
		ExecutionEventRecorder eventRecorder = executeTestsForClass(TestCase.class);

		assertEquals(1, eventRecorder.getTestStartedCount(), "# tests started");
		assertEquals(1, eventRecorder.getTestSuccessfulCount(), "# tests succeeded");
		assertEquals(asList("beforeEach", "test", "afterEach"), methodsInvoked);
	}

	static class TestCase {

		@CustomBeforeEach
		void beforeEach() {
			methodsInvoked.add("beforeEach");
		}

		@Test
		void test() {
			methodsInvoked.add("test");
		}

		@CustomAfterEach
		void afterEach() {
			methodsInvoked.add("afterEach");
		}

	}

	@BeforeEach
	@Retention(RetentionPolicy.RUNTIME)
	private @interface CustomBeforeEach {
	}

	@AfterEach
	@Retention(RetentionPolicy.RUNTIME)
	private @interface CustomAfterEach {
	}

}
