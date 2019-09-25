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

import static org.apiguardian.api.API.Status.INTERNAL;
import static org.junit.jupiter.engine.descriptor.ExtensionUtils.populateNewExtensionRegistryFromExtendWithAnnotation;
import static org.junit.jupiter.engine.support.JupiterThrowableCollectorFactory.createThrowableCollector;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.apiguardian.api.API;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestInstances;
import org.junit.jupiter.api.extension.TestWatcher;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.execution.AfterEachMethodAdapter;
import org.junit.jupiter.engine.execution.BeforeEachMethodAdapter;
import org.junit.jupiter.engine.execution.ExecutableInvoker;
import org.junit.jupiter.engine.execution.ExecutableInvoker.ReflectiveInterceptorCall;
import org.junit.jupiter.engine.execution.JupiterEngineExecutionContext;
import org.junit.jupiter.engine.extension.ExtensionRegistry;
import org.junit.jupiter.engine.extension.MutableExtensionRegistry;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.commons.util.BlacklistedExceptions;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.hierarchical.ThrowableCollector;

/**
 * {@link TestDescriptor} for {@link org.junit.jupiter.api.Test @Test} methods.
 *
 * <h3>Default Display Names</h3>
 *
 * <p>The default display name for a test method is the name of the method
 * concatenated with a comma-separated list of parameter types in parentheses.
 * The names of parameter types are retrieved using {@link Class#getSimpleName()}.
 * For example, the default display name for the following test method is
 * {@code testUser(TestInfo, User)}.
 *
 * <pre class="code">
 *   {@literal @}Test
 *   void testUser(TestInfo testInfo, {@literal @}Mock User user) { ... }
 * </pre>
 *
 * @since 5.0
 */
@API(status = INTERNAL, since = "5.0")
public class TestMethodTestDescriptor extends MethodBasedTestDescriptor {

	public static final String SEGMENT_TYPE = "method";
	private static final ExecutableInvoker executableInvoker = new ExecutableInvoker();
	private static final Logger logger = LoggerFactory.getLogger(TestMethodTestDescriptor.class);
	private static final ReflectiveInterceptorCall<Method, Void> defaultInterceptorCall = ReflectiveInterceptorCall.ofVoidMethod(
		InvocationInterceptor::interceptTestMethod);

	private final ReflectiveInterceptorCall<Method, Void> interceptorCall;

	public TestMethodTestDescriptor(UniqueId uniqueId, Class<?> testClass, Method testMethod,
			JupiterConfiguration configuration) {
		super(uniqueId, testClass, testMethod, configuration);
		this.interceptorCall = defaultInterceptorCall;
	}

	TestMethodTestDescriptor(UniqueId uniqueId, String displayName, Class<?> testClass, Method testMethod,
			JupiterConfiguration configuration, ReflectiveInterceptorCall<Method, Void> interceptorCall) {
		super(uniqueId, displayName, testClass, testMethod, configuration);
		this.interceptorCall = interceptorCall;
	}

	@Override
	public Type getType() {
		return Type.TEST;
	}

	// --- Node ----------------------------------------------------------------

	@Override
	public JupiterEngineExecutionContext prepare(JupiterEngineExecutionContext context) {
		MutableExtensionRegistry registry = populateNewExtensionRegistry(context);
		ThrowableCollector throwableCollector = createThrowableCollector();
		MethodExtensionContext extensionContext = new MethodExtensionContext(context.getExtensionContext(),
			context.getExecutionListener(), this, context.getConfiguration(), throwableCollector);
		throwableCollector.execute(() -> {
			TestInstances testInstances = context.getTestInstancesProvider().getTestInstances(registry);
			extensionContext.setTestInstances(testInstances);
		});

		// @formatter:off
		return context.extend()
				.withExtensionRegistry(registry)
				.withExtensionContext(extensionContext)
				.withThrowableCollector(throwableCollector)
				.build();
		// @formatter:on
	}

	protected MutableExtensionRegistry populateNewExtensionRegistry(JupiterEngineExecutionContext context) {
		return populateNewExtensionRegistryFromExtendWithAnnotation(context.getExtensionRegistry(), getTestMethod());
	}

	@Override
	public JupiterEngineExecutionContext execute(JupiterEngineExecutionContext context,
			DynamicTestExecutor dynamicTestExecutor) throws Exception {
		ThrowableCollector throwableCollector = context.getThrowableCollector();

		// @formatter:off
		invokeBeforeEachCallbacks(context);
			if (throwableCollector.isEmpty()) {
				invokeBeforeEachMethods(context);
				if (throwableCollector.isEmpty()) {
					invokeBeforeTestExecutionCallbacks(context);
					if (throwableCollector.isEmpty()) {
						invokeTestMethod(context, dynamicTestExecutor);
					}
					invokeAfterTestExecutionCallbacks(context);
				}
				invokeAfterEachMethods(context);
			}
		invokeAfterEachCallbacks(context);
		// @formatter:on

		throwableCollector.assertEmpty();

		return context;
	}

	private void invokeBeforeEachCallbacks(JupiterEngineExecutionContext context) {
		invokeBeforeMethodsOrCallbacksUntilExceptionOccurs(BeforeEachCallback.class, context,
			(callback, extensionContext) -> callback.beforeEach(extensionContext));
	}

	private void invokeBeforeEachMethods(JupiterEngineExecutionContext context) {
		ExtensionRegistry registry = context.getExtensionRegistry();
		invokeBeforeMethodsOrCallbacksUntilExceptionOccurs(BeforeEachMethodAdapter.class, context,
			(adapter, extensionContext) -> {
				try {
					adapter.invokeBeforeEachMethod(extensionContext, registry);
				}
				catch (Throwable throwable) {
					invokeBeforeEachExecutionExceptionHandlers(extensionContext, registry, throwable);
				}
			});
	}

	private void invokeBeforeEachExecutionExceptionHandlers(ExtensionContext context, ExtensionRegistry registry,
			Throwable throwable) {

		invokeExecutionExceptionHandlers(LifecycleMethodExecutionExceptionHandler.class, registry, throwable,
			(handler, handledThrowable) -> handler.handleBeforeEachMethodExecutionException(context, handledThrowable));
	}

	private void invokeBeforeTestExecutionCallbacks(JupiterEngineExecutionContext context) {
		invokeBeforeMethodsOrCallbacksUntilExceptionOccurs(BeforeTestExecutionCallback.class, context,
			(callback, extensionContext) -> callback.beforeTestExecution(extensionContext));
	}

	private <T extends Extension> void invokeBeforeMethodsOrCallbacksUntilExceptionOccurs(Class<T> type,
			JupiterEngineExecutionContext context, CallbackInvoker<T> callbackInvoker) {

		ExtensionRegistry registry = context.getExtensionRegistry();
		ExtensionContext extensionContext = context.getExtensionContext();
		ThrowableCollector throwableCollector = context.getThrowableCollector();

		for (T callback : registry.getExtensions(type)) {
			throwableCollector.execute(() -> callbackInvoker.invoke(callback, extensionContext));
			if (throwableCollector.isNotEmpty()) {
				break;
			}
		}
	}

	protected void invokeTestMethod(JupiterEngineExecutionContext context, DynamicTestExecutor dynamicTestExecutor) {
		ExtensionContext extensionContext = context.getExtensionContext();
		ThrowableCollector throwableCollector = context.getThrowableCollector();

		throwableCollector.execute(() -> {
			try {
				Method testMethod = getTestMethod();
				Object instance = extensionContext.getRequiredTestInstance();
				executableInvoker.invoke(testMethod, instance, extensionContext, context.getExtensionRegistry(),
					interceptorCall);
			}
			catch (Throwable throwable) {
				BlacklistedExceptions.rethrowIfBlacklisted(throwable);
				invokeTestExecutionExceptionHandlers(context.getExtensionRegistry(), extensionContext, throwable);
			}
		});
	}

	private void invokeTestExecutionExceptionHandlers(ExtensionRegistry registry, ExtensionContext context,
			Throwable throwable) {

		invokeExecutionExceptionHandlers(TestExecutionExceptionHandler.class, registry, throwable,
			(handler, handledThrowable) -> handler.handleTestExecutionException(context, handledThrowable));
	}

	private void invokeAfterTestExecutionCallbacks(JupiterEngineExecutionContext context) {
		invokeAllAfterMethodsOrCallbacks(AfterTestExecutionCallback.class, context,
			(callback, extensionContext) -> callback.afterTestExecution(extensionContext));
	}

	private void invokeAfterEachMethods(JupiterEngineExecutionContext context) {
		ExtensionRegistry registry = context.getExtensionRegistry();
		invokeAllAfterMethodsOrCallbacks(AfterEachMethodAdapter.class, context, (adapter, extensionContext) -> {
			try {
				adapter.invokeAfterEachMethod(extensionContext, registry);
			}
			catch (Throwable throwable) {
				invokeAfterEachExecutionExceptionHandlers(extensionContext, registry, throwable);
			}
		});
	}

	private void invokeAfterEachExecutionExceptionHandlers(ExtensionContext context, ExtensionRegistry registry,
			Throwable throwable) {

		invokeExecutionExceptionHandlers(LifecycleMethodExecutionExceptionHandler.class, registry, throwable,
			(handler, handledThrowable) -> handler.handleAfterEachMethodExecutionException(context, handledThrowable));
	}

	private void invokeAfterEachCallbacks(JupiterEngineExecutionContext context) {
		invokeAllAfterMethodsOrCallbacks(AfterEachCallback.class, context,
			(callback, extensionContext) -> callback.afterEach(extensionContext));
	}

	private <T extends Extension> void invokeAllAfterMethodsOrCallbacks(Class<T> type,
			JupiterEngineExecutionContext context, CallbackInvoker<T> callbackInvoker) {

		ExtensionRegistry registry = context.getExtensionRegistry();
		ExtensionContext extensionContext = context.getExtensionContext();
		ThrowableCollector throwableCollector = context.getThrowableCollector();

		registry.getReversedExtensions(type).forEach(callback -> {
			throwableCollector.execute(() -> callbackInvoker.invoke(callback, extensionContext));
		});
	}

	/**
	 * Invoke {@link TestWatcher#testDisabled(ExtensionContext, Optional)} on each
	 * registered {@link TestWatcher}, in registration order.
	 *
	 * @since 5.4
	 */
	@Override
	public void nodeSkipped(JupiterEngineExecutionContext context, TestDescriptor descriptor, SkipResult result) {
		if (context != null) {
			invokeTestWatchers(context, false,
				watcher -> watcher.testDisabled(context.getExtensionContext(), result.getReason()));
		}
	}

	/**
	 * Invoke {@link TestWatcher#testSuccessful testSuccessful()},
	 * {@link TestWatcher#testAborted testAborted()}, or
	 * {@link TestWatcher#testFailed testFailed()} on each
	 * registered {@link TestWatcher} according to the status of the supplied
	 * {@link TestExecutionResult}, in reverse registration order.
	 *
	 * @since 5.4
	 */
	@Override
	public void nodeFinished(JupiterEngineExecutionContext context, TestDescriptor descriptor,
			TestExecutionResult result) {

		if (context != null) {
			ExtensionContext extensionContext = context.getExtensionContext();
			TestExecutionResult.Status status = result.getStatus();

			invokeTestWatchers(context, true, watcher -> {
				switch (status) {
					case SUCCESSFUL:
						watcher.testSuccessful(extensionContext);
						break;
					case ABORTED:
						watcher.testAborted(extensionContext, result.getThrowable().orElse(null));
						break;
					case FAILED:
						watcher.testFailed(extensionContext, result.getThrowable().orElse(null));
						break;
				}
			});
		}
	}

	/**
	 * @since 5.4
	 */
	private void invokeTestWatchers(JupiterEngineExecutionContext context, boolean reverseOrder,
			Consumer<TestWatcher> callback) {

		ExtensionRegistry registry = context.getExtensionRegistry();

		List<TestWatcher> watchers = reverseOrder //
				? registry.getReversedExtensions(TestWatcher.class)
				: registry.getExtensions(TestWatcher.class);

		watchers.forEach(watcher -> {
			try {
				callback.accept(watcher);
			}
			catch (Throwable throwable) {
				BlacklistedExceptions.rethrowIfBlacklisted(throwable);
				ExtensionContext extensionContext = context.getExtensionContext();
				logger.warn(throwable,
					() -> String.format("Failed to invoke TestWatcher [%s] for method [%s] with display name [%s]",
						watcher.getClass().getName(),
						ReflectionUtils.getFullyQualifiedMethodName(extensionContext.getRequiredTestClass(),
							extensionContext.getRequiredTestMethod()),
						getDisplayName()));
			}
		});
	}

	/**
	 * @since 5.5
	 */
	@FunctionalInterface
	private interface CallbackInvoker<T extends Extension> {

		void invoke(T t, ExtensionContext context) throws Throwable;

	}

}
