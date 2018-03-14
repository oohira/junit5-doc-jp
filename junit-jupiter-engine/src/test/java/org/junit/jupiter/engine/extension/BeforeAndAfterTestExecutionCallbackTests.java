/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.extension;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.engine.AbstractJupiterTestEngineTests;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.engine.test.event.ExecutionEventRecorder;
import org.junit.platform.launcher.LauncherDiscoveryRequest;

/**
 * Integration tests that verify support for {@link BeforeTestExecutionCallback},
 * {@link AfterTestExecutionCallback}, {@link BeforeEach}, and {@link AfterEach}
 * in the {@link JupiterTestEngine}.
 *
 * @since 5.0
 * @see BeforeAndAfterEachTests
 */
class BeforeAndAfterTestExecutionCallbackTests extends AbstractJupiterTestEngineTests {

	private static List<String> callSequence = new ArrayList<>();

	private static Optional<Throwable> actualExceptionInAfterTestExecution;

	@BeforeEach
	void resetCallSequence() {
		callSequence.clear();
		actualExceptionInAfterTestExecution = null;
	}

	@Test
	void beforeAndAfterTestExecutionCallbacks() {
		LauncherDiscoveryRequest request = request().selectors(selectClass(OuterTestCase.class)).build();

		ExecutionEventRecorder eventRecorder = executeTests(request);

		assertEquals(2, eventRecorder.getTestStartedCount(), "# tests started");
		assertEquals(2, eventRecorder.getTestSuccessfulCount(), "# tests succeeded");
		assertEquals(0, eventRecorder.getTestSkippedCount(), "# tests skipped");
		assertEquals(0, eventRecorder.getTestAbortedCount(), "# tests aborted");
		assertEquals(0, eventRecorder.getTestFailedCount(), "# tests failed");

		// @formatter:off
		assertEquals(asList(

			// OuterTestCase
			"beforeEachMethodOuter",
				"fooBeforeTestExecutionCallback",
				"barBeforeTestExecutionCallback",
					"testOuter",
				"barAfterTestExecutionCallback",
				"fooAfterTestExecutionCallback",
			"afterEachMethodOuter",

			// InnerTestCase
			"beforeEachMethodOuter",
				"beforeEachMethodInner",
					"fooBeforeTestExecutionCallback",
					"barBeforeTestExecutionCallback",
						"fizzBeforeTestExecutionCallback",
							"testInner",
						"fizzAfterTestExecutionCallback",
					"barAfterTestExecutionCallback",
					"fooAfterTestExecutionCallback",
				"afterEachMethodInner",
			"afterEachMethodOuter"

		), callSequence, "wrong call sequence");
		// @formatter:on
	}

	@Test
	void beforeAndAfterTestExecutionCallbacksDeclaredOnSuperclassAndSubclass() {
		LauncherDiscoveryRequest request = request().selectors(selectClass(ChildTestCase.class)).build();

		ExecutionEventRecorder eventRecorder = executeTests(request);

		assertEquals(1, eventRecorder.getTestStartedCount(), "# tests started");
		assertEquals(1, eventRecorder.getTestSuccessfulCount(), "# tests succeeded");
		assertEquals(0, eventRecorder.getTestSkippedCount(), "# tests skipped");
		assertEquals(0, eventRecorder.getTestAbortedCount(), "# tests aborted");
		assertEquals(0, eventRecorder.getTestFailedCount(), "# tests failed");

		// @formatter:off
		assertEquals(asList(
			"fooBeforeTestExecutionCallback",
			"barBeforeTestExecutionCallback",
				"testChild",
			"barAfterTestExecutionCallback",
			"fooAfterTestExecutionCallback"
		), callSequence, "wrong call sequence");
		// @formatter:on
	}

	@Test
	void beforeAndAfterTestExecutionCallbacksDeclaredOnInterfaceAndClass() {
		LauncherDiscoveryRequest request = request().selectors(selectClass(TestInterfaceTestCase.class)).build();

		ExecutionEventRecorder eventRecorder = executeTests(request);

		assertEquals(2, eventRecorder.getTestStartedCount(), "# tests started");
		assertEquals(2, eventRecorder.getTestSuccessfulCount(), "# tests succeeded");
		assertEquals(0, eventRecorder.getTestSkippedCount(), "# tests skipped");
		assertEquals(0, eventRecorder.getTestAbortedCount(), "# tests aborted");
		assertEquals(0, eventRecorder.getTestFailedCount(), "# tests failed");

		// @formatter:off
		assertEquals(asList(

			// Test Interface
			"fooBeforeTestExecutionCallback",
				"barBeforeTestExecutionCallback",
					"defaultTestMethod",
				"barAfterTestExecutionCallback",
			"fooAfterTestExecutionCallback",

			// Test Class
			"fooBeforeTestExecutionCallback",
				"barBeforeTestExecutionCallback",
					"localTestMethod",
				"barAfterTestExecutionCallback",
			"fooAfterTestExecutionCallback"

		), callSequence, "wrong call sequence");
		// @formatter:on
	}

	@Test
	void beforeEachMethodThrowsAnException() {
		LauncherDiscoveryRequest request = request().selectors(
			selectClass(ExceptionInBeforeEachMethodTestCase.class)).build();

		ExecutionEventRecorder eventRecorder = executeTests(request);

		assertEquals(1, eventRecorder.getTestStartedCount(), "# tests started");
		assertEquals(0, eventRecorder.getTestSuccessfulCount(), "# tests succeeded");
		assertEquals(0, eventRecorder.getTestSkippedCount(), "# tests skipped");
		assertEquals(0, eventRecorder.getTestAbortedCount(), "# tests aborted");
		assertEquals(1, eventRecorder.getTestFailedCount(), "# tests failed");

		// @formatter:off
		assertEquals(asList(
			"beforeEachMethod", // throws an exception.
				// fooBeforeTestExecutionCallback should not get invoked.
					// test should not get invoked.
				// fooAfterTestExecutionCallback should not get invoked.
			"afterEachMethod"
		), callSequence, "wrong call sequence");
		// @formatter:on

		assertNull(actualExceptionInAfterTestExecution,
			"test exception (fooAfterTestExecutionCallback should not have been called)");
	}

	@Test
	void beforeTestExecutionCallbackThrowsAnException() {
		LauncherDiscoveryRequest request = request().selectors(
			selectClass(ExceptionInBeforeTestExecutionCallbackTestCase.class)).build();

		ExecutionEventRecorder eventRecorder = executeTests(request);

		assertEquals(1, eventRecorder.getTestStartedCount(), "# tests started");
		assertEquals(0, eventRecorder.getTestSuccessfulCount(), "# tests succeeded");
		assertEquals(0, eventRecorder.getTestSkippedCount(), "# tests skipped");
		assertEquals(0, eventRecorder.getTestAbortedCount(), "# tests aborted");
		assertEquals(1, eventRecorder.getTestFailedCount(), "# tests failed");

		// @formatter:off
		assertEquals(asList(
			"beforeEachMethod",
				"fooBeforeTestExecutionCallback",
				"exceptionThrowingBeforeTestExecutionCallback", // throws an exception.
				// barBeforeTestExecutionCallback should not get invoked.
					// test() should not get invoked.
				"barAfterTestExecutionCallback",
				"fooAfterTestExecutionCallback",
			"afterEachMethod"
		), callSequence, "wrong call sequence");
		// @formatter:on

		assertNotNull(actualExceptionInAfterTestExecution, "test exception");
		assertTrue(actualExceptionInAfterTestExecution.isPresent(), "test exception should be present");
		assertEquals(EnigmaException.class, actualExceptionInAfterTestExecution.get().getClass());
	}

	@Test
	void afterTestExecutionCallbackThrowsAnException() {
		LauncherDiscoveryRequest request = request().selectors(
			selectClass(ExceptionInAfterTestExecutionCallbackTestCase.class)).build();

		ExecutionEventRecorder eventRecorder = executeTests(request);

		assertEquals(1, eventRecorder.getTestStartedCount(), "# tests started");
		assertEquals(0, eventRecorder.getTestSuccessfulCount(), "# tests succeeded");
		assertEquals(0, eventRecorder.getTestSkippedCount(), "# tests skipped");
		assertEquals(0, eventRecorder.getTestAbortedCount(), "# tests aborted");
		assertEquals(1, eventRecorder.getTestFailedCount(), "# tests failed");

		// @formatter:off
		assertEquals(asList(
			"beforeEachMethod",
				"fooBeforeTestExecutionCallback",
				"barBeforeTestExecutionCallback",
					"test",
				"barAfterTestExecutionCallback",
				"exceptionThrowingAfterTestExecutionCallback", // throws an exception.
				"fooAfterTestExecutionCallback",
			"afterEachMethod"
		), callSequence, "wrong call sequence");
		// @formatter:on

		assertNotNull(actualExceptionInAfterTestExecution, "test exception");
		assertTrue(actualExceptionInAfterTestExecution.isPresent(), "test exception should be present");
		assertEquals(EnigmaException.class, actualExceptionInAfterTestExecution.get().getClass());
	}

	@Test
	void testMethodThrowsAnException() {
		LauncherDiscoveryRequest request = request().selectors(
			selectClass(ExceptionInTestMethodTestCase.class)).build();

		ExecutionEventRecorder eventRecorder = executeTests(request);

		assertEquals(1, eventRecorder.getTestStartedCount(), "# tests started");
		assertEquals(0, eventRecorder.getTestSuccessfulCount(), "# tests succeeded");
		assertEquals(0, eventRecorder.getTestSkippedCount(), "# tests skipped");
		assertEquals(0, eventRecorder.getTestAbortedCount(), "# tests aborted");
		assertEquals(1, eventRecorder.getTestFailedCount(), "# tests failed");

		// @formatter:off
		assertEquals(asList(
			"beforeEachMethod",
				"fooBeforeTestExecutionCallback",
					"test", // throws an exception.
				"fooAfterTestExecutionCallback",
			"afterEachMethod"
		), callSequence, "wrong call sequence");
		// @formatter:on

		assertNotNull(actualExceptionInAfterTestExecution, "test exception");
		assertTrue(actualExceptionInAfterTestExecution.isPresent(), "test exception should be present");
		assertEquals(EnigmaException.class, actualExceptionInAfterTestExecution.get().getClass());
	}

	// -------------------------------------------------------------------------

	@ExtendWith(FooTestExecutionCallbacks.class)
	static class ParentTestCase {
	}

	@ExtendWith(BarTestExecutionCallbacks.class)
	static class ChildTestCase extends ParentTestCase {

		@Test
		void test() {
			callSequence.add("testChild");
		}
	}

	@ExtendWith(FooTestExecutionCallbacks.class)
	private interface TestInterface {

		@Test
		default void defaultTest() {
			callSequence.add("defaultTestMethod");
		}
	}

	@ExtendWith(BarTestExecutionCallbacks.class)
	static class TestInterfaceTestCase implements TestInterface {

		@Test
		void localTest() {
			callSequence.add("localTestMethod");
		}
	}

	@ExtendWith({ FooTestExecutionCallbacks.class, BarTestExecutionCallbacks.class })
	static class OuterTestCase {

		@BeforeEach
		void beforeEach() {
			callSequence.add("beforeEachMethodOuter");
		}

		@Test
		void testOuter() {
			callSequence.add("testOuter");
		}

		@AfterEach
		void afterEach() {
			callSequence.add("afterEachMethodOuter");
		}

		@Nested
		@ExtendWith(FizzTestExecutionCallbacks.class)
		class InnerTestCase {

			@BeforeEach
			void beforeInnerMethod() {
				callSequence.add("beforeEachMethodInner");
			}

			@Test
			void testInner() {
				callSequence.add("testInner");
			}

			@AfterEach
			void afterInnerMethod() {
				callSequence.add("afterEachMethodInner");
			}
		}
	}

	@ExtendWith({ FooTestExecutionCallbacks.class, ExceptionThrowingBeforeTestExecutionCallback.class,
			BarTestExecutionCallbacks.class })
	static class ExceptionInBeforeTestExecutionCallbackTestCase {

		@BeforeEach
		void beforeEach() {
			callSequence.add("beforeEachMethod");
		}

		@Test
		void test() {
			callSequence.add("test");
		}

		@AfterEach
		void afterEach() {
			callSequence.add("afterEachMethod");
		}
	}

	@ExtendWith({ FooTestExecutionCallbacks.class, ExceptionThrowingAfterTestExecutionCallback.class,
			BarTestExecutionCallbacks.class })
	static class ExceptionInAfterTestExecutionCallbackTestCase {

		@BeforeEach
		void beforeEach() {
			callSequence.add("beforeEachMethod");
		}

		@Test
		void test() {
			callSequence.add("test");
		}

		@AfterEach
		void afterEach() {
			callSequence.add("afterEachMethod");
		}
	}

	@ExtendWith(FooTestExecutionCallbacks.class)
	static class ExceptionInBeforeEachMethodTestCase {

		@BeforeEach
		void beforeEach() {
			callSequence.add("beforeEachMethod");
			throw new EnigmaException("@BeforeEach");
		}

		@Test
		void test() {
			callSequence.add("test");
		}

		@AfterEach
		void afterEach() {
			callSequence.add("afterEachMethod");
		}
	}

	@ExtendWith(FooTestExecutionCallbacks.class)
	static class ExceptionInTestMethodTestCase {

		@BeforeEach
		void beforeEach() {
			callSequence.add("beforeEachMethod");
		}

		@Test
		void test() {
			callSequence.add("test");
			throw new EnigmaException("@Test");
		}

		@AfterEach
		void afterEach() {
			callSequence.add("afterEachMethod");
		}
	}

	// -------------------------------------------------------------------------

	static class FooTestExecutionCallbacks implements BeforeTestExecutionCallback, AfterTestExecutionCallback {

		@Override
		public void beforeTestExecution(ExtensionContext context) {
			callSequence.add("fooBeforeTestExecutionCallback");
		}

		@Override
		public void afterTestExecution(ExtensionContext context) {
			callSequence.add("fooAfterTestExecutionCallback");
			actualExceptionInAfterTestExecution = context.getExecutionException();
		}
	}

	static class BarTestExecutionCallbacks implements BeforeTestExecutionCallback, AfterTestExecutionCallback {

		@Override
		public void beforeTestExecution(ExtensionContext context) {
			callSequence.add("barBeforeTestExecutionCallback");
		}

		@Override
		public void afterTestExecution(ExtensionContext context) {
			callSequence.add("barAfterTestExecutionCallback");
		}
	}

	static class FizzTestExecutionCallbacks implements BeforeTestExecutionCallback, AfterTestExecutionCallback {

		@Override
		public void beforeTestExecution(ExtensionContext context) {
			callSequence.add("fizzBeforeTestExecutionCallback");
		}

		@Override
		public void afterTestExecution(ExtensionContext context) {
			callSequence.add("fizzAfterTestExecutionCallback");
		}
	}

	static class ExceptionThrowingBeforeTestExecutionCallback implements BeforeTestExecutionCallback {

		@Override
		public void beforeTestExecution(ExtensionContext context) {
			callSequence.add("exceptionThrowingBeforeTestExecutionCallback");
			throw new EnigmaException("BeforeTestExecutionCallback");
		}
	}

	static class ExceptionThrowingAfterTestExecutionCallback implements AfterTestExecutionCallback {

		@Override
		public void afterTestExecution(ExtensionContext context) {
			callSequence.add("exceptionThrowingAfterTestExecutionCallback");
			throw new EnigmaException("AfterTestExecutionCallback");
		}
	}

}
