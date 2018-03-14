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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.test.event.ExecutionEventRecorder;

/**
 * Integration tests that verify support for {@link BeforeAll} and {@link AfterAll}
 * when used as meta-annotations in the {@link JupiterTestEngine}.
 *
 * @since 5.0
 * @see BeforeEachAndAfterEachComposedAnnotationTests
 */
class BeforeAllAndAfterAllComposedAnnotationTests extends AbstractJupiterTestEngineTests {

	private static final List<String> methodsInvoked = new ArrayList<>();

	@Test
	void beforeAllAndAfterAllAsMetaAnnotations() {
		ExecutionEventRecorder eventRecorder = executeTestsForClass(TestCase.class);

		assertEquals(1, eventRecorder.getTestStartedCount(), "# tests started");
		assertEquals(1, eventRecorder.getTestSuccessfulCount(), "# tests succeeded");
		assertEquals(asList("beforeAll", "test", "afterAll"), methodsInvoked);
	}

	static class TestCase {

		@CustomBeforeAll
		static void beforeAll() {
			methodsInvoked.add("beforeAll");
		}

		@Test
		void test() {
			methodsInvoked.add("test");
		}

		@CustomAfterAll
		static void afterAll() {
			methodsInvoked.add("afterAll");
		}

	}

	@BeforeAll
	@Retention(RetentionPolicy.RUNTIME)
	private @interface CustomBeforeAll {
	}

	@AfterAll
	@Retention(RetentionPolicy.RUNTIME)
	private @interface CustomAfterAll {
	}

}
