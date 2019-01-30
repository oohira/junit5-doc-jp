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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.junit.jupiter.api.MethodOrderer.Random.RANDOM_SEED_PROPERTY_NAME;
import static org.junit.jupiter.engine.Constants.DEFAULT_PARALLEL_EXECUTION_MODE;
import static org.junit.jupiter.engine.Constants.PARALLEL_EXECUTION_ENABLED_PROPERTY_NAME;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.MethodDescriptor;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.MethodOrderer.Alphanumeric;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.MethodOrderer.Random;
import org.junit.jupiter.api.MethodOrdererContext;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.jupiter.engine.TrackLogRecords;
import org.junit.platform.commons.logging.LogRecordListener;
import org.junit.platform.commons.util.ClassUtils;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Events;
import org.mockito.Mockito;

/**
 * Integration tests that verify support for custom test method execution order
 * in the {@link JupiterTestEngine}.
 *
 * @since 5.4
 */
class OrderedMethodTests {

	private static final Set<String> callSequence = Collections.synchronizedSet(new LinkedHashSet<>());
	private static final Set<String> threadNames = Collections.synchronizedSet(new LinkedHashSet<>());

	@BeforeEach
	void clearCallSequence() {
		callSequence.clear();
		threadNames.clear();
	}

	@Test
	void alphanumeric() {
		Class<?> testClass = AlphanumericTestCase.class;

		// The name of the base class MUST start with a letter alphanumerically
		// greater than "A" so that BaseTestCase comes after AlphanumericTestCase
		// if methods are sorted by class name for the fallback ordering if two
		// methods have the same name but different parameter lists. Note, however,
		// that Alphanumeric actually does not order methods like that, but we want
		// this check to remain in place to ensure that the ordering does not rely
		// on the class names.
		assertThat(testClass.getSuperclass().getName()).isGreaterThan(testClass.getName());

		var tests = executeTestsInParallel(AlphanumericTestCase.class);

		tests.assertStatistics(stats -> stats.succeeded(callSequence.size()));

		assertThat(callSequence).containsExactly("$()", "AAA()", "AAA(org.junit.jupiter.api.TestInfo)",
			"AAA(org.junit.jupiter.api.TestReporter)", "ZZ_Top()", "___()", "a1()", "a2()", "b()", "c()", "zzz()");
		assertThat(threadNames).hasSize(1);
	}

	@Test
	void orderAnnotation() {
		assertOrderAnnotationSupport(OrderAnnotationTestCase.class);
	}

	@Test
	void orderAnnotationInNestedTestClass() {
		assertOrderAnnotationSupport(OuterTestCase.class);
	}

	private void assertOrderAnnotationSupport(Class<?> testClass) {
		var tests = executeTestsInParallel(testClass);

		tests.assertStatistics(stats -> stats.succeeded(callSequence.size()));

		assertThat(callSequence).containsExactly("test1", "test2", "test3", "test4", "test5", "test6");
		assertThat(threadNames).hasSize(1);
	}

	@Test
	void random() {
		Set<String> uniqueSequences = new HashSet<>();

		for (int i = 0; i < 10; i++) {
			callSequence.clear();

			var tests = executeTestsInParallel(RandomTestCase.class);

			tests.assertStatistics(stats -> stats.succeeded(callSequence.size()));

			uniqueSequences.add(callSequence.stream().collect(Collectors.joining(",")));
		}

		// We assume that at least 3 out of 10 are different...
		assertThat(uniqueSequences).size().isGreaterThanOrEqualTo(3);
		// and that at least 2 different threads were used...
		assertThat(threadNames).size().isGreaterThanOrEqualTo(2);
	}

	@Test
	@TrackLogRecords
	void randomWithBogusSeed(LogRecordListener listener) {
		String seed = "explode";
		String expectedMessage = "Failed to convert configuration parameter [" + Random.RANDOM_SEED_PROPERTY_NAME
				+ "] with value [" + seed + "] to a long. Using System.nanoTime() as fallback.";

		Set<String> uniqueSequences = new HashSet<>();

		for (int i = 0; i < 10; i++) {
			callSequence.clear();
			listener.clear();

			var tests = executeTestsInParallelWithRandomSeed(RandomTestCase.class, seed);

			tests.assertStatistics(stats -> stats.succeeded(callSequence.size()));

			uniqueSequences.add(callSequence.stream().collect(Collectors.joining(",")));

			// @formatter:off
			assertTrue(listener.stream(Random.class, Level.WARNING)
				.map(LogRecord::getMessage)
				.anyMatch(expectedMessage::equals));
			// @formatter:on
		}

		// We assume that at least 3 out of 10 are different...
		assertThat(uniqueSequences).size().isGreaterThanOrEqualTo(3);
	}

	@Test
	@TrackLogRecords
	void randomWithCustomSeed(LogRecordListener listener) {
		String seed = "42";
		String expectedMessage = "Using custom seed for configuration parameter [" + Random.RANDOM_SEED_PROPERTY_NAME
				+ "] with value [" + seed + "].";

		for (int i = 0; i < 10; i++) {
			callSequence.clear();
			listener.clear();

			var tests = executeTestsInParallelWithRandomSeed(RandomTestCase.class, seed);

			tests.assertStatistics(stats -> stats.succeeded(callSequence.size()));

			// With a custom seed, the "randomness" must be the same for every iteration.
			assertThat(callSequence).containsExactly("test2()", "test3()", "test4()", "repetition 1 of 1", "test1()");

			// @formatter:off
			assertTrue(listener.stream(Random.class, Level.CONFIG)
				.map(LogRecord::getMessage)
				.anyMatch(expectedMessage::equals));
			// @formatter:on
		}
	}

	@Test
	@TrackLogRecords
	void misbehavingMethodOrdererThatAddsElements(LogRecordListener listener) {
		Class<?> testClass = MisbehavingByAddingTestCase.class;

		executeTestsInParallel(testClass).assertStatistics(stats -> stats.succeeded(2));

		assertThat(callSequence).containsExactlyInAnyOrder("test1()", "test2()");

		String expectedMessage = "MethodOrderer [" + MisbehavingByAdding.class.getName()
				+ "] added 2 MethodDescriptor(s) for test class [" + testClass.getName() + "] which will be ignored.";

		assertExpectedLogMessage(listener, expectedMessage);
	}

	@Test
	@TrackLogRecords
	void misbehavingMethodOrdererThatRemovesElements(LogRecordListener listener) {
		Class<?> testClass = MisbehavingByRemovingTestCase.class;

		executeTestsInParallel(testClass).assertStatistics(stats -> stats.succeeded(3));

		assertThat(callSequence).containsExactlyInAnyOrder("test1()", "test2()", "test3()");

		String expectedMessage = "MethodOrderer [" + MisbehavingByRemoving.class.getName()
				+ "] removed 2 MethodDescriptor(s) for test class [" + testClass.getName()
				+ "] which will be retained with arbitrary ordering.";

		assertExpectedLogMessage(listener, expectedMessage);
	}

	private void assertExpectedLogMessage(LogRecordListener listener, String expectedMessage) {
		// @formatter:off
		assertTrue(listener.stream(Level.WARNING)
			.map(LogRecord::getMessage)
			.anyMatch(expectedMessage::equals));
		// @formatter:on
	}

	private Events executeTestsInParallel(Class<?> testClass) {
		// @formatter:off
		return EngineTestKit
				.engine("junit-jupiter")
				.configurationParameter(PARALLEL_EXECUTION_ENABLED_PROPERTY_NAME, "true")
				.configurationParameter(DEFAULT_PARALLEL_EXECUTION_MODE, "concurrent")
				.selectors(selectClass(testClass))
				.execute()
				.tests();
		// @formatter:on
	}

	private Events executeTestsInParallelWithRandomSeed(Class<?> testClass, String seed) {
		var configurationParameters = Map.of(//
			PARALLEL_EXECUTION_ENABLED_PROPERTY_NAME, "true", //
			RANDOM_SEED_PROPERTY_NAME, seed //
		);

		// @formatter:off
		return EngineTestKit
				.engine("junit-jupiter")
				.configurationParameters(configurationParameters)
				.selectors(selectClass(testClass))
				.execute()
				.tests();
		// @formatter:on
	}

	// -------------------------------------------------------------------------

	static class BaseTestCase {

		@Test
		void AAA() {
		}

		@Test
		void c() {
		}

	}

	@TestMethodOrder(Alphanumeric.class)
	static class AlphanumericTestCase extends BaseTestCase {

		@BeforeEach
		void trackInvocations(TestInfo testInfo) {
			var method = testInfo.getTestMethod().get();
			var signature = String.format("%s(%s)", method.getName(),
				ClassUtils.nullSafeToString(method.getParameterTypes()));

			callSequence.add(signature);
			threadNames.add(Thread.currentThread().getName());
		}

		@TestFactory
		DynamicTest b() {
			return dynamicTest("dynamic", () -> {
			});
		}

		@Test
		void $() {
		}

		@Test
		void ___() {
		}

		@Test
		void AAA(TestReporter testReporter) {
		}

		@Test
		void AAA(TestInfo testInfo) {
		}

		@Test
		void ZZ_Top() {
		}

		@Test
		void a1() {
		}

		@Test
		void a2() {
		}

		@RepeatedTest(1)
		void zzz() {
		}
	}

	@TestMethodOrder(OrderAnnotation.class)
	static class OrderAnnotationTestCase {

		@BeforeEach
		void trackInvocations(TestInfo testInfo) {
			callSequence.add(testInfo.getDisplayName());
			threadNames.add(Thread.currentThread().getName());
		}

		@Test
		@DisplayName("test6")
		// @Order(6)
		void defaultOrderValue() {
		}

		@Test
		@DisplayName("test3")
		@Order(3)
		void $() {
		}

		@Test
		@DisplayName("test5")
		@Order(5)
		void AAA() {
		}

		@TestFactory
		@DisplayName("test4")
		@Order(4)
		DynamicTest aaa() {
			return dynamicTest("test4", () -> {
			});
		}

		@Test
		@DisplayName("test1")
		@Order(1)
		void zzz() {
		}

		@RepeatedTest(value = 1, name = "{displayName}")
		@DisplayName("test2")
		@Order(2)
		void ___() {
		}
	}

	static class OuterTestCase {

		@Nested
		class NestedOrderAnnotationTestCase extends OrderAnnotationTestCase {
		}
	}

	@TestMethodOrder(Random.class)
	static class RandomTestCase {

		@BeforeEach
		void trackInvocations(TestInfo testInfo) {
			callSequence.add(testInfo.getDisplayName());
			threadNames.add(Thread.currentThread().getName());
		}

		@Test
		void test1() {
		}

		@Test
		void test2() {
		}

		@Test
		void test3() {
		}

		@TestFactory
		DynamicTest test4() {
			return dynamicTest("dynamic", () -> {
			});
		}

		@RepeatedTest(1)
		void test5() {
		}
	}

	@TestMethodOrder(MisbehavingByAdding.class)
	static class MisbehavingByAddingTestCase {

		@BeforeEach
		void trackInvocations(TestInfo testInfo) {
			callSequence.add(testInfo.getDisplayName());
		}

		@Test
		void test1() {
		}

		@Test
		void test2() {
		}
	}

	@TestMethodOrder(MisbehavingByRemoving.class)
	static class MisbehavingByRemovingTestCase {

		@BeforeEach
		void trackInvocations(TestInfo testInfo) {
			callSequence.add(testInfo.getDisplayName());
		}

		@Test
		void test1() {
		}

		@Test
		void test2() {
		}

		@Test
		void test3() {
		}
	}

	static class MisbehavingByAdding implements MethodOrderer {

		@Override
		public void orderMethods(MethodOrdererContext context) {
			context.getMethodDescriptors().add(mock(MethodDescriptor.class));
			context.getMethodDescriptors().add(mock(MethodDescriptor.class));
		}

		@SuppressWarnings("unchecked")
		static <T> T mock(Class<? super T> type) {
			return (T) Mockito.mock(type);
		}

	}

	static class MisbehavingByRemoving implements MethodOrderer {

		@Override
		public void orderMethods(MethodOrdererContext context) {
			context.getMethodDescriptors().remove(0);
			context.getMethodDescriptors().remove(0);
		}
	}

}
