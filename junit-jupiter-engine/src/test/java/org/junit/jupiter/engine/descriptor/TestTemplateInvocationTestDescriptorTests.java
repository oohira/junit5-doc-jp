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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.platform.engine.UniqueId;

class TestTemplateInvocationTestDescriptorTests {

	@Test
	void invocationsDoNotDeclareExclusiveResources() throws Exception {
		Class<MyTestCase> testClass = MyTestCase.class;
		Method testTemplateMethod = testClass.getDeclaredMethod("testTemplate");
		JupiterConfiguration configuration = mock(JupiterConfiguration.class);
		TestTemplateTestDescriptor parent = new TestTemplateTestDescriptor(UniqueId.root("segment", "template"),
			testClass, testTemplateMethod, configuration);
		TestTemplateInvocationContext invocationContext = mock(TestTemplateInvocationContext.class);
		when(invocationContext.getDisplayName(anyInt())).thenReturn("invocation");

		TestTemplateInvocationTestDescriptor testDescriptor = new TestTemplateInvocationTestDescriptor(
			parent.getUniqueId().append(TestTemplateInvocationTestDescriptor.SEGMENT_TYPE, "1"), testClass,
			testTemplateMethod, invocationContext, 1, configuration);

		assertThat(parent.getExclusiveResources()).hasSize(1);
		assertThat(testDescriptor.getExclusiveResources()).isEmpty();
	}

	static class MyTestCase {
		@TestTemplate
		@ResourceLock("a")
		void testTemplate() {
		}
	}

}
