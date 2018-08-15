/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.descriptor;

import static org.apiguardian.api.API.Status.STABLE;
import static org.junit.platform.commons.util.ClassUtils.nullSafeToString;

import java.lang.reflect.Method;
import java.util.Objects;

import org.apiguardian.api.API;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ToStringBuilder;
import org.junit.platform.engine.TestSource;

/**
 * Method based {@link org.junit.platform.engine.TestSource TestSource}.
 *
 * <p>This class stores the method name along with the names of its parameter
 * types because {@link Method} does not implement {@link java.io.Serializable}.
 *
 * @since 1.0
 * @see org.junit.platform.engine.discovery.MethodSelector
 */
@API(status = STABLE, since = "1.0")
public class MethodSource implements TestSource {

	private static final long serialVersionUID = 1L;

	/**
	 * Create a new {@code MethodSource} using the supplied class name and
	 * method name.
	 *
	 * @param className the class name; must not be {@code null} or blank
	 * @param methodName the method name; must not be {@code null} or blank
	 */
	public static MethodSource from(String className, String methodName) {
		return new MethodSource(className, methodName);
	}

	/**
	 * Create a new {@code MethodSource} using the supplied class name, method
	 * name, and method parameter types.
	 *
	 * @param className the class name; must not be {@code null} or blank
	 * @param methodName the method name; must not be {@code null} or blank
	 * @param methodParameterTypes the method parameter types as a string
	 */
	public static MethodSource from(String className, String methodName, String methodParameterTypes) {
		return new MethodSource(className, methodName, methodParameterTypes);
	}

	/**
	 * Create a new {@code MethodSource} using the supplied {@link Method method}.
	 *
	 * @param testMethod the Java method; must not be {@code null}
	 * @see #from(Class, Method)
	 */
	public static MethodSource from(Method testMethod) {
		return new MethodSource(testMethod);
	}

	/**
	 * Create a new {@code MethodSource} using the supplied
	 * {@link Class class} and {@link Method method}.
	 *
	 * <p>This method should be used in favor of {@link #from(Method)} if the
	 * test method is inherited from a superclass or present as an interface
	 * {@code default} method.
	 *
	 * @param testClass the Java class; must not be {@code null}
	 * @param testMethod the Java method; must not be {@code null}
	 * @since 1.3
	 */
	@API(status = STABLE, since = "1.3")
	public static MethodSource from(Class<?> testClass, Method testMethod) {
		return new MethodSource(testClass, testMethod);
	}

	private final String className;
	private final String methodName;
	private final String methodParameterTypes;

	private MethodSource(String className, String methodName) {
		this(className, methodName, null);
	}

	private MethodSource(String className, String methodName, String methodParameterTypes) {
		Preconditions.notBlank(className, "Class name must not be null or blank");
		Preconditions.notBlank(methodName, "Method name must not be null or blank");
		this.className = className;
		this.methodName = methodName;
		this.methodParameterTypes = methodParameterTypes;
	}

	private MethodSource(Method testMethod) {
		this(Preconditions.notNull(testMethod, "Method must not be null").getDeclaringClass(), testMethod);
	}

	/**
	 * @since 1.3
	 */
	private MethodSource(Class<?> testClass, Method testMethod) {
		Preconditions.notNull(testClass, "Class must not be null");
		Preconditions.notNull(testMethod, "Method must not be null");
		this.className = testClass.getName();
		this.methodName = testMethod.getName();
		this.methodParameterTypes = nullSafeToString(testMethod.getParameterTypes());
	}

	/**
	 * Get the class name of this source.
	 */
	public String getClassName() {
		return this.className;
	}

	/**
	 * Get the method name of this source.
	 */
	public final String getMethodName() {
		return this.methodName;
	}

	/**
	 * Get the method parameter types of this source.
	 */
	public final String getMethodParameterTypes() {
		return this.methodParameterTypes;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		MethodSource that = (MethodSource) o;
		return Objects.equals(this.className, that.className)//
				&& Objects.equals(this.methodName, that.methodName)//
				&& Objects.equals(this.methodParameterTypes, that.methodParameterTypes);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.className, this.methodName, this.methodParameterTypes);
	}

	@Override
	public String toString() {
		// @formatter:off
		return new ToStringBuilder(this)
				.append("className", this.className)
				.append("methodName", this.methodName)
				.append("methodParameterTypes", this.methodParameterTypes)
				.toString();
		// @formatter:on
	}

}
