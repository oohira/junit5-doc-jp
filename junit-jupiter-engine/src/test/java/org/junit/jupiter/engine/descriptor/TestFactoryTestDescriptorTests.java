/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.descriptor;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.engine.execution.JupiterEngineExecutionContext;
import org.junit.jupiter.engine.execution.ThrowableCollector;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.hierarchical.Node;

/**
 * Unit tests for {@link TestFactoryTestDescriptor}.
 *
 * @since 5.0
 */
class TestFactoryTestDescriptorTests {

	private JupiterEngineExecutionContext context;
	private ExtensionContext extensionContext;
	private TestFactoryTestDescriptor descriptor;
	private boolean isClosed;

	@BeforeEach
	void before() throws Exception {
		extensionContext = mock(ExtensionContext.class);
		isClosed = false;

		context = new JupiterEngineExecutionContext(null, null).extend().withThrowableCollector(
			new ThrowableCollector()).withExtensionContext(extensionContext).build();

		Method testMethod = CustomStreamTestCase.class.getDeclaredMethod("customStream");
		descriptor = new TestFactoryTestDescriptor(UniqueId.forEngine("engine"), CustomStreamTestCase.class,
			testMethod);
		when(extensionContext.getTestMethod()).thenReturn(Optional.of(testMethod));
	}

	@Test
	void streamsFromTestFactoriesShouldBeClosed() {
		Stream<DynamicTest> dynamicTestStream = Stream.empty();
		prepareMockForTestInstanceWithCustomStream(dynamicTestStream);

		descriptor.invokeTestMethod(context, mock(Node.DynamicTestExecutor.class));

		assertTrue(isClosed);
	}

	@Test
	void streamsFromTestFactoriesShouldBeClosedWhenTheyThrow() {
		Stream<Integer> integerStream = Stream.of(1, 2);
		prepareMockForTestInstanceWithCustomStream(integerStream);

		descriptor.invokeTestMethod(context, mock(Node.DynamicTestExecutor.class));

		assertTrue(isClosed);
	}

	private void prepareMockForTestInstanceWithCustomStream(Stream<?> stream) {
		Stream<?> mockStream = stream.onClose(() -> isClosed = true);
		when(extensionContext.getRequiredTestInstance()).thenReturn(new CustomStreamTestCase(mockStream));
	}

	private static class CustomStreamTestCase {

		private final Stream<?> mockStream;

		CustomStreamTestCase(Stream<?> mockStream) {
			this.mockStream = mockStream;
		}

		@TestFactory
		Stream<?> customStream() {
			return mockStream;
		}
	}

}
