/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params;

import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;
import static org.junit.platform.commons.util.AnnotationUtils.findRepeatableAnnotations;
import static org.junit.platform.commons.util.AnnotationUtils.isAnnotated;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.support.AnnotationConsumerInitializer;
import org.junit.platform.commons.util.ExceptionUtils;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ReflectionUtils;

/**
 * @since 5.0
 */
class ParameterizedTestExtension implements TestTemplateInvocationContextProvider {

	private static final String METHOD_CONTEXT_KEY = "context";

	@Override
	public boolean supportsTestTemplate(ExtensionContext context) {
		if (!context.getTestMethod().isPresent()) {
			return false;
		}

		Method testMethod = context.getTestMethod().get();
		if (!isAnnotated(testMethod, ParameterizedTest.class)) {
			return false;
		}

		ParameterizedTestMethodContext methodContext = new ParameterizedTestMethodContext(testMethod);

		Preconditions.condition(methodContext.hasPotentiallyValidSignature(),
			() -> String.format(
				"@ParameterizedTest method [%s] declares formal parameters in an invalid order: "
						+ "argument aggregators must be declared after any indexed arguments "
						+ "and before any arguments resolved by another ParameterResolver.",
				testMethod.toGenericString()));

		getStore(context).put(METHOD_CONTEXT_KEY, methodContext);

		return true;
	}

	@Override
	public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(
			ExtensionContext extensionContext) {

		Method templateMethod = extensionContext.getRequiredTestMethod();
		String displayName = extensionContext.getDisplayName();
		ParameterizedTestMethodContext methodContext = getStore(extensionContext)//
				.get(METHOD_CONTEXT_KEY, ParameterizedTestMethodContext.class);
		ParameterizedTestNameFormatter formatter = createNameFormatter(templateMethod, displayName);
		AtomicLong invocationCount = new AtomicLong(0);

		// @formatter:off
		return findRepeatableAnnotations(templateMethod, ArgumentsSource.class)
				.stream()
				.map(ArgumentsSource::value)
				.map(ReflectionUtils::newInstance)
				.map(provider -> AnnotationConsumerInitializer.initialize(templateMethod, provider))
				.flatMap(provider -> arguments(provider, extensionContext))
				.map(Arguments::get)
				.map(arguments -> consumedArguments(arguments, methodContext))
				.map(arguments -> createInvocationContext(formatter, methodContext, arguments))
				.peek(invocationContext -> invocationCount.incrementAndGet())
				.onClose(() ->
						Preconditions.condition(invocationCount.get() > 0,
								"Configuration error: You must configure at least one set of arguments for this @ParameterizedTest"));
		// @formatter:on
	}

	private ExtensionContext.Store getStore(ExtensionContext context) {
		return context.getStore(Namespace.create(ParameterizedTestExtension.class, context.getRequiredTestMethod()));
	}

	private TestTemplateInvocationContext createInvocationContext(ParameterizedTestNameFormatter formatter,
			ParameterizedTestMethodContext methodContext, Object[] arguments) {
		return new ParameterizedTestInvocationContext(formatter, methodContext, arguments);
	}

	private ParameterizedTestNameFormatter createNameFormatter(Method templateMethod, String displayName) {
		ParameterizedTest parameterizedTest = findAnnotation(templateMethod, ParameterizedTest.class).get();
		String pattern = Preconditions.notBlank(parameterizedTest.name().trim(),
			() -> String.format(
				"Configuration error: @ParameterizedTest on method [%s] must be declared with a non-empty name.",
				templateMethod));
		return new ParameterizedTestNameFormatter(pattern, displayName);
	}

	protected static Stream<? extends Arguments> arguments(ArgumentsProvider provider, ExtensionContext context) {
		try {
			return provider.provideArguments(context);
		}
		catch (Exception e) {
			throw ExceptionUtils.throwAsUncheckedException(e);
		}
	}

	private Object[] consumedArguments(Object[] arguments, ParameterizedTestMethodContext methodContext) {
		int parameterCount = methodContext.getParameterCount();
		return methodContext.hasAggregator() ? arguments
				: (arguments.length > parameterCount ? Arrays.copyOf(arguments, parameterCount) : arguments);
	}

}
