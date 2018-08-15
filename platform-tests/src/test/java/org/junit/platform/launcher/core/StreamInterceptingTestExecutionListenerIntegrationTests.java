/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.junit.platform.engine.TestExecutionResult.successful;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;
import static org.junit.platform.launcher.LauncherConstants.CAPTURE_STDERR_PROPERTY_NAME;
import static org.junit.platform.launcher.LauncherConstants.CAPTURE_STDOUT_PROPERTY_NAME;
import static org.junit.platform.launcher.LauncherConstants.STDERR_REPORT_ENTRY_KEY;
import static org.junit.platform.launcher.LauncherConstants.STDOUT_REPORT_ENTRY_KEY;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;
import static org.junit.platform.launcher.core.LauncherFactoryForTestingPurposesOnly.createLauncher;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.engine.support.hierarchical.DemoHierarchicalTestEngine;
import org.junit.platform.launcher.LauncherConstants;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

/**
 * @since 1.3
 */
class StreamInterceptingTestExecutionListenerIntegrationTests {

	@ParameterizedTest(name = "{0}")
	@MethodSource("systemStreams")
	@ExtendWith(HiddenSystemOutAndErr.class)
	void interceptsStream(String configParam, Supplier<PrintStream> printStreamSupplier, String reportKey) {
		DemoHierarchicalTestEngine engine = new DemoHierarchicalTestEngine("engine");
		TestDescriptor test = engine.addTest("test", () -> printStreamSupplier.get().print("4567890"));
		TestExecutionListener listener = mock(TestExecutionListener.class);
		doAnswer(invocation -> {
			TestIdentifier testIdentifier = invocation.getArgument(0);
			if (testIdentifier.getUniqueId().equals(test.getUniqueId().toString())) {
				printStreamSupplier.get().print("123");
			}
			return null;
		}).when(listener).executionStarted(any());

		DefaultLauncher launcher = createLauncher(engine);
		LauncherDiscoveryRequest discoveryRequest = request()//
				.selectors(selectUniqueId(test.getUniqueId()))//
				.configurationParameter(configParam, String.valueOf(true))//
				.configurationParameter(LauncherConstants.CAPTURE_MAX_BUFFER_PROPERTY_NAME, String.valueOf(5))//
				.build();
		launcher.execute(discoveryRequest, listener);

		ArgumentCaptor<TestPlan> testPlanArgumentCaptor = ArgumentCaptor.forClass(TestPlan.class);
		InOrder inOrder = inOrder(listener);
		inOrder.verify(listener).testPlanExecutionStarted(testPlanArgumentCaptor.capture());
		TestPlan testPlan = testPlanArgumentCaptor.getValue();
		TestIdentifier testIdentifier = testPlan.getTestIdentifier(test.getUniqueId().toString());

		ArgumentCaptor<ReportEntry> reportEntryArgumentCaptor = ArgumentCaptor.forClass(ReportEntry.class);
		inOrder.verify(listener).reportingEntryPublished(same(testIdentifier), reportEntryArgumentCaptor.capture());
		inOrder.verify(listener).executionFinished(testIdentifier, successful());
		ReportEntry reportEntry = reportEntryArgumentCaptor.getValue();

		assertThat(reportEntry.getKeyValuePairs()).containsExactly(entry(reportKey, "12345"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("systemStreams")
	@ExtendWith(HiddenSystemOutAndErr.class)
	void doesNotInterceptStreamWhenAlreadyBeingIntercepted(String configParam,
			Supplier<PrintStream> printStreamSupplier) {
		DemoHierarchicalTestEngine engine = new DemoHierarchicalTestEngine("engine");
		TestDescriptor test = engine.addTest("test", () -> printStreamSupplier.get().print("1234567890"));

		assertThat(StreamInterceptor.registerStdout(1)).isPresent();
		assertThat(StreamInterceptor.registerStderr(1)).isPresent();

		DefaultLauncher launcher = createLauncher(engine);
		LauncherDiscoveryRequest discoveryRequest = request()//
				.selectors(selectUniqueId(test.getUniqueId()))//
				.configurationParameter(configParam, String.valueOf(true))//
				.build();
		TestExecutionListener listener = mock(TestExecutionListener.class);
		launcher.execute(discoveryRequest, listener);

		verify(listener, never()).reportingEntryPublished(any(), any());
	}

	@SuppressWarnings("unused") // used via @MethodSource("systemStreams")
	private static Stream<Arguments> systemStreams() {
		return Stream.of(//
			streamType(CAPTURE_STDOUT_PROPERTY_NAME, () -> System.out, STDOUT_REPORT_ENTRY_KEY), //
			streamType(CAPTURE_STDERR_PROPERTY_NAME, () -> System.err, STDERR_REPORT_ENTRY_KEY));
	}

	private static Arguments streamType(String configParam, Supplier<PrintStream> printStreamSupplier,
			String reportKey) {
		return arguments(configParam, printStreamSupplier, reportKey);
	}

	static class HiddenSystemOutAndErr implements BeforeTestExecutionCallback, AfterTestExecutionCallback {

		private static final Namespace NAMESPACE = Namespace.create(HiddenSystemOutAndErr.class);

		@Override
		public void beforeTestExecution(ExtensionContext context) {
			ExtensionContext.Store store = context.getStore(NAMESPACE);
			store.put("out", System.out);
			store.put("err", System.err);
			System.setOut(new PrintStream(new ByteArrayOutputStream()));
			System.setErr(new PrintStream(new ByteArrayOutputStream()));
		}

		@Override
		public void afterTestExecution(ExtensionContext context) {
			ExtensionContext.Store store = context.getStore(NAMESPACE);
			System.setOut(store.get("out", PrintStream.class));
			System.setErr(store.get("err", PrintStream.class));
		}
	}

}
