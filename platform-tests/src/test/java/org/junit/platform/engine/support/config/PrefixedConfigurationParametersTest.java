/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.engine.ConfigurationParameters;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PrefixedConfigurationParametersTests {

	@Mock
	private ConfigurationParameters delegate;

	@Test
	void delegatesGetCalls() {
		when(delegate.get(any())).thenReturn(Optional.of("result"));
		PrefixedConfigurationParameters parameters = new PrefixedConfigurationParameters(delegate, "foo.bar.");

		assertThat(parameters.get("qux")).contains("result");

		verify(delegate).get("foo.bar.qux");
	}

	@Test
	void delegatesGetBooleanCalls() {
		when(delegate.getBoolean(any())).thenReturn(Optional.of(true));
		PrefixedConfigurationParameters parameters = new PrefixedConfigurationParameters(delegate, "foo.bar.");

		assertThat(parameters.getBoolean("qux")).contains(true);

		verify(delegate).getBoolean("foo.bar.qux");
	}

	@Test
	void delegatesGetWithTransformerCalls() {
		when(delegate.get(any(), any())).thenReturn(Optional.of("QUX"));
		PrefixedConfigurationParameters parameters = new PrefixedConfigurationParameters(delegate, "foo.bar.");

		Function<String, String> transformer = String::toUpperCase;
		assertThat(parameters.get("qux", transformer)).contains("QUX");

		verify(delegate).get("foo.bar.qux", transformer);
	}

	@Test
	void delegatesSizeCalls() {
		when(delegate.size()).thenReturn(42);
		PrefixedConfigurationParameters parameters = new PrefixedConfigurationParameters(delegate, "foo.bar.");

		assertThat(parameters.size()).isEqualTo(42);

		verify(delegate).size();
	}
}
