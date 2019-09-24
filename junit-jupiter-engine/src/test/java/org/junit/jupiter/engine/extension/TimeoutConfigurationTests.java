/*
 * Copyright 2015-2019 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.extension;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.engine.Constants.DEFAULT_AFTER_ALL_METHOD_TIMEOUT_PROPERTY_NAME;
import static org.junit.jupiter.engine.Constants.DEFAULT_AFTER_EACH_METHOD_TIMEOUT_PROPERTY_NAME;
import static org.junit.jupiter.engine.Constants.DEFAULT_BEFORE_ALL_METHOD_TIMEOUT_PROPERTY_NAME;
import static org.junit.jupiter.engine.Constants.DEFAULT_BEFORE_EACH_METHOD_TIMEOUT_PROPERTY_NAME;
import static org.junit.jupiter.engine.Constants.DEFAULT_LIFECYCLE_METHOD_TIMEOUT_PROPERTY_NAME;
import static org.junit.jupiter.engine.Constants.DEFAULT_TESTABLE_METHOD_TIMEOUT_PROPERTY_NAME;
import static org.junit.jupiter.engine.Constants.DEFAULT_TEST_FACTORY_METHOD_TIMEOUT_PROPERTY_NAME;
import static org.junit.jupiter.engine.Constants.DEFAULT_TEST_METHOD_TIMEOUT_PROPERTY_NAME;
import static org.junit.jupiter.engine.Constants.DEFAULT_TEST_TEMPLATE_METHOD_TIMEOUT_PROPERTY_NAME;
import static org.junit.jupiter.engine.Constants.DEFAULT_TIMEOUT_PROPERTY_NAME;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.engine.TrackLogRecords;
import org.junit.platform.commons.logging.LogRecordListener;

/**
 * @since 5.5
 */
class TimeoutConfigurationTests {

	ExtensionContext extensionContext = mock(ExtensionContext.class);
	TimeoutConfiguration config = new TimeoutConfiguration(extensionContext);

	@Test
	void noTimeoutIfNoPropertiesAreSet() {
		assertThat(config.getDefaultBeforeAllMethodTimeout()).isEmpty();
		assertThat(config.getDefaultBeforeEachMethodTimeout()).isEmpty();
		assertThat(config.getDefaultTestMethodTimeout()).isEmpty();
		assertThat(config.getDefaultTestTemplateMethodTimeout()).isEmpty();
		assertThat(config.getDefaultTestFactoryMethodTimeout()).isEmpty();
		assertThat(config.getDefaultAfterEachMethodTimeout()).isEmpty();
		assertThat(config.getDefaultAfterAllMethodTimeout()).isEmpty();
	}

	@Test
	void defaultTimeoutIsUsedUnlessAMoreSpecificOneIsSet() {
		when(extensionContext.getConfigurationParameter(DEFAULT_TIMEOUT_PROPERTY_NAME)).thenReturn(Optional.of("42"));

		assertThat(config.getDefaultBeforeAllMethodTimeout()).contains(new TimeoutDuration(42, SECONDS));
		assertThat(config.getDefaultBeforeEachMethodTimeout()).contains(new TimeoutDuration(42, SECONDS));
		assertThat(config.getDefaultTestMethodTimeout()).contains(new TimeoutDuration(42, SECONDS));
		assertThat(config.getDefaultTestTemplateMethodTimeout()).contains(new TimeoutDuration(42, SECONDS));
		assertThat(config.getDefaultTestFactoryMethodTimeout()).contains(new TimeoutDuration(42, SECONDS));
		assertThat(config.getDefaultAfterEachMethodTimeout()).contains(new TimeoutDuration(42, SECONDS));
		assertThat(config.getDefaultAfterAllMethodTimeout()).contains(new TimeoutDuration(42, SECONDS));
	}

	@Test
	void defaultCategoryTimeoutIsUsedUnlessAMoreSpecificOneIsSet() {
		when(extensionContext.getConfigurationParameter(DEFAULT_TIMEOUT_PROPERTY_NAME)).thenReturn(Optional.of("2"));
		when(extensionContext.getConfigurationParameter(DEFAULT_LIFECYCLE_METHOD_TIMEOUT_PROPERTY_NAME)).thenReturn(
			Optional.of("3"));
		when(extensionContext.getConfigurationParameter(DEFAULT_TESTABLE_METHOD_TIMEOUT_PROPERTY_NAME)).thenReturn(
			Optional.of("5"));

		assertThat(config.getDefaultBeforeAllMethodTimeout()).contains(new TimeoutDuration(3, SECONDS));
		assertThat(config.getDefaultBeforeEachMethodTimeout()).contains(new TimeoutDuration(3, SECONDS));
		assertThat(config.getDefaultTestMethodTimeout()).contains(new TimeoutDuration(5, SECONDS));
		assertThat(config.getDefaultTestTemplateMethodTimeout()).contains(new TimeoutDuration(5, SECONDS));
		assertThat(config.getDefaultTestFactoryMethodTimeout()).contains(new TimeoutDuration(5, SECONDS));
		assertThat(config.getDefaultAfterEachMethodTimeout()).contains(new TimeoutDuration(3, SECONDS));
		assertThat(config.getDefaultAfterAllMethodTimeout()).contains(new TimeoutDuration(3, SECONDS));
	}

	@Test
	void specificTimeoutsAreUsedIfSet() {
		when(extensionContext.getConfigurationParameter(DEFAULT_TIMEOUT_PROPERTY_NAME)).thenReturn(Optional.of("2"));
		when(extensionContext.getConfigurationParameter(DEFAULT_LIFECYCLE_METHOD_TIMEOUT_PROPERTY_NAME)).thenReturn(
			Optional.of("3"));
		when(extensionContext.getConfigurationParameter(DEFAULT_TESTABLE_METHOD_TIMEOUT_PROPERTY_NAME)).thenReturn(
			Optional.of("5"));
		when(extensionContext.getConfigurationParameter(DEFAULT_BEFORE_ALL_METHOD_TIMEOUT_PROPERTY_NAME)).thenReturn(
			Optional.of("7ns"));
		when(extensionContext.getConfigurationParameter(DEFAULT_BEFORE_EACH_METHOD_TIMEOUT_PROPERTY_NAME)).thenReturn(
			Optional.of("11μs"));
		when(extensionContext.getConfigurationParameter(DEFAULT_TEST_METHOD_TIMEOUT_PROPERTY_NAME)).thenReturn(
			Optional.of("13ms"));
		when(extensionContext.getConfigurationParameter(DEFAULT_TEST_TEMPLATE_METHOD_TIMEOUT_PROPERTY_NAME)).thenReturn(
			Optional.of("17s"));
		when(extensionContext.getConfigurationParameter(DEFAULT_TEST_FACTORY_METHOD_TIMEOUT_PROPERTY_NAME)).thenReturn(
			Optional.of("19m"));
		when(extensionContext.getConfigurationParameter(DEFAULT_AFTER_EACH_METHOD_TIMEOUT_PROPERTY_NAME)).thenReturn(
			Optional.of("23h"));
		when(extensionContext.getConfigurationParameter(DEFAULT_AFTER_ALL_METHOD_TIMEOUT_PROPERTY_NAME)).thenReturn(
			Optional.of("29d"));

		assertThat(config.getDefaultBeforeAllMethodTimeout()).contains(new TimeoutDuration(7, NANOSECONDS));
		assertThat(config.getDefaultBeforeEachMethodTimeout()).contains(new TimeoutDuration(11, MICROSECONDS));
		assertThat(config.getDefaultTestMethodTimeout()).contains(new TimeoutDuration(13, MILLISECONDS));
		assertThat(config.getDefaultTestTemplateMethodTimeout()).contains(new TimeoutDuration(17, SECONDS));
		assertThat(config.getDefaultTestFactoryMethodTimeout()).contains(new TimeoutDuration(19, MINUTES));
		assertThat(config.getDefaultAfterEachMethodTimeout()).contains(new TimeoutDuration(23, HOURS));
		assertThat(config.getDefaultAfterAllMethodTimeout()).contains(new TimeoutDuration(29, DAYS));
	}

	@Test
	@TrackLogRecords
	void logsInvalidValues(LogRecordListener logRecordListener) {
		when(extensionContext.getConfigurationParameter(DEFAULT_TEST_METHOD_TIMEOUT_PROPERTY_NAME)).thenReturn(
			Optional.of("invalid"));

		assertThat(config.getDefaultTestMethodTimeout()).isEmpty();
		assertThat(logRecordListener.stream(Level.WARNING).map(LogRecord::getMessage)) //
				.containsExactly(
					"Ignored invalid timeout 'invalid' set via the 'junit.jupiter.execution.timeout.test.method.default' configuration parameter.");
	}
}
