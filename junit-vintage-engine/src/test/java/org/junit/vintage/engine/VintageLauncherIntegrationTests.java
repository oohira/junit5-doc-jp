/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.commons.util.CollectionUtils.getOnlyElement;
import static org.junit.platform.engine.FilterResult.excluded;
import static org.junit.platform.engine.FilterResult.includedIf;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.EngineFilter.includeEngines;
import static org.junit.platform.launcher.TagFilter.excludeTags;
import static org.junit.platform.launcher.TagFilter.includeTags;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;
import static org.junit.vintage.engine.descriptor.VintageTestDescriptor.ENGINE_ID;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.engine.TrackLogRecords;
import org.junit.platform.commons.logging.LogRecordListener;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.PostDiscoveryFilter;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.runners.Suite;
import org.junit.vintage.engine.descriptor.RunnerTestDescriptor;
import org.junit.vintage.engine.samples.junit4.Categories;
import org.junit.vintage.engine.samples.junit4.EnclosedJUnit4TestCase;
import org.junit.vintage.engine.samples.junit4.JUnit4SuiteOfSuiteWithFilterableChildRunner;
import org.junit.vintage.engine.samples.junit4.JUnit4SuiteWithTwoTestCases;
import org.junit.vintage.engine.samples.junit4.JUnit4TestCaseWithNotFilterableRunner;
import org.junit.vintage.engine.samples.junit4.NotFilterableRunner;
import org.junit.vintage.engine.samples.junit4.ParameterizedTestCase;
import org.junit.vintage.engine.samples.junit4.PlainJUnit4TestCaseWithFiveTestMethods;
import org.junit.vintage.engine.samples.junit4.PlainJUnit4TestCaseWithTwoTestMethods;

/**
 * @since 5.1
 */
class VintageLauncherIntegrationTests {

	@Test
	void executesOnlyTaggedMethodOfRegularTestClass() {
		Class<?> testClass = PlainJUnit4TestCaseWithFiveTestMethods.class;
		LauncherDiscoveryRequestBuilder request = request() //
				.selectors(selectClass(testClass)) //
				.filters(includeTags(Categories.Failing.class.getName()));

		TestPlan testPlan = discover(request);
		assertThat(testPlan.getDescendants(getOnlyElement(testPlan.getRoots()))).hasSize(2);

		Map<TestIdentifier, TestExecutionResult> results = execute(request);
		assertThat(results.keySet().stream().map(TestIdentifier::getDisplayName)) //
				.containsExactlyInAnyOrder("JUnit Vintage", testClass.getSimpleName(), "failingTest");
	}

	@Test
	void executesIncludedTaggedMethodOfNestedTestClass() {
		Class<?> testClass = EnclosedJUnit4TestCase.class;
		Class<?> nestedTestClass = EnclosedJUnit4TestCase.NestedClass.class;
		LauncherDiscoveryRequestBuilder request = request() //
				.selectors(selectClass(testClass)) //
				.filters(includeTags(Categories.Failing.class.getName()));

		TestPlan testPlan = discover(request);
		assertThat(testPlan.getDescendants(getOnlyElement(testPlan.getRoots()))).hasSize(3);

		Map<TestIdentifier, TestExecutionResult> results = execute(request);
		assertThat(results.keySet().stream().map(TestIdentifier::getDisplayName)) //
				.containsExactlyInAnyOrder("JUnit Vintage", testClass.getSimpleName(), nestedTestClass.getName(),
					"failingTest");
	}

	@Test
	void executesOnlyNotExcludedTaggedMethodOfNestedTestClass() {
		Class<?> testClass = EnclosedJUnit4TestCase.class;
		Class<?> nestedTestClass = EnclosedJUnit4TestCase.NestedClass.class;
		LauncherDiscoveryRequestBuilder request = request() //
				.selectors(selectClass(testClass)) //
				.filters(excludeTags(Categories.Failing.class.getName()));

		TestPlan testPlan = discover(request);
		assertThat(testPlan.getDescendants(getOnlyElement(testPlan.getRoots()))).hasSize(3);

		Map<TestIdentifier, TestExecutionResult> results = execute(request);
		assertThat(results.keySet().stream().map(TestIdentifier::getDisplayName)) //
				.containsExactlyInAnyOrder("JUnit Vintage", testClass.getSimpleName(), nestedTestClass.getName(),
					"successfulTest");
	}

	@Test
	void removesWholeSubtree() {
		Class<?> testClass = EnclosedJUnit4TestCase.class;
		LauncherDiscoveryRequestBuilder request = request() //
				.selectors(selectClass(testClass)) //
				.filters(excludeTags(Categories.Plain.class.getName()));

		TestPlan testPlan = discover(request);
		assertThat(testPlan.getDescendants(getOnlyElement(testPlan.getRoots()))).isEmpty();

		Map<TestIdentifier, TestExecutionResult> results = execute(request);
		assertThat(results.keySet().stream().map(TestIdentifier::getDisplayName)) //
				.containsExactlyInAnyOrder("JUnit Vintage");
	}

	@Test
	void removesCompleteClassIfNoMethodHasMatchingTags() {
		Class<?> testClass = PlainJUnit4TestCaseWithFiveTestMethods.class;
		LauncherDiscoveryRequestBuilder request = request() //
				.selectors(selectClass(testClass)) //
				.filters(includeTags("wrong-tag"));

		TestPlan testPlan = discover(request);
		assertThat(testPlan.getDescendants(getOnlyElement(testPlan.getRoots()))).isEmpty();

		Map<TestIdentifier, TestExecutionResult> results = execute(request);
		assertThat(results.keySet().stream().map(TestIdentifier::getDisplayName)) //
				.containsExactly("JUnit Vintage");
	}

	@Test
	void removesCompleteClassIfItHasExcludedTag() {
		Class<?> testClass = PlainJUnit4TestCaseWithFiveTestMethods.class;
		LauncherDiscoveryRequestBuilder request = request() //
				.selectors(selectClass(testClass)) //
				.filters(excludeTags(Categories.Plain.class.getName()));

		TestPlan testPlan = discover(request);
		assertThat(testPlan.getDescendants(getOnlyElement(testPlan.getRoots()))).isEmpty();

		Map<TestIdentifier, TestExecutionResult> results = execute(request);
		assertThat(results.keySet().stream().map(TestIdentifier::getDisplayName)) //
				.containsExactly("JUnit Vintage");
	}

	@TrackLogRecords
	@Test
	void executesAllTestsForNotFilterableRunner(LogRecordListener logRecordListener) {
		Class<?> testClass = JUnit4TestCaseWithNotFilterableRunner.class;
		LauncherDiscoveryRequestBuilder request = request() //
				.selectors(selectClass(testClass)) //
				.filters((PostDiscoveryFilter) descriptor -> includedIf(descriptor.getDisplayName().contains("#1")));

		TestPlan testPlan = discover(request);
		logRecordListener.clear();
		assertThat(testPlan.getDescendants(getOnlyElement(testPlan.getRoots()))).hasSize(3);

		Map<TestIdentifier, TestExecutionResult> results = execute(request);
		assertThat(results.keySet().stream().map(TestIdentifier::getDisplayName)) //
				.containsExactlyInAnyOrder("JUnit Vintage", testClass.getSimpleName(), "Test #0", "Test #1");
		assertThat(logRecordListener.stream(RunnerTestDescriptor.class, Level.WARNING).map(LogRecord::getMessage)) //
				.containsExactly(
					"Runner " + NotFilterableRunner.class.getName() + " (used on class " + testClass.getName() + ")" //
							+ " does not support filtering and will therefore be run completely.");
	}

	@TrackLogRecords
	@Test
	void executesAllTestsForNotFilterableChildRunnerOfSuite(LogRecordListener logRecordListener) {
		Class<?> suiteClass = JUnit4SuiteOfSuiteWithFilterableChildRunner.class;
		Class<?> testClass = JUnit4TestCaseWithNotFilterableRunner.class;
		LauncherDiscoveryRequestBuilder request = request() //
				.selectors(selectClass(suiteClass)) //
				.filters((PostDiscoveryFilter) descriptor -> includedIf(descriptor.getDisplayName().contains("#1")));

		TestPlan testPlan = discover(request);
		logRecordListener.clear();
		assertThat(testPlan.getDescendants(getOnlyElement(testPlan.getRoots()))).hasSize(4);

		Map<TestIdentifier, TestExecutionResult> results = execute(request);
		assertThat(results.keySet().stream().map(TestIdentifier::getDisplayName)) //
				.containsExactlyInAnyOrder("JUnit Vintage", suiteClass.getSimpleName(), testClass.getName(), "Test #0",
					"Test #1");
		assertThat(logRecordListener.stream(RunnerTestDescriptor.class, Level.WARNING).map(LogRecord::getMessage)) //
				.containsExactly("Runner " + Suite.class.getName() + " (used on class " + suiteClass.getName() + ")" //
						+ " was not able to satisfy all filter requests.");
	}

	@Test
	void executesOnlyTaggedMethodsForSuite() {
		Class<?> suiteClass = JUnit4SuiteWithTwoTestCases.class;
		Class<?> testClass = PlainJUnit4TestCaseWithTwoTestMethods.class;
		LauncherDiscoveryRequestBuilder request = request() //
				.selectors(selectClass(suiteClass)) //
				.filters(includeTags(Categories.Successful.class.getName()));

		TestPlan testPlan = discover(request);
		assertThat(testPlan.getDescendants(getOnlyElement(testPlan.getRoots()))).hasSize(3);

		Map<TestIdentifier, TestExecutionResult> results = execute(request);
		assertThat(results.keySet().stream().map(TestIdentifier::getDisplayName)) //
				.containsExactlyInAnyOrder("JUnit Vintage", suiteClass.getSimpleName(), testClass.getName(),
					"successfulTest");
	}

	@Test
	void removesCompleteClassWithNotFilterableRunnerIfItHasExcludedTag() {
		Class<?> testClass = JUnit4TestCaseWithNotFilterableRunner.class;
		LauncherDiscoveryRequestBuilder request = request() //
				.selectors(selectClass(testClass)) //
				.filters(excludeTags(Categories.Successful.class.getName()));

		TestPlan testPlan = discover(request);
		assertThat(testPlan.getDescendants(getOnlyElement(testPlan.getRoots()))).isEmpty();

		Map<TestIdentifier, TestExecutionResult> results = execute(request);
		assertThat(results.keySet().stream().map(TestIdentifier::getDisplayName)) //
				.containsExactly("JUnit Vintage");
	}

	@Test
	void filtersOutAllDescendantsOfParameterizedTestCase() {
		Class<?> testClass = ParameterizedTestCase.class;
		LauncherDiscoveryRequestBuilder request = request() //
				.selectors(selectClass(testClass)) //
				.filters((PostDiscoveryFilter) descriptor -> excluded("excluded"));

		TestPlan testPlan = discover(request);
		assertThat(testPlan.getDescendants(getOnlyElement(testPlan.getRoots()))).isEmpty();

		Map<TestIdentifier, TestExecutionResult> results = execute(request);
		assertThat(results.keySet().stream().map(TestIdentifier::getDisplayName)) //
				.containsExactly("JUnit Vintage");
	}

	private TestPlan discover(LauncherDiscoveryRequestBuilder requestBuilder) {
		Launcher launcher = LauncherFactory.create();
		return launcher.discover(toRequest(requestBuilder));
	}

	private Map<TestIdentifier, TestExecutionResult> execute(LauncherDiscoveryRequestBuilder requestBuilder) {
		Map<TestIdentifier, TestExecutionResult> results = new LinkedHashMap<>();
		LauncherDiscoveryRequest request = toRequest(requestBuilder);
		Launcher launcher = LauncherFactory.create();
		launcher.execute(request, new TestExecutionListener() {
			@Override
			public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
				results.put(testIdentifier, testExecutionResult);
			}
		});
		return results;
	}

	private LauncherDiscoveryRequest toRequest(LauncherDiscoveryRequestBuilder requestBuilder) {
		return requestBuilder.filters(includeEngines(ENGINE_ID)).build();
	}

}
