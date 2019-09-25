/*
 * Copyright 2015-2019 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.descriptor;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;

/**
 * Unit tests for {@link TestTemplateTestDescriptor}.
 *
 * @since 5.0
 */
class TestTemplateTestDescriptorTests {
	private JupiterConfiguration jupiterConfiguration = mock(JupiterConfiguration.class);

	@Test
	void inheritsTagsFromParent() throws Exception {
		UniqueId rootUniqueId = UniqueId.root("segment", "template");
		UniqueId parentUniqueId = rootUniqueId.append("class", "myClass");
		AbstractTestDescriptor parent = containerTestDescriptorWithTags(parentUniqueId,
			singleton(TestTag.create("foo")));

		when(jupiterConfiguration.getDefaultDisplayNameGenerator()).thenReturn(new DisplayNameGenerator.Standard());

		TestTemplateTestDescriptor testDescriptor = new TestTemplateTestDescriptor(
			parentUniqueId.append("tmp", "testTemplate()"), MyTestCase.class,
			MyTestCase.class.getDeclaredMethod("testTemplate"), jupiterConfiguration);
		parent.addChild(testDescriptor);

		assertThat(testDescriptor.getTags()).containsExactlyInAnyOrder(TestTag.create("foo"), TestTag.create("bar"),
			TestTag.create("baz"));
	}

	@Test
	void shouldUseCustomDisplayNameGeneratorIfPresentFromConfiguration() throws Exception {
		UniqueId rootUniqueId = UniqueId.root("segment", "template");
		UniqueId parentUniqueId = rootUniqueId.append("class", "myClass");
		AbstractTestDescriptor parent = containerTestDescriptorWithTags(parentUniqueId,
			singleton(TestTag.create("foo")));

		when(jupiterConfiguration.getDefaultDisplayNameGenerator()).thenReturn(new CustomDisplayNameGenerator());

		TestTemplateTestDescriptor testDescriptor = new TestTemplateTestDescriptor(
			parentUniqueId.append("tmp", "testTemplate()"), MyTestCase.class,
			MyTestCase.class.getDeclaredMethod("testTemplate"), jupiterConfiguration);
		parent.addChild(testDescriptor);

		assertThat(testDescriptor.getDisplayName()).isEqualTo("method-display-name");
	}

	@Test
	void shouldUseStandardDisplayNameGeneratorIfConfigurationNotPresent() throws Exception {
		UniqueId rootUniqueId = UniqueId.root("segment", "template");
		UniqueId parentUniqueId = rootUniqueId.append("class", "myClass");
		AbstractTestDescriptor parent = containerTestDescriptorWithTags(parentUniqueId,
			singleton(TestTag.create("foo")));

		when(jupiterConfiguration.getDefaultDisplayNameGenerator()).thenReturn(new DisplayNameGenerator.Standard());

		TestTemplateTestDescriptor testDescriptor = new TestTemplateTestDescriptor(
			parentUniqueId.append("tmp", "testTemplate()"), MyTestCase.class,
			MyTestCase.class.getDeclaredMethod("testTemplate"), jupiterConfiguration);
		parent.addChild(testDescriptor);

		assertThat(testDescriptor.getDisplayName()).isEqualTo("testTemplate()");
	}

	private AbstractTestDescriptor containerTestDescriptorWithTags(UniqueId uniqueId, Set<TestTag> tags) {
		return new AbstractTestDescriptor(uniqueId, "testDescriptor with tags") {

			@Override
			public Type getType() {
				return Type.CONTAINER;
			}

			@Override
			public Set<TestTag> getTags() {
				return tags;
			}
		};
	}

	static class MyTestCase {

		@Tag("bar")
		@Tag("baz")
		@TestTemplate
		void testTemplate() {
		}
	}

}
