/*
 * Copyright 2015-2019 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.execution;

import static java.util.stream.Collectors.joining;
import static org.apiguardian.api.API.Status.INTERNAL;

import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apiguardian.api.API;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.InvocationInterceptor.Invocation;
import org.junit.jupiter.engine.extension.ExtensionRegistry;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.util.ExceptionUtils;

@API(status = INTERNAL, since = "5.5")
public class InvocationInterceptorChain {

	public <T> T invoke(Invocation<T> invocation, ExtensionRegistry extensionRegistry, InterceptorCall<T> call) {
		List<InvocationInterceptor> interceptors = extensionRegistry.getExtensions(InvocationInterceptor.class);
		if (interceptors.isEmpty()) {
			return proceed(invocation);
		}
		return chainAndInvoke(invocation, call, interceptors);
	}

	private <T> T chainAndInvoke(Invocation<T> invocation, InterceptorCall<T> call,
			List<InvocationInterceptor> interceptors) {

		ValidatingInvocation<T> validatingInvocation = new ValidatingInvocation<>(invocation, interceptors);
		Invocation<T> chainedInvocation = chainInterceptors(validatingInvocation, call, interceptors);
		T result = proceed(chainedInvocation);
		validatingInvocation.verifyInvokedAtLeastOnce();
		return result;
	}

	private <T> Invocation<T> chainInterceptors(Invocation<T> invocation, InterceptorCall<T> call,
			List<InvocationInterceptor> interceptors) {

		Invocation<T> result = invocation;
		ListIterator<InvocationInterceptor> iterator = interceptors.listIterator(interceptors.size());
		while (iterator.hasPrevious()) {
			InvocationInterceptor interceptor = iterator.previous();
			result = new InterceptedInvocation<>(result, call, interceptor);
		}
		return result;
	}

	private <T> T proceed(Invocation<T> invocation) {
		try {
			return invocation.proceed();
		}
		catch (Throwable t) {
			throw ExceptionUtils.throwAsUncheckedException(t);
		}
	}

	@FunctionalInterface
	public interface InterceptorCall<T> {

		T apply(InvocationInterceptor interceptor, Invocation<T> invocation) throws Throwable;

		static InterceptorCall<Void> ofVoid(VoidInterceptorCall call) {
			return ((interceptorChain, invocation) -> {
				call.apply(interceptorChain, invocation);
				return null;
			});
		}

	}

	@FunctionalInterface
	public interface VoidInterceptorCall {

		void apply(InvocationInterceptor interceptor, Invocation<Void> invocation) throws Throwable;

	}

	private static class InterceptedInvocation<T> implements Invocation<T> {

		private final Invocation<T> invocation;
		private final InterceptorCall<T> call;
		private final InvocationInterceptor interceptor;

		InterceptedInvocation(Invocation<T> invocation, InterceptorCall<T> call, InvocationInterceptor interceptor) {
			this.invocation = invocation;
			this.call = call;
			this.interceptor = interceptor;
		}

		@Override
		public T proceed() throws Throwable {
			return call.apply(interceptor, invocation);
		}

	}

	private static class ValidatingInvocation<T> implements Invocation<T> {

		private final AtomicBoolean invoked = new AtomicBoolean();
		private final Invocation<T> delegate;
		private final List<InvocationInterceptor> interceptors;

		ValidatingInvocation(Invocation<T> delegate, List<InvocationInterceptor> interceptors) {
			this.delegate = delegate;
			this.interceptors = interceptors;
		}

		@Override
		public T proceed() throws Throwable {
			if (!invoked.compareAndSet(false, true)) {
				fail("Chain of InvocationInterceptors called invocation multiple times instead of just once");
			}
			return delegate.proceed();
		}

		void verifyInvokedAtLeastOnce() {
			if (!invoked.get()) {
				fail("Chain of InvocationInterceptors never called invocation");
			}
		}

		private void fail(String prefix) {
			String commaSeparatedInterceptorClasses = interceptors.stream().map(Object::getClass).map(
				Class::getName).collect(joining(", "));
			throw new JUnitException(prefix + ": " + commaSeparatedInterceptorClasses);
		}

	}

}
