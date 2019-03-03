/*
 * Copyright 2015-2019 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.util;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static org.apiguardian.api.API.Status.INTERNAL;

import java.util.function.Function;

import org.apiguardian.api.API;

/**
 * Collection of utilities for working with {@link Class classes}.
 *
 * <h3>DISCLAIMER</h3>
 *
 * <p>These utilities are intended solely for usage within the JUnit framework
 * itself. <strong>Any usage by external parties is not supported.</strong>
 * Use at your own risk!
 *
 * @since 1.0
 */
@API(status = INTERNAL, since = "1.0")
public final class ClassUtils {

	private ClassUtils() {
		/* no-op */
	}

	/**
	 * Get the fully qualified name of the supplied class.
	 *
	 * <p>This is a null-safe variant of {@link Class#getName()}.
	 *
	 * @param clazz the class whose name should be retrieved, potentially
	 * {@code null}
	 * @return the fully qualified class name or {@code "null"} if the supplied
	 * class reference is {@code null}
	 * @since 1.3
	 * @see #nullSafeToString(Class...)
	 * @see StringUtils#nullSafeToString(Object)
	 */
	public static String nullSafeToString(Class<?> clazz) {
		return clazz == null ? "null" : clazz.getName();
	}

	/**
	 * Generate a comma-separated list of fully qualified class names for the
	 * supplied classes.
	 *
	 * @param classes the classes whose names should be included in the
	 * generated string
	 * @return a comma-separated list of fully qualified class names, or an empty
	 * string if the supplied class array is {@code null} or empty
	 * @see #nullSafeToString(Function, Class...)
	 * @see StringUtils#nullSafeToString(Object)
	 */
	public static String nullSafeToString(Class<?>... classes) {
		return nullSafeToString(Class::getName, classes);
	}

	/**
	 * Generate a comma-separated list of mapped values for the supplied classes.
	 *
	 * <p>The values are generated by the supplied {@code mapper}
	 * (e.g., {@code Class::getName}, {@code Class::getSimpleName}, etc.), unless
	 * a class reference is {@code null} in which case it will be mapped to
	 * {@code "null"}.
	 *
	 * @param mapper the mapper to use; never {@code null}
	 * @param classes the classes to map
	 * @return a comma-separated list of mapped values, or an empty string if
	 * the supplied class array is {@code null} or empty
	 * @see #nullSafeToString(Class...)
	 * @see StringUtils#nullSafeToString(Object)
	 */
	public static String nullSafeToString(Function<? super Class<?>, ? extends String> mapper, Class<?>... classes) {
		Preconditions.notNull(mapper, "Mapping function must not be null");

		if (classes == null || classes.length == 0) {
			return "";
		}
		return stream(classes).map(clazz -> clazz == null ? "null" : mapper.apply(clazz)).collect(joining(", "));
	}

}
