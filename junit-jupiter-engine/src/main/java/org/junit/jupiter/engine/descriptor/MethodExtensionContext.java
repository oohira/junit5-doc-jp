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

import static org.apiguardian.api.API.Status.INTERNAL;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Optional;

import org.apiguardian.api.API;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.engine.execution.ThrowableCollector;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.EngineExecutionListener;

/**
 * @since 5.0
 */
@API(status = INTERNAL, since = "5.0")
public final class MethodExtensionContext extends AbstractExtensionContext<TestMethodTestDescriptor> {

	private final Object testInstance;

	private final ThrowableCollector throwableCollector;

	public MethodExtensionContext(ExtensionContext parent, EngineExecutionListener engineExecutionListener,
			TestMethodTestDescriptor testDescriptor, ConfigurationParameters configurationParameters,
			Object testInstance, ThrowableCollector throwableCollector) {

		super(parent, engineExecutionListener, testDescriptor, configurationParameters);

		this.testInstance = testInstance;
		this.throwableCollector = throwableCollector;
	}

	@Override
	public Optional<AnnotatedElement> getElement() {
		return Optional.of(getTestDescriptor().getTestMethod());
	}

	@Override
	public Optional<Class<?>> getTestClass() {
		return Optional.of(getTestDescriptor().getTestClass());
	}

	@Override
	public Optional<Lifecycle> getTestInstanceLifecycle() {
		return getParent().flatMap(ExtensionContext::getTestInstanceLifecycle);
	}

	@Override
	public Optional<Object> getTestInstance() {
		return Optional.of(this.testInstance);
	}

	@Override
	public Optional<Method> getTestMethod() {
		return Optional.of(getTestDescriptor().getTestMethod());
	}

	@Override
	public Optional<Throwable> getExecutionException() {
		return Optional.ofNullable(this.throwableCollector.getThrowable());
	}

}
