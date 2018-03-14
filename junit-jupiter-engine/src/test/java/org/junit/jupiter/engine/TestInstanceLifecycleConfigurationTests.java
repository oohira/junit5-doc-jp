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

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_METHOD;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.test.event.ExecutionEventRecorder;
import org.junit.platform.launcher.Launcher;

/**
 * Integration tests for {@link TestInstance @TestInstance} lifecycle
 * configuration support, not to be confused with {@link TestInstanceLifecycleTests}.
 *
 * <p>Specifically, this class tests custom lifecycle configuration specified
 * via {@code @TestInstance} as well as via {@link ConfigurationParameters}
 * supplied to the {@link Launcher} or via a JVM system property.
 *
 * @since 5.0
 * @see TestInstanceLifecycleTests
 */
class TestInstanceLifecycleConfigurationTests extends AbstractJupiterTestEngineTests {

	private static final String KEY = Constants.DEFAULT_TEST_INSTANCE_LIFECYCLE_PROPERTY_NAME;

	private static final List<String> methodsInvoked = new ArrayList<>();

	@BeforeEach
	@AfterEach
	void reset() {
		methodsInvoked.clear();
		System.clearProperty(KEY);
	}

	@Test
	void instancePerMethodUsingStandardDefaultConfiguration() {
		performAssertions(AssumedInstancePerTestMethodTestCase.class, 2, 0, 1, "beforeAll", "test", "afterAll");
	}

	@Test
	void instancePerClassConfiguredViaExplicitAnnotationDeclaration() {
		performAssertions(ExplicitInstancePerClassTestCase.class, 2, 0, 1, "beforeAll", "test", "afterAll");
	}

	@Test
	void instancePerClassConfiguredViaSystemProperty() {
		Class<?> testClass = AssumedInstancePerClassTestCase.class;

		// Should fail by default...
		performAssertions(testClass, 2, 1, 0);

		// Should pass with the system property set
		System.setProperty(KEY, PER_CLASS.name());
		performAssertions(testClass, 2, 0, 1, "beforeAll", "test", "afterAll");
	}

	@Test
	void instancePerClassConfiguredViaConfigParam() {
		Class<?> testClass = AssumedInstancePerClassTestCase.class;

		// Should fail by default...
		performAssertions(testClass, 2, 1, 0);

		// Should pass with the config param
		performAssertions(testClass, singletonMap(KEY, PER_CLASS.name()), 2, 0, 1, "beforeAll", "test", "afterAll");
	}

	@Test
	void instancePerClassConfiguredViaConfigParamThatOverridesSystemProperty() {
		Class<?> testClass = AssumedInstancePerClassTestCase.class;

		// Should fail with system property
		System.setProperty(KEY, PER_METHOD.name());
		performAssertions(testClass, 2, 1, 0);

		// Should pass with the config param
		performAssertions(testClass, singletonMap(KEY, PER_CLASS.name()), 2, 0, 1, "beforeAll", "test", "afterAll");
	}

	@Test
	void instancePerMethodConfiguredViaExplicitAnnotationDeclarationThatOverridesSystemProperty() {
		System.setProperty(KEY, PER_CLASS.name());
		performAssertions(ExplicitInstancePerTestMethodTestCase.class, 2, 0, 1, "beforeAll", "test", "afterAll");
	}

	@Test
	void instancePerMethodConfiguredViaExplicitAnnotationDeclarationThatOverridesConfigParam() {
		Class<?> testClass = ExplicitInstancePerTestMethodTestCase.class;
		performAssertions(testClass, singletonMap(KEY, PER_CLASS.name()), 2, 0, 1, "beforeAll", "test", "afterAll");
	}

	private void performAssertions(Class<?> testClass, int containers, int containersFailed, int tests,
			String... methods) {

		performAssertions(testClass, emptyMap(), containers, containersFailed, tests, methods);
	}

	private void performAssertions(Class<?> testClass, Map<String, String> configParams, int containers,
			int failedContainers, int tests, String... methods) {

		// @formatter:off
		ExecutionEventRecorder eventRecorder = executeTests(
			request()
				.selectors(selectClass(testClass))
				.configurationParameters(configParams)
				.build()
		);

		assertAll(
			() -> assertEquals(containers, eventRecorder.getContainerStartedCount(), "# containers started"),
			() -> assertEquals(containers, eventRecorder.getContainerFinishedCount(), "# containers finished"),
			() -> assertEquals(failedContainers, eventRecorder.getContainerFailedCount(), "# containers failed"),
			() -> assertEquals(tests, eventRecorder.getTestStartedCount(), "# tests started"),
			() -> assertEquals(tests, eventRecorder.getTestSuccessfulCount(), "# tests succeeded"),
			() -> assertEquals(Arrays.asList(methods), methodsInvoked)
		);
		// @formatter:on
	}

	// -------------------------------------------------------------------------

	@TestInstance(PER_METHOD)
	static class ExplicitInstancePerTestMethodTestCase {

		@BeforeAll
		static void beforeAll() {
			methodsInvoked.add("beforeAll");
		}

		@Test
		void test() {
			methodsInvoked.add("test");
		}

		@AfterAll
		static void afterAllStatic() {
			methodsInvoked.add("afterAll");
		}

	}

	/**
	 * "per-method" lifecycle mode is assumed since the {@code @BeforeAll} and
	 * {@code @AfterAll} methods are static, even though there is no explicit
	 * {@code @TestInstance} declaration.
	 */
	static class AssumedInstancePerTestMethodTestCase {

		@BeforeAll
		static void beforeAll() {
			methodsInvoked.add("beforeAll");
		}

		@Test
		void test() {
			methodsInvoked.add("test");
		}

		@AfterAll
		static void afterAllStatic() {
			methodsInvoked.add("afterAll");
		}

	}

	@TestInstance(PER_CLASS)
	static class ExplicitInstancePerClassTestCase {

		@BeforeAll
		void beforeAll(TestInfo testInfo) {
			methodsInvoked.add("beforeAll");
		}

		@Test
		void test() {
			methodsInvoked.add("test");
		}

		@AfterAll
		void afterAll(TestInfo testInfo) {
			methodsInvoked.add("afterAll");
		}

	}

	/**
	 * "per-class" lifecycle mode is assumed since the {@code @BeforeAll} and
	 * {@code @AfterAll} methods are non-static, even though there is no
	 * explicit {@code @TestInstance} declaration.
	 */
	static class AssumedInstancePerClassTestCase {

		@BeforeAll
		void beforeAll(TestInfo testInfo) {
			methodsInvoked.add("beforeAll");
		}

		@Test
		void test() {
			methodsInvoked.add("test");
		}

		@AfterAll
		void afterAll(TestInfo testInfo) {
			methodsInvoked.add("afterAll");
		}

	}

}
