/*
 * Copyright 2015-2019 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.engine.descriptor.CustomDisplayNameGenerator;

/**
 * Unit tests for {@link CachingJupiterConfiguration}.
 */
class CachingJupiterConfigurationTests {

	private final JupiterConfiguration delegate = mock(JupiterConfiguration.class);
	private final JupiterConfiguration cache = new CachingJupiterConfiguration(delegate);

	@Test
	void cachesDefaultExecutionMode() {
		when(delegate.getDefaultExecutionMode()).thenReturn(ExecutionMode.CONCURRENT);

		assertThat(cache.getDefaultExecutionMode()).isEqualTo(ExecutionMode.CONCURRENT);
		assertThat(cache.getDefaultExecutionMode()).isEqualTo(ExecutionMode.CONCURRENT);

		verify(delegate, only()).getDefaultExecutionMode();
	}

	@Test
	void cachesDefaultTestInstanceLifecycle() {
		when(delegate.getDefaultTestInstanceLifecycle()).thenReturn(Lifecycle.PER_CLASS);

		assertThat(cache.getDefaultTestInstanceLifecycle()).isEqualTo(Lifecycle.PER_CLASS);
		assertThat(cache.getDefaultTestInstanceLifecycle()).isEqualTo(Lifecycle.PER_CLASS);

		verify(delegate, only()).getDefaultTestInstanceLifecycle();
	}

	@Test
	void cachesExecutionConditionFilter() {
		Predicate<ExecutionCondition> predicate = executionCondition -> true;
		when(delegate.getExecutionConditionFilter()).thenReturn(predicate);

		assertThat(cache.getExecutionConditionFilter()).isSameAs(predicate);
		assertThat(cache.getExecutionConditionFilter()).isSameAs(predicate);

		verify(delegate, only()).getExecutionConditionFilter();
	}

	@Test
	void cachesExtensionAutoDetectionEnabled() {
		when(delegate.isExtensionAutoDetectionEnabled()).thenReturn(true);

		assertThat(cache.isExtensionAutoDetectionEnabled()).isTrue();
		assertThat(cache.isExtensionAutoDetectionEnabled()).isTrue();

		verify(delegate, only()).isExtensionAutoDetectionEnabled();
	}

	@Test
	void cachesParallelExecutionEnabled() {
		when(delegate.isParallelExecutionEnabled()).thenReturn(true);

		assertThat(cache.isParallelExecutionEnabled()).isTrue();
		assertThat(cache.isParallelExecutionEnabled()).isTrue();

		verify(delegate, only()).isParallelExecutionEnabled();
	}

	@Test
	void cachesDefaultDisplayNameGenerator() {
		CustomDisplayNameGenerator customDisplayNameGenerator = new CustomDisplayNameGenerator();
		when(delegate.getDefaultDisplayNameGenerator()).thenReturn(customDisplayNameGenerator);

		// call `cache.getDefaultDisplayNameGenerator()` twice to verify the delegate method is called only once.
		assertThat(cache.getDefaultDisplayNameGenerator()).isSameAs(customDisplayNameGenerator);
		assertThat(cache.getDefaultDisplayNameGenerator()).isSameAs(customDisplayNameGenerator);

		verify(delegate, only()).getDefaultDisplayNameGenerator();
	}

	@Test
	void doesNotCacheRawParameters() {
		when(delegate.getRawConfigurationParameter("foo")).thenReturn(Optional.of("bar")).thenReturn(
			Optional.of("baz"));

		assertThat(cache.getRawConfigurationParameter("foo")).contains("bar");
		assertThat(cache.getRawConfigurationParameter("foo")).contains("baz");

		verify(delegate, times(2)).getRawConfigurationParameter("foo");
		verifyNoMoreInteractions(delegate);
	}

}
