/*
 * Copyright 2015-2019 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.descriptor;

import static java.util.stream.Collectors.joining;
import static org.apiguardian.api.API.Status.INTERNAL;
import static org.junit.jupiter.engine.descriptor.DisplayNameUtils.createDisplayNameSupplierForClass;
import static org.junit.jupiter.engine.descriptor.ExtensionUtils.populateNewExtensionRegistryFromExtendWithAnnotation;
import static org.junit.jupiter.engine.descriptor.ExtensionUtils.registerExtensionsFromFields;
import static org.junit.jupiter.engine.descriptor.LifecycleMethodUtils.findAfterAllMethods;
import static org.junit.jupiter.engine.descriptor.LifecycleMethodUtils.findAfterEachMethods;
import static org.junit.jupiter.engine.descriptor.LifecycleMethodUtils.findBeforeAllMethods;
import static org.junit.jupiter.engine.descriptor.LifecycleMethodUtils.findBeforeEachMethods;
import static org.junit.jupiter.engine.descriptor.TestInstanceLifecycleUtils.getTestInstanceLifecycle;
import static org.junit.jupiter.engine.support.JupiterThrowableCollectorFactory.createThrowableCollector;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apiguardian.api.API;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstanceFactory;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.jupiter.api.extension.TestInstances;
import org.junit.jupiter.api.extension.TestInstantiationException;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.execution.AfterEachMethodAdapter;
import org.junit.jupiter.engine.execution.BeforeEachMethodAdapter;
import org.junit.jupiter.engine.execution.DefaultTestInstances;
import org.junit.jupiter.engine.execution.ExecutableInvoker;
import org.junit.jupiter.engine.execution.JupiterEngineExecutionContext;
import org.junit.jupiter.engine.execution.TestInstancesProvider;
import org.junit.jupiter.engine.extension.ExtensionRegistry;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.util.BlacklistedExceptions;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.commons.util.StringUtils;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.hierarchical.ExclusiveResource;
import org.junit.platform.engine.support.hierarchical.ThrowableCollector;

/**
 * {@link TestDescriptor} for tests based on Java classes.
 *
 * <h3>Default Display Names</h3>
 *
 * <p>The default display name for a top-level or nested static test class is
 * the fully qualified name of the class with the package name and leading dot
 * (".") removed.
 *
 * @since 5.0
 */
@API(status = INTERNAL, since = "5.0")
public class ClassTestDescriptor extends JupiterTestDescriptor {

	private static final ExecutableInvoker executableInvoker = new ExecutableInvoker();

	private final Class<?> testClass;
	private final Set<TestTag> tags;
	protected final Lifecycle lifecycle;

	private ExecutionMode defaultChildExecutionMode;
	private TestInstanceFactory testInstanceFactory;
	private List<Method> beforeAllMethods;
	private List<Method> afterAllMethods;

	public ClassTestDescriptor(UniqueId uniqueId, Class<?> testClass, JupiterConfiguration configuration) {
		this(uniqueId, testClass, createDisplayNameSupplierForClass(testClass), configuration);
	}

	ClassTestDescriptor(UniqueId uniqueId, Class<?> testClass, Supplier<String> displayNameSupplier,
			JupiterConfiguration configuration) {
		super(uniqueId, testClass, displayNameSupplier, ClassSource.from(testClass), configuration);

		this.testClass = testClass;
		this.tags = getTags(testClass);
		this.lifecycle = getTestInstanceLifecycle(testClass, configuration);
		this.defaultChildExecutionMode = (this.lifecycle == Lifecycle.PER_CLASS ? ExecutionMode.SAME_THREAD : null);
	}

	// --- TestDescriptor ------------------------------------------------------

	@Override
	public Set<TestTag> getTags() {
		// return modifiable copy
		return new LinkedHashSet<>(this.tags);
	}

	public final Class<?> getTestClass() {
		return this.testClass;
	}

	@Override
	public Type getType() {
		return Type.CONTAINER;
	}

	@Override
	public String getLegacyReportingName() {
		return this.testClass.getName();
	}

	// --- Node ----------------------------------------------------------------

	@Override
	protected Optional<ExecutionMode> getExplicitExecutionMode() {
		return getExecutionModeFromAnnotation(getTestClass());
	}

	@Override
	protected Optional<ExecutionMode> getDefaultChildExecutionMode() {
		return Optional.ofNullable(this.defaultChildExecutionMode);
	}

	public void setDefaultChildExecutionMode(ExecutionMode defaultChildExecutionMode) {
		this.defaultChildExecutionMode = defaultChildExecutionMode;
	}

	@Override
	public Set<ExclusiveResource> getExclusiveResources() {
		return getExclusiveResourcesFromAnnotation(getTestClass());
	}

	@Override
	public JupiterEngineExecutionContext prepare(JupiterEngineExecutionContext context) {
		ExtensionRegistry registry = populateNewExtensionRegistryFromExtendWithAnnotation(
			context.getExtensionRegistry(), this.testClass);

		// Register extensions from static fields here, at the class level but
		// after extensions registered via @ExtendWith.
		registerExtensionsFromFields(registry, this.testClass, null);

		// Resolve the TestInstanceFactory at the class level in order to fail
		// the entire class in case of configuration errors (e.g., more than
		// one factory registered per class).
		this.testInstanceFactory = resolveTestInstanceFactory(registry);

		registerBeforeEachMethodAdapters(registry);
		registerAfterEachMethodAdapters(registry);

		ThrowableCollector throwableCollector = createThrowableCollector();
		ClassExtensionContext extensionContext = new ClassExtensionContext(context.getExtensionContext(),
			context.getExecutionListener(), this, this.lifecycle, context.getConfiguration(), throwableCollector);

		this.beforeAllMethods = findBeforeAllMethods(this.testClass, this.lifecycle == Lifecycle.PER_METHOD);
		this.afterAllMethods = findAfterAllMethods(this.testClass, this.lifecycle == Lifecycle.PER_METHOD);

		// @formatter:off
		return context.extend()
				.withTestInstancesProvider(testInstancesProvider(context, registry, extensionContext))
				.withExtensionRegistry(registry)
				.withExtensionContext(extensionContext)
				.withThrowableCollector(throwableCollector)
				.build();
		// @formatter:on
	}

	@Override
	public JupiterEngineExecutionContext before(JupiterEngineExecutionContext context) {
		ThrowableCollector throwableCollector = context.getThrowableCollector();

		Lifecycle lifecycle = context.getExtensionContext().getTestInstanceLifecycle().orElse(Lifecycle.PER_METHOD);
		if (lifecycle == Lifecycle.PER_CLASS) {
			// Eagerly load test instance for BeforeAllCallbacks, if necessary,
			// and store the instance in the ExtensionContext.
			ClassExtensionContext extensionContext = (ClassExtensionContext) context.getExtensionContext();
			throwableCollector.execute(() -> extensionContext.setTestInstances(
				context.getTestInstancesProvider().getTestInstances(Optional.empty())));
		}

		if (throwableCollector.isEmpty()) {
			context.beforeAllCallbacksExecuted(true);
			invokeBeforeAllCallbacks(context);

			if (throwableCollector.isEmpty()) {
				context.beforeAllMethodsExecuted(true);
				invokeBeforeAllMethods(context);
			}
		}

		throwableCollector.assertEmpty();

		return context;
	}

	@Override
	public void after(JupiterEngineExecutionContext context) {

		ThrowableCollector throwableCollector = context.getThrowableCollector();
		Throwable previousThrowable = throwableCollector.getThrowable();

		if (context.beforeAllMethodsExecuted()) {
			invokeAfterAllMethods(context);
		}

		if (context.beforeAllCallbacksExecuted()) {
			invokeAfterAllCallbacks(context);
		}

		// If the previous Throwable was not null when this method was called,
		// that means an exception was already thrown either before or during
		// the execution of this Node. If an exception was already thrown, any
		// later exceptions were added as suppressed exceptions to that original
		// exception unless a more severe exception occurred in the meantime.
		if (previousThrowable != throwableCollector.getThrowable()) {
			throwableCollector.assertEmpty();
		}
	}

	private TestInstanceFactory resolveTestInstanceFactory(ExtensionRegistry registry) {
		List<TestInstanceFactory> factories = registry.getExtensions(TestInstanceFactory.class);

		if (factories.size() == 1) {
			return factories.get(0);
		}

		if (factories.size() > 1) {
			String factoryNames = factories.stream()//
					.map(factory -> factory.getClass().getName())//
					.collect(joining(", "));

			String errorMessage = String.format(
				"The following TestInstanceFactory extensions were registered for test class [%s], but only one is permitted: %s",
				testClass.getName(), factoryNames);

			throw new ExtensionConfigurationException(errorMessage);
		}

		return null;
	}

	private TestInstancesProvider testInstancesProvider(JupiterEngineExecutionContext parentExecutionContext,
			ExtensionRegistry registry, ClassExtensionContext extensionContext) {

		TestInstancesProvider testInstancesProvider = childRegistry -> instantiateAndPostProcessTestInstance(
			parentExecutionContext, extensionContext, childRegistry.orElse(registry));

		return childRegistry -> extensionContext.getTestInstances().orElseGet(
			() -> testInstancesProvider.getTestInstances(childRegistry));
	}

	private TestInstances instantiateAndPostProcessTestInstance(JupiterEngineExecutionContext parentExecutionContext,
			ExtensionContext extensionContext, ExtensionRegistry registry) {

		TestInstances instances = instantiateTestClass(parentExecutionContext, registry, extensionContext);
		invokeTestInstancePostProcessors(instances.getInnermostInstance(), registry, extensionContext);
		// In addition, we register extensions from instance fields here since the
		// best time to do that is immediately following test class instantiation
		// and post processing.
		registerExtensionsFromFields(registry, this.testClass, instances.getInnermostInstance());
		return instances;
	}

	protected TestInstances instantiateTestClass(JupiterEngineExecutionContext parentExecutionContext,
			ExtensionRegistry registry, ExtensionContext extensionContext) {

		return instantiateTestClass(Optional.empty(), registry, extensionContext);
	}

	protected TestInstances instantiateTestClass(Optional<TestInstances> outerInstances, ExtensionRegistry registry,
			ExtensionContext extensionContext) {

		Optional<Object> outerInstance = outerInstances.map(TestInstances::getInnermostInstance);
		Object instance = this.testInstanceFactory != null //
				? invokeTestInstanceFactory(outerInstance, extensionContext) //
				: invokeTestClassConstructor(outerInstance, registry, extensionContext);
		return outerInstances.map(instances -> DefaultTestInstances.of(instances, instance)).orElse(
			DefaultTestInstances.of(instance));
	}

	private Object invokeTestInstanceFactory(Optional<Object> outerInstance, ExtensionContext extensionContext) {
		Object instance;

		try {
			instance = this.testInstanceFactory.createTestInstance(
				new DefaultTestInstanceFactoryContext(this.testClass, outerInstance), extensionContext);
		}
		catch (Throwable throwable) {
			BlacklistedExceptions.rethrowIfBlacklisted(throwable);

			if (throwable instanceof TestInstantiationException) {
				throw (TestInstantiationException) throwable;
			}

			String message = String.format("TestInstanceFactory [%s] failed to instantiate test class [%s]",
				this.testInstanceFactory.getClass().getName(), this.testClass.getName());
			if (StringUtils.isNotBlank(throwable.getMessage())) {
				message += ": " + throwable.getMessage();
			}
			throw new TestInstantiationException(message, throwable);
		}

		if (!this.testClass.isInstance(instance)) {
			String testClassName = this.testClass.getName();
			Class<?> instanceClass = (instance == null ? null : instance.getClass());
			String instanceClassName = (instanceClass == null ? "null" : instanceClass.getName());

			// If the test instance was loaded via a different ClassLoader, append
			// the identity hash codes to the type names to help users disambiguate
			// between otherwise identical "fully qualified class names".
			if (testClassName.equals(instanceClassName)) {
				testClassName += "@" + Integer.toHexString(System.identityHashCode(this.testClass));
				instanceClassName += "@" + Integer.toHexString(System.identityHashCode(instanceClass));
			}
			String message = String.format(
				"TestInstanceFactory [%s] failed to return an instance of [%s] and instead returned an instance of [%s].",
				this.testInstanceFactory.getClass().getName(), testClassName, instanceClassName);

			throw new TestInstantiationException(message);
		}

		return instance;
	}

	private Object invokeTestClassConstructor(Optional<Object> outerInstance, ExtensionRegistry registry,
			ExtensionContext extensionContext) {

		Constructor<?> constructor = ReflectionUtils.getDeclaredConstructor(this.testClass);
		return outerInstance.isPresent() //
				? executableInvoker.invoke(constructor, outerInstance.get(), extensionContext, registry) //
				: executableInvoker.invoke(constructor, extensionContext, registry);
	}

	private void invokeTestInstancePostProcessors(Object instance, ExtensionRegistry registry,
			ExtensionContext context) {

		registry.stream(TestInstancePostProcessor.class).forEach(
			extension -> executeAndMaskThrowable(() -> extension.postProcessTestInstance(instance, context)));
	}

	private void invokeBeforeAllCallbacks(JupiterEngineExecutionContext context) {
		ExtensionRegistry registry = context.getExtensionRegistry();
		ExtensionContext extensionContext = context.getExtensionContext();
		ThrowableCollector throwableCollector = context.getThrowableCollector();

		for (BeforeAllCallback callback : registry.getExtensions(BeforeAllCallback.class)) {
			throwableCollector.execute(() -> callback.beforeAll(extensionContext));
			if (throwableCollector.isNotEmpty()) {
				break;
			}
		}
	}

	private void invokeBeforeAllMethods(JupiterEngineExecutionContext context) {
		ExtensionRegistry registry = context.getExtensionRegistry();
		ExtensionContext extensionContext = context.getExtensionContext();
		ThrowableCollector throwableCollector = context.getThrowableCollector();
		Object testInstance = extensionContext.getTestInstance().orElse(null);

		for (Method method : this.beforeAllMethods) {
			throwableCollector.execute(
				() -> executableInvoker.invoke(method, testInstance, extensionContext, registry));
			if (throwableCollector.isNotEmpty()) {
				break;
			}
		}
	}

	private void invokeAfterAllMethods(JupiterEngineExecutionContext context) {
		ExtensionRegistry registry = context.getExtensionRegistry();
		ExtensionContext extensionContext = context.getExtensionContext();
		ThrowableCollector throwableCollector = context.getThrowableCollector();
		Object testInstance = extensionContext.getTestInstance().orElse(null);

		this.afterAllMethods.forEach(method -> throwableCollector.execute(
			() -> executableInvoker.invoke(method, testInstance, extensionContext, registry)));
	}

	private void invokeAfterAllCallbacks(JupiterEngineExecutionContext context) {
		ExtensionRegistry registry = context.getExtensionRegistry();
		ExtensionContext extensionContext = context.getExtensionContext();
		ThrowableCollector throwableCollector = context.getThrowableCollector();

		registry.getReversedExtensions(AfterAllCallback.class)//
				.forEach(extension -> throwableCollector.execute(() -> extension.afterAll(extensionContext)));
	}

	private void registerBeforeEachMethodAdapters(ExtensionRegistry registry) {
		List<Method> beforeEachMethods = findBeforeEachMethods(this.testClass);
		registerMethodsAsExtensions(beforeEachMethods, registry, this::synthesizeBeforeEachMethodAdapter);
	}

	private void registerAfterEachMethodAdapters(ExtensionRegistry registry) {
		// Make a local copy since findAfterEachMethods() returns an immutable list.
		List<Method> afterEachMethods = new ArrayList<>(findAfterEachMethods(this.testClass));

		// Since the bottom-up ordering of afterEachMethods will later be reversed when the
		// synthesized AfterEachMethodAdapters are executed within TestMethodTestDescriptor,
		// we have to reverse the afterEachMethods list to put them in top-down order before
		// we register them as synthesized extensions.
		Collections.reverse(afterEachMethods);

		registerMethodsAsExtensions(afterEachMethods, registry, this::synthesizeAfterEachMethodAdapter);
	}

	private void registerMethodsAsExtensions(List<Method> methods, ExtensionRegistry registry,
			Function<Method, Extension> extensionSynthesizer) {

		methods.forEach(method -> registry.registerExtension(extensionSynthesizer.apply(method), method));
	}

	private BeforeEachMethodAdapter synthesizeBeforeEachMethodAdapter(Method method) {
		return (extensionContext, registry) -> invokeMethodInExtensionContext(method, extensionContext, registry);
	}

	private AfterEachMethodAdapter synthesizeAfterEachMethodAdapter(Method method) {
		return (extensionContext, registry) -> invokeMethodInExtensionContext(method, extensionContext, registry);
	}

	private void invokeMethodInExtensionContext(Method method, ExtensionContext context, ExtensionRegistry registry) {
		TestInstances testInstances = context.getRequiredTestInstances();
		Object target = testInstances.findInstance(method.getDeclaringClass()).orElseThrow(
			() -> new JUnitException("Failed to find instance for method: " + method.toGenericString()));

		executableInvoker.invoke(method, target, context, registry);
	}

}
