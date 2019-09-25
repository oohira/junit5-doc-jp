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

import static org.junit.jupiter.engine.config.JupiterConfiguration.DEFAULT_AFTER_ALL_METHOD_TIMEOUT_PROPERTY_NAME;
import static org.junit.jupiter.engine.config.JupiterConfiguration.DEFAULT_AFTER_EACH_METHOD_TIMEOUT_PROPERTY_NAME;
import static org.junit.jupiter.engine.config.JupiterConfiguration.DEFAULT_BEFORE_ALL_METHOD_TIMEOUT_PROPERTY_NAME;
import static org.junit.jupiter.engine.config.JupiterConfiguration.DEFAULT_BEFORE_EACH_METHOD_TIMEOUT_PROPERTY_NAME;
import static org.junit.jupiter.engine.config.JupiterConfiguration.DEFAULT_LIFECYCLE_METHOD_TIMEOUT_PROPERTY_NAME;
import static org.junit.jupiter.engine.config.JupiterConfiguration.DEFAULT_TESTABLE_METHOD_TIMEOUT_PROPERTY_NAME;
import static org.junit.jupiter.engine.config.JupiterConfiguration.DEFAULT_TEST_FACTORY_METHOD_TIMEOUT_PROPERTY_NAME;
import static org.junit.jupiter.engine.config.JupiterConfiguration.DEFAULT_TEST_METHOD_TIMEOUT_PROPERTY_NAME;
import static org.junit.jupiter.engine.config.JupiterConfiguration.DEFAULT_TEST_TEMPLATE_METHOD_TIMEOUT_PROPERTY_NAME;
import static org.junit.jupiter.engine.config.JupiterConfiguration.DEFAULT_TIMEOUT_PROPERTY_NAME;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;

/**
 * @since 5.5
 */
class TimeoutConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(TimeoutConfiguration.class);

	private final TimeoutDurationParser parser = new TimeoutDurationParser();
	private final Map<String, Optional<TimeoutDuration>> cache = new ConcurrentHashMap<>();
	private final ExtensionContext extensionContext;

	TimeoutConfiguration(ExtensionContext extensionContext) {
		this.extensionContext = extensionContext;
	}

	Optional<TimeoutDuration> getDefaultTestMethodTimeout() {
		return parseOrDefault(DEFAULT_TEST_METHOD_TIMEOUT_PROPERTY_NAME, this::getDefaultTestableMethodTimeout);
	}

	Optional<TimeoutDuration> getDefaultTestTemplateMethodTimeout() {
		return parseOrDefault(DEFAULT_TEST_TEMPLATE_METHOD_TIMEOUT_PROPERTY_NAME,
			this::getDefaultTestableMethodTimeout);
	}

	Optional<TimeoutDuration> getDefaultTestFactoryMethodTimeout() {
		return parseOrDefault(DEFAULT_TEST_FACTORY_METHOD_TIMEOUT_PROPERTY_NAME, this::getDefaultTestableMethodTimeout);
	}

	Optional<TimeoutDuration> getDefaultBeforeAllMethodTimeout() {
		return parseOrDefault(DEFAULT_BEFORE_ALL_METHOD_TIMEOUT_PROPERTY_NAME, this::getDefaultLifecycleMethodTimeout);
	}

	Optional<TimeoutDuration> getDefaultBeforeEachMethodTimeout() {
		return parseOrDefault(DEFAULT_BEFORE_EACH_METHOD_TIMEOUT_PROPERTY_NAME, this::getDefaultLifecycleMethodTimeout);
	}

	Optional<TimeoutDuration> getDefaultAfterEachMethodTimeout() {
		return parseOrDefault(DEFAULT_AFTER_EACH_METHOD_TIMEOUT_PROPERTY_NAME, this::getDefaultLifecycleMethodTimeout);
	}

	Optional<TimeoutDuration> getDefaultAfterAllMethodTimeout() {
		return parseOrDefault(DEFAULT_AFTER_ALL_METHOD_TIMEOUT_PROPERTY_NAME, this::getDefaultLifecycleMethodTimeout);
	}

	private Optional<TimeoutDuration> getDefaultTestableMethodTimeout() {
		return parseOrDefault(DEFAULT_TESTABLE_METHOD_TIMEOUT_PROPERTY_NAME, this::getDefaultTimeout);
	}

	private Optional<TimeoutDuration> getDefaultLifecycleMethodTimeout() {
		return parseOrDefault(DEFAULT_LIFECYCLE_METHOD_TIMEOUT_PROPERTY_NAME, this::getDefaultTimeout);
	}

	private Optional<TimeoutDuration> getDefaultTimeout() {
		return parseTimeoutDuration(DEFAULT_TIMEOUT_PROPERTY_NAME);
	}

	private Optional<TimeoutDuration> parseOrDefault(String propertyName,
			Supplier<Optional<TimeoutDuration>> defaultSupplier) {
		Optional<TimeoutDuration> timeoutConfiguration = parseTimeoutDuration(propertyName);
		return timeoutConfiguration.isPresent() ? timeoutConfiguration : defaultSupplier.get();
	}

	private Optional<TimeoutDuration> parseTimeoutDuration(String propertyName) {
		return cache.computeIfAbsent(propertyName, key -> extensionContext.getConfigurationParameter(key).map(value -> {
			try {
				return parser.parse(value);
			}
			catch (Exception e) {
				logger.warn(e,
					() -> String.format("Ignored invalid timeout '%s' set via the '%s' configuration parameter.", value,
						key));
				return null;
			}
		}));
	}

}
