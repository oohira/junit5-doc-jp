/*
 * Copyright 2015-2019 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.core;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.commons.util.CollectionUtils.getOnlyElement;
import static org.junit.platform.engine.TestExecutionResult.successful;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;
import static org.junit.platform.launcher.EngineFilter.excludeEngines;
import static org.junit.platform.launcher.EngineFilter.includeEngines;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;
import static org.junit.platform.launcher.core.LauncherFactoryForTestingPurposesOnly.createLauncher;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.engine.TrackLogRecords;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.commons.logging.LogRecordListener;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.junit.platform.engine.support.hierarchical.DemoHierarchicalTestDescriptor;
import org.junit.platform.engine.support.hierarchical.DemoHierarchicalTestEngine;
import org.junit.platform.fakes.TestDescriptorStub;
import org.junit.platform.fakes.TestEngineSpy;
import org.junit.platform.fakes.TestEngineStub;
import org.junit.platform.launcher.PostDiscoveryFilter;
import org.junit.platform.launcher.PostDiscoveryFilterStub;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

/**
 * @since 1.0
 */
@TrackLogRecords
class DefaultLauncherTests {

	private static final String FOO = DefaultLauncherTests.class.getSimpleName() + ".foo";
	private static final String BAR = DefaultLauncherTests.class.getSimpleName() + ".bar";

	private static final Runnable noOp = () -> {
	};

	@Test
	void constructLauncherWithoutAnyEngines() {
		Throwable exception = assertThrows(PreconditionViolationException.class, () -> createLauncher());

		assertThat(exception).hasMessageContaining("Cannot create Launcher without at least one TestEngine");
	}

	@Test
	void constructLauncherWithMultipleTestEnginesWithDuplicateIds() {
		JUnitException exception = assertThrows(JUnitException.class,
			() -> createLauncher(new DemoHierarchicalTestEngine("dummy id"),
				new DemoHierarchicalTestEngine("dummy id")));

		assertThat(exception).hasMessageContaining("multiple engines with the same ID");
	}

	@Test
	void registerTestExecutionListenersWithNullArray() {
		DefaultLauncher launcher = createLauncher(new DemoHierarchicalTestEngine("dummy id"));

		PreconditionViolationException exception = assertThrows(PreconditionViolationException.class,
			() -> launcher.registerTestExecutionListeners((TestExecutionListener[]) null));

		assertThat(exception).hasMessageContaining("listeners array must not be null or empty");
	}

	@Test
	void registerTestExecutionListenersWithEmptyArray() {
		DefaultLauncher launcher = createLauncher(new DemoHierarchicalTestEngine("dummy id"));

		PreconditionViolationException exception = assertThrows(PreconditionViolationException.class,
			() -> launcher.registerTestExecutionListeners(new TestExecutionListener[0]));

		assertThat(exception).hasMessageContaining("listeners array must not be null or empty");
	}

	@Test
	void registerTestExecutionListenersWithArrayContainingNullElements() {
		DefaultLauncher launcher = createLauncher(new DemoHierarchicalTestEngine("dummy id"));

		PreconditionViolationException exception = assertThrows(PreconditionViolationException.class,
			() -> launcher.registerTestExecutionListeners(new TestExecutionListener[] { null }));

		assertThat(exception).hasMessageContaining("individual listeners must not be null");
	}

	@Test
	void discoverEmptyTestPlanWithEngineWithoutAnyTests() {
		DefaultLauncher launcher = createLauncher(new DemoHierarchicalTestEngine());

		TestPlan testPlan = launcher.discover(request().build());

		assertThat(testPlan.getRoots()).hasSize(1);
	}

	@Test
	void discoverTestPlanForEngineThatReturnsNullForItsRootDescriptor() {
		TestEngine engine = new TestEngineStub() {

			@Override
			public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
				return null;
			}
		};

		TestPlan testPlan = createLauncher(engine).discover(request().build());
		assertThat(testPlan.getRoots()).hasSize(0);
	}

	@Test
	void discoverTestPlanForEngineThatThrowsAnErrorInDiscoveryPhase() {
		TestEngine engine = new TestEngineStub() {

			@Override
			public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
				throw new Error("ignored");
			}
		};

		TestPlan testPlan = createLauncher(engine).discover(request().build());
		assertThat(testPlan.getRoots()).hasSize(0);
	}

	@Test
	void discoverTestPlanForEngineThatThrowsRuntimeExceptionInDiscoveryPhase() {
		TestEngine engine = new TestEngineStub() {

			@Override
			public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
				throw new RuntimeException("ignored");
			}
		};

		TestPlan testPlan = createLauncher(engine).discover(request().build());
		assertThat(testPlan.getRoots()).hasSize(0);
	}

	@Test
	void discoverTestPlanForSingleEngine() {
		DemoHierarchicalTestEngine engine = new DemoHierarchicalTestEngine("myEngine");
		engine.addTest("test1", noOp);
		engine.addTest("test2", noOp);

		DefaultLauncher launcher = createLauncher(engine);

		TestPlan testPlan = launcher.discover(request().selectors(selectPackage("any")).build());

		assertThat(testPlan.getRoots()).hasSize(1);
		TestIdentifier rootIdentifier = testPlan.getRoots().iterator().next();
		assertThat(testPlan.getChildren(rootIdentifier.getUniqueId())).hasSize(2);
		assertThat(testPlan.getChildren("[engine:myEngine]")).hasSize(2);
	}

	@Test
	void discoverTestPlanForMultipleEngines() {
		DemoHierarchicalTestEngine firstEngine = new DemoHierarchicalTestEngine("engine1");
		TestDescriptor test1 = firstEngine.addTest("test1", noOp);
		DemoHierarchicalTestEngine secondEngine = new DemoHierarchicalTestEngine("engine2");
		TestDescriptor test2 = secondEngine.addTest("test2", noOp);

		DefaultLauncher launcher = createLauncher(firstEngine, secondEngine);

		TestPlan testPlan = launcher.discover(
			request().selectors(selectUniqueId(test1.getUniqueId()), selectUniqueId(test2.getUniqueId())).build());

		assertThat(testPlan.getRoots()).hasSize(2);
		assertThat(testPlan.getChildren(UniqueId.forEngine("engine1").toString())).hasSize(1);
		assertThat(testPlan.getChildren(UniqueId.forEngine("engine2").toString())).hasSize(1);
	}

	@Test
	void launcherWillNotExecuteEnginesIfNotIncludedByAnEngineFilter() {
		DemoHierarchicalTestEngine firstEngine = new DemoHierarchicalTestEngine("first");
		TestDescriptor test1 = firstEngine.addTest("test1", noOp);
		DemoHierarchicalTestEngine secondEngine = new DemoHierarchicalTestEngine("second");
		TestDescriptor test2 = secondEngine.addTest("test2", noOp);

		DefaultLauncher launcher = createLauncher(firstEngine, secondEngine);

		// @formatter:off
		TestPlan testPlan = launcher.discover(
			request()
				.selectors(selectUniqueId(test1.getUniqueId()), selectUniqueId(test2.getUniqueId()))
				.filters(includeEngines("first"))
				.build());
		// @formatter:on

		assertThat(testPlan.getRoots()).hasSize(1);
		TestIdentifier rootIdentifier = testPlan.getRoots().iterator().next();
		assertThat(testPlan.getChildren(rootIdentifier.getUniqueId())).hasSize(1);
		assertThat(testPlan.getChildren(UniqueId.forEngine("first").toString())).hasSize(1);
	}

	@Test
	void launcherWillExecuteAllEnginesExplicitlyIncludedViaSingleEngineFilter() {
		DemoHierarchicalTestEngine firstEngine = new DemoHierarchicalTestEngine("first");
		TestDescriptor test1 = firstEngine.addTest("test1", noOp);
		DemoHierarchicalTestEngine secondEngine = new DemoHierarchicalTestEngine("second");
		TestDescriptor test2 = secondEngine.addTest("test2", noOp);

		DefaultLauncher launcher = createLauncher(firstEngine, secondEngine);

		// @formatter:off
		TestPlan testPlan = launcher.discover(
			request()
				.selectors(selectUniqueId(test1.getUniqueId()), selectUniqueId(test2.getUniqueId()))
				.filters(includeEngines("first", "second"))
				.build());
		// @formatter:on

		assertThat(testPlan.getRoots()).hasSize(2);
	}

	@Test
	void launcherWillNotExecuteEnginesExplicitlyIncludedViaMultipleCompetingEngineFilters() {
		DemoHierarchicalTestEngine firstEngine = new DemoHierarchicalTestEngine("first");
		TestDescriptor test1 = firstEngine.addTest("test1", noOp);
		DemoHierarchicalTestEngine secondEngine = new DemoHierarchicalTestEngine("second");
		TestDescriptor test2 = secondEngine.addTest("test2", noOp);

		DefaultLauncher launcher = createLauncher(firstEngine, secondEngine);

		// @formatter:off
		TestPlan testPlan = launcher.discover(
			request()
				.selectors(selectUniqueId(test1.getUniqueId()), selectUniqueId(test2.getUniqueId()))
				.filters(includeEngines("first"), includeEngines("second"))
				.build());
		// @formatter:on

		assertThat(testPlan.getRoots()).isEmpty();
	}

	@Test
	void launcherWillNotExecuteEnginesExplicitlyExcludedByAnEngineFilter() {
		DemoHierarchicalTestEngine firstEngine = new DemoHierarchicalTestEngine("first");
		TestDescriptor test1 = firstEngine.addTest("test1", noOp);
		DemoHierarchicalTestEngine secondEngine = new DemoHierarchicalTestEngine("second");
		TestDescriptor test2 = secondEngine.addTest("test2", noOp);

		DefaultLauncher launcher = createLauncher(firstEngine, secondEngine);

		// @formatter:off
		TestPlan testPlan = launcher.discover(
			request()
				.selectors(selectUniqueId(test1.getUniqueId()), selectUniqueId(test2.getUniqueId()))
				.filters(excludeEngines("second"))
				.build());
		// @formatter:on

		assertThat(testPlan.getRoots()).hasSize(1);
		TestIdentifier rootIdentifier = testPlan.getRoots().iterator().next();
		assertThat(testPlan.getChildren(rootIdentifier.getUniqueId())).hasSize(1);
		assertThat(testPlan.getChildren(UniqueId.forEngine("first").toString())).hasSize(1);
	}

	@Test
	void launcherWillExecuteEnginesHonoringBothIncludeAndExcludeEngineFilters() {
		DemoHierarchicalTestEngine firstEngine = new DemoHierarchicalTestEngine("first");
		TestDescriptor test1 = firstEngine.addTest("test1", noOp);
		DemoHierarchicalTestEngine secondEngine = new DemoHierarchicalTestEngine("second");
		TestDescriptor test2 = secondEngine.addTest("test2", noOp);
		DemoHierarchicalTestEngine thirdEngine = new DemoHierarchicalTestEngine("third");
		TestDescriptor test3 = thirdEngine.addTest("test3", noOp);

		DefaultLauncher launcher = createLauncher(firstEngine, secondEngine, thirdEngine);

		// @formatter:off
		TestPlan testPlan = launcher.discover(
			request()
				.selectors(selectUniqueId(test1.getUniqueId()), selectUniqueId(test2.getUniqueId()), selectUniqueId(test3.getUniqueId()))
				.filters(includeEngines("first", "second"), excludeEngines("second"))
				.build());
		// @formatter:on

		assertThat(testPlan.getRoots()).hasSize(1);
		TestIdentifier rootIdentifier = testPlan.getRoots().iterator().next();
		assertThat(testPlan.getChildren(rootIdentifier.getUniqueId())).hasSize(1);
		assertThat(testPlan.getChildren(UniqueId.forEngine("first").toString())).hasSize(1);
	}

	@Test
	void launcherAppliesPostDiscoveryFilters() {
		DemoHierarchicalTestEngine engine = new DemoHierarchicalTestEngine("myEngine");
		DemoHierarchicalTestDescriptor test1 = engine.addTest("test1", noOp);
		engine.addTest("test2", noOp);

		DefaultLauncher launcher = createLauncher(engine);

		PostDiscoveryFilter includeWithUniqueIdContainsTest = new PostDiscoveryFilterStub(
			descriptor -> FilterResult.includedIf(descriptor.getUniqueId().toString().contains("test")),
			() -> "filter1");
		PostDiscoveryFilter includeWithUniqueIdContains1 = new PostDiscoveryFilterStub(
			descriptor -> FilterResult.includedIf(descriptor.getUniqueId().toString().contains("1")), () -> "filter2");

		TestPlan testPlan = launcher.discover( //
			request() //
					.selectors(selectPackage("any")) //
					.filters(includeWithUniqueIdContainsTest, includeWithUniqueIdContains1) //
					.build());

		assertThat(testPlan.getChildren(UniqueId.forEngine("myEngine").toString())).hasSize(1);
		assertThat(testPlan.getTestIdentifier(test1.getUniqueId().toString())).isNotNull();
	}

	@Test
	void withoutConfigurationParameters_LauncherPassesEmptyConfigurationParametersIntoTheExecutionRequest() {
		TestEngineSpy engine = new TestEngineSpy();

		DefaultLauncher launcher = createLauncher(engine);
		launcher.execute(request().build());

		ConfigurationParameters configurationParameters = engine.requestForExecution.getConfigurationParameters();
		assertThat(configurationParameters.get("key").isPresent()).isFalse();
		assertThat(configurationParameters.size()).isEqualTo(0);
	}

	@Test
	void withConfigurationParameters_LauncherPassesPopulatedConfigurationParametersIntoTheExecutionRequest() {
		TestEngineSpy engine = new TestEngineSpy();

		DefaultLauncher launcher = createLauncher(engine);
		launcher.execute(request().configurationParameter("key", "value").build());

		ConfigurationParameters configurationParameters = engine.requestForExecution.getConfigurationParameters();
		assertThat(configurationParameters.size()).isEqualTo(1);
		assertThat(configurationParameters.get("key").isPresent()).isTrue();
		assertThat(configurationParameters.get("key").get()).isEqualTo("value");
	}

	@Test
	void withoutConfigurationParameters_LookupFallsBackToSystemProperty() {
		System.setProperty(FOO, BAR);

		try {
			TestEngineSpy engine = new TestEngineSpy();

			DefaultLauncher launcher = createLauncher(engine);
			launcher.execute(request().build());

			ConfigurationParameters configurationParameters = engine.requestForExecution.getConfigurationParameters();
			assertThat(configurationParameters.size()).isEqualTo(0);
			Optional<String> optionalFoo = configurationParameters.get(FOO);
			assertTrue(optionalFoo.isPresent(), "foo should have been picked up via system property");
			assertEquals(BAR, optionalFoo.get(), "foo property");
		}
		finally {
			System.clearProperty(FOO);
		}
	}

	@Test
	void withAdditionalListener() {
		TestEngineSpy engine = new TestEngineSpy();
		SummaryGeneratingListener listener = new SummaryGeneratingListener();

		DefaultLauncher launcher = createLauncher(engine);
		launcher.execute(request().build(), listener);

		assertThat(listener.getSummary()).isNotNull();
		assertThat(listener.getSummary().getContainersFoundCount()).isEqualTo(1);
		assertThat(listener.getSummary().getTestsFoundCount()).isEqualTo(1);
	}

	@Test
	void prunesTestDescriptorsAfterApplyingPostDiscoveryFilters() {
		TestEngineSpy engine = new TestEngineSpy() {

			@Override
			public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
				super.discover(discoveryRequest, uniqueId);
				TestDescriptorStub engineDescriptor = new TestDescriptorStub(uniqueId, uniqueId.toString());
				TestDescriptorStub containerDescriptor = new TestDescriptorStub(uniqueId.append("container", "a"),
					"container") {

					@Override
					public Type getType() {
						return Type.CONTAINER;
					}
				};
				containerDescriptor.addChild(
					new TestDescriptorStub(containerDescriptor.getUniqueId().append("test", "b"), "test"));
				engineDescriptor.addChild(containerDescriptor);
				return engineDescriptor;
			}
		};

		DefaultLauncher launcher = createLauncher(engine);
		TestPlan testPlan = launcher.discover(request().filters(
			(PostDiscoveryFilter) testDescriptor -> FilterResult.includedIf(testDescriptor.isContainer())).build());

		assertThat(testPlan.getRoots()).hasSize(1);
		TestIdentifier engineIdentifier = getOnlyElement(testPlan.getRoots());
		assertThat(testPlan.getChildren(engineIdentifier)).isEmpty();
	}

	@Test
	void reportsDynamicTestDescriptorsCorrectly() {
		UniqueId engineId = UniqueId.forEngine(TestEngineSpy.ID);
		UniqueId containerAndTestId = engineId.append("c&t", "c&t");
		UniqueId dynamicTestId = containerAndTestId.append("test", "test");

		TestEngineSpy engine = new TestEngineSpy() {

			@Override
			public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
				super.discover(discoveryRequest, uniqueId);
				TestDescriptorStub engineDescriptor = new TestDescriptorStub(uniqueId, uniqueId.toString());
				engineDescriptor.addChild(new TestDescriptorStub(containerAndTestId, "c&t") {

					@Override
					public Type getType() {
						return Type.CONTAINER_AND_TEST;
					}
				});
				return engineDescriptor;
			}

			@Override
			public void execute(ExecutionRequest request) {
				super.execute(request);
				EngineExecutionListener listener = request.getEngineExecutionListener();

				listener.executionStarted(request.getRootTestDescriptor());
				TestDescriptor containerAndTest = getOnlyElement(request.getRootTestDescriptor().getChildren());
				listener.executionStarted(containerAndTest);

				TestDescriptorStub dynamicTest = new TestDescriptorStub(dynamicTestId, "test");
				dynamicTest.setParent(containerAndTest);
				listener.dynamicTestRegistered(dynamicTest);
				listener.executionStarted(dynamicTest);
				listener.executionFinished(dynamicTest, successful());

				listener.executionFinished(containerAndTest, successful());
				listener.executionFinished(request.getRootTestDescriptor(), successful());
			}
		};

		DefaultLauncher launcher = createLauncher(engine);
		TestExecutionListener listener = mock(TestExecutionListener.class);
		launcher.execute(request().build(), listener);

		InOrder inOrder = inOrder(listener);
		ArgumentCaptor<TestPlan> testPlanArgumentCaptor = ArgumentCaptor.forClass(TestPlan.class);
		inOrder.verify(listener).testPlanExecutionStarted(testPlanArgumentCaptor.capture());

		TestPlan testPlan = testPlanArgumentCaptor.getValue();
		TestIdentifier engineTestIdentifier = testPlan.getTestIdentifier(engineId.toString());
		TestIdentifier containerAndTestIdentifier = testPlan.getTestIdentifier(containerAndTestId.toString());
		TestIdentifier dynamicTestIdentifier = testPlan.getTestIdentifier(dynamicTestId.toString());
		assertThat(engineTestIdentifier.getParentId()).isEmpty();
		assertThat(containerAndTestIdentifier.getParentId()).contains(engineId.toString());
		assertThat(dynamicTestIdentifier.getParentId()).contains(containerAndTestId.toString());

		inOrder.verify(listener).executionStarted(engineTestIdentifier);
		inOrder.verify(listener).executionStarted(containerAndTestIdentifier);
		inOrder.verify(listener).dynamicTestRegistered(dynamicTestIdentifier);
		inOrder.verify(listener).executionStarted(dynamicTestIdentifier);
		inOrder.verify(listener).executionFinished(dynamicTestIdentifier, successful());
		inOrder.verify(listener).executionFinished(containerAndTestIdentifier, successful());
		inOrder.verify(listener).executionFinished(engineTestIdentifier, successful());
		inOrder.verify(listener).testPlanExecutionFinished(same(testPlan));
	}

	@Test
	void launcherCanExecuteTestPlan() {
		TestEngine engine = mock(TestEngine.class);
		when(engine.getId()).thenReturn("some-engine");
		when(engine.discover(any(), any())).thenAnswer(invocation -> {
			UniqueId uniqueId = invocation.getArgument(1);
			return new EngineDescriptor(uniqueId, uniqueId.toString());
		});

		DefaultLauncher launcher = createLauncher(engine);
		TestPlan testPlan = launcher.discover(request().build());
		verify(engine, times(1)).discover(any(), any());

		launcher.execute(testPlan);
		verify(engine, times(1)).execute(any());
	}

	@Test
	@SuppressWarnings("deprecation")
	void testPlanWarnsWhenModified(LogRecordListener listener) {
		TestEngine engine = new TestEngineSpy();
		DefaultLauncher launcher = createLauncher(engine);
		TestPlan testPlan = launcher.discover(request().build());
		TestIdentifier engineIdentifier = getOnlyElement(testPlan.getRoots());
		UniqueId engineUniqueId = UniqueId.parse(engineIdentifier.getUniqueId());
		assertThat(testPlan.getChildren(engineIdentifier)).hasSize(1);

		TestIdentifier addedIdentifier = TestIdentifier.from(
			new TestDescriptorStub(engineUniqueId.append("test", "test2"), "test2"));
		testPlan.add(addedIdentifier);
		testPlan.add(addedIdentifier);

		assertThat(testPlan.getChildren(engineIdentifier)).hasSize(1).doesNotContain(addedIdentifier);
		assertThat(listener.stream(InternalTestPlan.class, Level.WARNING).map(LogRecord::getMessage).collect(
			toList())).containsExactly("Attempt to modify the TestPlan was detected. " //
					+ "A future version of the JUnit Platform will ignore this call and eventually even throw an exception. " //
					+ "Please contact your IDE/tool vendor and request a fix (see https://github.com/junit-team/junit5/issues/1732 for details).");
	}

	@Test
	void thirdPartyEngineUsingReservedEngineIdPrefixEmitsWarning(LogRecordListener listener) {
		String id = "junit-using-reserved-prefix";
		createLauncher(new TestEngineStub(id));
		assertThat(listener.stream(DefaultLauncher.class, Level.WARNING).map(LogRecord::getMessage)) //
				.containsExactly(
					"Third-party TestEngine implementations are forbidden to use the reserved 'junit-' prefix for their ID: '"
							+ id + "'");
	}

	@Test
	void thirdPartyEngineClaimingToBeJupiterResultsInException() {
		assertImposter("junit-jupiter");
	}

	@Test
	void thirdPartyEngineClaimingToBeVintageResultsInException() {
		assertImposter("junit-vintage");
	}

	private void assertImposter(String id) {
		TestEngine impostor = new TestEngineStub(id);
		Exception exception = assertThrows(JUnitException.class, () -> createLauncher(impostor));
		assertThat(exception).hasMessage(
			"Third-party TestEngine '%s' is forbidden to use the reserved '%s' TestEngine ID.",
			impostor.getClass().getName(), id);
	}

}
