/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.function.Function;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.engine.extension.ExtensionRegistry;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.engine.ConfigurationParameters;

/**
 * Unit tests for {@link ExecutableInvoker}.
 *
 * @since 5.0
 */
class ExecutableInvokerTests {

	private static final String ENIGMA = "enigma";

	private final MethodSource instance = mock(MethodSource.class);
	private Method method;

	private final ExtensionContext extensionContext = mock(ExtensionContext.class);

	private final ConfigurationParameters configParams = mock(ConfigurationParameters.class);

	private ExtensionRegistry extensionRegistry = ExtensionRegistry.createRegistryWithDefaultExtensions(configParams);

	@Test
	void constructorInjection() {
		register(new StringParameterResolver(), new NumberParameterResolver());

		Class<ConstructorInjectionTestCase> outerClass = ConstructorInjectionTestCase.class;
		Constructor<ConstructorInjectionTestCase> constructor = ReflectionUtils.getDeclaredConstructor(outerClass);
		ConstructorInjectionTestCase outer = newInvoker().invoke(constructor, extensionContext, extensionRegistry);

		assertNotNull(outer);
		assertEquals(ENIGMA, outer.str);

		Class<ConstructorInjectionTestCase.NestedTestCase> innerClass = ConstructorInjectionTestCase.NestedTestCase.class;
		Constructor<ConstructorInjectionTestCase.NestedTestCase> innerConstructor = ReflectionUtils.getDeclaredConstructor(
			innerClass);
		ConstructorInjectionTestCase.NestedTestCase inner = newInvoker().invoke(innerConstructor, outer,
			extensionContext, extensionRegistry);

		assertNotNull(inner);
		assertEquals(42, inner.num);
	}

	@Test
	void constructorInjectionWithMissingResolver() {
		Constructor<ConstructorInjectionTestCase> constructor = ReflectionUtils.getDeclaredConstructor(
			ConstructorInjectionTestCase.class);

		Exception exception = assertThrows(ParameterResolutionException.class,
			() -> newInvoker().invoke(constructor, extensionContext, extensionRegistry));

		assertThat(exception.getMessage())//
				.contains("No ParameterResolver registered for parameter [java.lang.String")//
				.contains("in constructor")//
				.contains(ConstructorInjectionTestCase.class.getName());
	}

	@Test
	void invokingMethodsWithoutParameterDoesNotDependOnExtensions() throws Exception {
		testMethodWithNoParameters();
		extensionRegistry = null;

		invokeMethod();

		verify(instance).noParameter();
	}

	@Test
	void resolveArgumentsViaParameterResolver() {
		testMethodWithASingleStringParameter();
		thereIsAParameterResolverThatResolvesTheParameterTo("argument");

		invokeMethod();

		verify(instance).singleStringParameter("argument");
	}

	@Test
	void resolveMultipleArguments() {
		testMethodWith("multipleParameters", String.class, Integer.class, Double.class);
		register(ConfigurableParameterResolver.supportsAndResolvesTo(parameterContext -> {
			switch (parameterContext.getIndex()) {
				case 0:
					return "0";
				case 1:
					return 1;
				default:
					return 2.0;
			}
		}));

		invokeMethod();

		verify(instance).multipleParameters("0", 1, 2.0);
	}

	@Test
	void onlyConsiderParameterResolversThatSupportAParticularParameter() {
		testMethodWithASingleStringParameter();
		thereIsAParameterResolverThatDoesNotSupportThisParameter();
		thereIsAParameterResolverThatResolvesTheParameterTo("something");

		invokeMethod();

		verify(instance).singleStringParameter("something");
	}

	@Test
	void passContextInformationToParameterResolverMethods() {
		anyTestMethodWithAtLeastOneParameter();
		ArgumentRecordingParameterResolver extension = new ArgumentRecordingParameterResolver();
		register(extension);

		invokeMethod();

		assertSame(extensionContext, extension.supportsArguments.extensionContext);
		assertEquals(0, extension.supportsArguments.parameterContext.getIndex());
		assertSame(instance, extension.supportsArguments.parameterContext.getTarget().get());
		assertSame(extensionContext, extension.resolveArguments.extensionContext);
		assertEquals(0, extension.resolveArguments.parameterContext.getIndex());
		assertSame(instance, extension.resolveArguments.parameterContext.getTarget().get());
		assertThat(extension.resolveArguments.parameterContext.toString())//
				.contains("parameter", String.class.getTypeName(), "index", "0", "target", "Mock");
	}

	@Test
	void invocationOfMethodsWithPrimitiveTypesIsSupported() {
		testMethodWithASinglePrimitiveIntParameter();
		thereIsAParameterResolverThatResolvesTheParameterTo(42);

		invokeMethod();

		verify(instance).primitiveParameterInt(42);
	}

	@Test
	void nullIsAViableArgumentIfAReferenceTypeParameterIsExpected() {
		testMethodWithASingleStringParameter();
		thereIsAParameterResolverThatResolvesTheParameterTo(null);

		invokeMethod();

		verify(instance).singleStringParameter(null);
	}

	@Test
	void reportThatNullIsNotAViableArgumentIfAPrimitiveTypeIsExpected() {
		testMethodWithASinglePrimitiveIntParameter();
		thereIsAParameterResolverThatResolvesTheParameterTo(null);

		ParameterResolutionException caught = assertThrows(ParameterResolutionException.class, this::invokeMethod);

		// @formatter:off
		assertThat(caught.getMessage())
				.contains("in method")
				.contains("resolved a null value for parameter [int")
				.contains("but a primitive of type [int] is required.");
		// @formatter:on
	}

	@Test
	void reportIfThereIsNoParameterResolverThatSupportsTheParameter() {
		testMethodWithASingleStringParameter();

		ParameterResolutionException caught = assertThrows(ParameterResolutionException.class, this::invokeMethod);

		assertThat(caught.getMessage()).contains("parameter [java.lang.String").contains("in method");
	}

	@Test
	void reportIfThereAreMultipleParameterResolversThatSupportTheParameter() {
		testMethodWithASingleStringParameter();
		thereIsAParameterResolverThatResolvesTheParameterTo("one");
		thereIsAParameterResolverThatResolvesTheParameterTo("two");

		ParameterResolutionException caught = assertThrows(ParameterResolutionException.class, this::invokeMethod);

		// @formatter:off
		assertThat(caught.getMessage())
				.contains("parameter [java.lang.String")
				.contains("in method")
				.contains(ConfigurableParameterResolver.class.getName() + ", " + ConfigurableParameterResolver.class.getName());
		// @formatter:on
	}

	@Test
	void reportTypeMismatchBetweenParameterAndResolvedParameter() {
		testMethodWithASingleStringParameter();
		thereIsAParameterResolverThatResolvesTheParameterTo(BigDecimal.ONE);

		ParameterResolutionException caught = assertThrows(ParameterResolutionException.class, this::invokeMethod);

		// @formatter:off
		assertThat(caught.getMessage())
				.contains("resolved a value of type [java.math.BigDecimal] for parameter [java.lang.String")
				.contains("in method")
				.contains("but a value assignment compatible with [java.lang.String] is required.");
		// @formatter:on
	}

	@Test
	void wrapAllExceptionsThrownDuringParameterResolutionIntoAParameterResolutionException() {
		anyTestMethodWithAtLeastOneParameter();
		IllegalArgumentException cause = anyExceptionButParameterResolutionException();
		throwDuringParameterResolution(cause);

		ParameterResolutionException caught = assertThrows(ParameterResolutionException.class, this::invokeMethod);

		assertSame(cause, caught.getCause(), () -> "cause should be present");
		assertThat(caught.getMessage())//
				.startsWith("Failed to resolve parameter [java.lang.String")//
				.contains("in method");
	}

	@Test
	void doNotWrapThrownExceptionIfItIsAlreadyAParameterResolutionException() {
		anyTestMethodWithAtLeastOneParameter();
		ParameterResolutionException cause = new ParameterResolutionException("custom message");
		throwDuringParameterResolution(cause);

		ParameterResolutionException caught = assertThrows(ParameterResolutionException.class, this::invokeMethod);

		assertSame(cause, caught);
	}

	private IllegalArgumentException anyExceptionButParameterResolutionException() {
		return new IllegalArgumentException();
	}

	private void throwDuringParameterResolution(RuntimeException parameterResolutionException) {
		register(ConfigurableParameterResolver.onAnyCallThrow(parameterResolutionException));
	}

	private void thereIsAParameterResolverThatResolvesTheParameterTo(Object argument) {
		register(ConfigurableParameterResolver.supportsAndResolvesTo(parameterContext -> argument));
	}

	private void thereIsAParameterResolverThatDoesNotSupportThisParameter() {
		register(ConfigurableParameterResolver.withoutSupport());
	}

	private void anyTestMethodWithAtLeastOneParameter() {
		testMethodWithASingleStringParameter();
	}

	private void testMethodWithNoParameters() {
		testMethodWith("noParameter");
	}

	private void testMethodWithASingleStringParameter() {
		testMethodWith("singleStringParameter", String.class);
	}

	private void testMethodWithASinglePrimitiveIntParameter() {
		testMethodWith("primitiveParameterInt", int.class);
	}

	private void testMethodWith(String methodName, Class<?>... parameterTypes) {
		this.method = ReflectionUtils.findMethod(this.instance.getClass(), methodName, parameterTypes).get();
	}

	private void register(ParameterResolver... resolvers) {
		for (ParameterResolver resolver : resolvers) {
			extensionRegistry.registerExtension(resolver, this);
		}
	}

	private ExecutableInvoker newInvoker() {
		return new ExecutableInvoker();
	}

	private void invokeMethod() {
		newInvoker().invoke(this.method, this.instance, this.extensionContext, this.extensionRegistry);
	}

	// -------------------------------------------------------------------------

	static class ArgumentRecordingParameterResolver implements ParameterResolver {

		Arguments supportsArguments;
		Arguments resolveArguments;

		static class Arguments {

			final ParameterContext parameterContext;
			final ExtensionContext extensionContext;

			Arguments(ParameterContext parameterContext, ExtensionContext extensionContext) {
				this.parameterContext = parameterContext;
				this.extensionContext = extensionContext;
			}
		}

		@Override
		public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
			supportsArguments = new Arguments(parameterContext, extensionContext);
			return true;
		}

		@Override
		public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
			resolveArguments = new Arguments(parameterContext, extensionContext);
			return null;
		}
	}

	static class ConfigurableParameterResolver implements ParameterResolver {

		static ParameterResolver onAnyCallThrow(RuntimeException runtimeException) {
			return new ConfigurableParameterResolver(parameterContext -> {
				throw runtimeException;
			}, parameterContext -> {
				throw runtimeException;
			});
		}

		static ParameterResolver supportsAndResolvesTo(Function<ParameterContext, Object> resolve) {
			return new ConfigurableParameterResolver(parameterContext -> true, resolve);
		}

		static ParameterResolver withoutSupport() {
			return new ConfigurableParameterResolver(parameterContext -> false, parameter -> {
				throw new UnsupportedOperationException();
			});
		}

		private final Predicate<ParameterContext> supports;
		private final Function<ParameterContext, Object> resolve;

		private ConfigurableParameterResolver(Predicate<ParameterContext> supports,
				Function<ParameterContext, Object> resolve) {
			this.supports = supports;
			this.resolve = resolve;
		}

		@Override
		public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
			return supports.test(parameterContext);
		}

		@Override
		public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
			return resolve.apply(parameterContext);
		}
	}

	interface MethodSource {

		void noParameter();

		void singleStringParameter(String parameter);

		void primitiveParameterInt(int parameter);

		void multipleParameters(String first, Integer second, Double third);
	}

	private static class StringParameterResolver implements ParameterResolver {

		@Override
		public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
			return parameterContext.getParameter().getType() == String.class;
		}

		@Override
		public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
			return ENIGMA;
		}
	}

	private static class NumberParameterResolver implements ParameterResolver {

		@Override
		public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
			return parameterContext.getParameter().getType() == Number.class;
		}

		@Override
		public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
			return 42;
		}
	}

	private static class ConstructorInjectionTestCase {

		final String str;

		@SuppressWarnings("unused")
		ConstructorInjectionTestCase(String str) {
			this.str = str;
		}

		class NestedTestCase {

			final Number num;

			@SuppressWarnings("unused")
			NestedTestCase(Number num) {
				this.num = num;
			}
		}
	}

}
