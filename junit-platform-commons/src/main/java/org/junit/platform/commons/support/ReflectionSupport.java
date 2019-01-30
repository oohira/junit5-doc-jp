/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.support;

import static org.apiguardian.api.API.Status.DEPRECATED;
import static org.apiguardian.api.API.Status.MAINTAINED;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.apiguardian.api.API;
import org.junit.platform.commons.function.Try;
import org.junit.platform.commons.util.ExceptionUtils;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ReflectionUtils;

/**
 * {@code ReflectionSupport} provides static utility methods for common
 * reflection tasks &mdash; for example, scanning for classes in the class-path
 * or module-path, loading classes, finding methods, invoking methods, etc.
 *
 * <p>{@link org.junit.platform.engine.TestEngine TestEngine} and extension
 * authors are encouraged to use these supported methods in order to align with
 * the behavior of the JUnit Platform.
 *
 * @since 1.0
 * @see AnnotationSupport
 * @see ClassSupport
 * @see ModifierSupport
 */
@API(status = MAINTAINED, since = "1.0")
public final class ReflectionSupport {

	private ReflectionSupport() {
		/* no-op */
	}

	/**
	 * Load a class by its <em>primitive name</em> or <em>fully qualified name</em>,
	 * using the default {@link ClassLoader}.
	 *
	 * <p>Class names for arrays may be specified using either the JVM's internal
	 * String representation (e.g., {@code [[I} for {@code int[][]},
	 * {@code [Ljava.lang.String;} for {@code java.lang.String[]}, etc.) or
	 * <em>source code syntax</em> (e.g., {@code int[][]}, {@code java.lang.String[]},
	 * etc.).
	 *
	 * @param name the name of the class to load; never {@code null} or blank
	 * @return an {@code Optional} containing the loaded class; never {@code null}
	 * but potentially empty if no such class could be loaded
	 * @deprecated Please use {@link #tryToLoadClass(String)} instead.
	 */
	@API(status = DEPRECATED, since = "1.4")
	@Deprecated
	@SuppressWarnings("deprecation")
	public static Optional<Class<?>> loadClass(String name) {
		return ReflectionUtils.loadClass(name);
	}

	/**
	 * Try to load a class by its <em>primitive name</em> or <em>fully qualified name</em>,
	 * using the default {@link ClassLoader}.
	 *
	 * <p>Class names for arrays may be specified using either the JVM's internal
	 * String representation (e.g., {@code [[I} for {@code int[][]},
	 * {@code [Lava.lang.String;} for {@code java.lang.String[]}, etc.) or
	 * <em>source code syntax</em> (e.g., {@code int[][]}, {@code java.lang.String[]},
	 * etc.).
	 *
	 * @param name the name of the class to load; never {@code null} or blank
	 * @return a successful {@code Try} containing the loaded class or a failed
	 * {@code Try} containing the exception if no such class could be loaded;
	 * never {@code null}
	 * @since 1.4
	 */
	@API(status = MAINTAINED, since = "1.4")
	public static Try<Class<?>> tryToLoadClass(String name) {
		return ReflectionUtils.tryToLoadClass(name);
	}

	/**
	 * Find all {@linkplain Class classes} in the supplied classpath {@code root}
	 * that match the specified {@code classFilter} and {@code classNameFilter}
	 * predicates.
	 *
	 * <p>The classpath scanning algorithm searches recursively in subpackages
	 * beginning with the root of the classpath.
	 *
	 * @param root the URI for the classpath root in which to scan; never
	 * {@code null}
	 * @param classFilter the class type filter; never {@code null}
	 * @param classNameFilter the class name filter; never {@code null}
	 * @return an immutable list of all such classes found; never {@code null}
	 * but potentially empty
	 * @see #findAllClassesInPackage(String, Predicate, Predicate)
	 * @see #findAllClassesInModule(String, Predicate, Predicate)
	 */
	public static List<Class<?>> findAllClassesInClasspathRoot(URI root, Predicate<Class<?>> classFilter,
			Predicate<String> classNameFilter) {

		return ReflectionUtils.findAllClassesInClasspathRoot(root, classFilter, classNameFilter);
	}

	/**
	 * Find all {@linkplain Class classes} in the supplied {@code basePackageName}
	 * that match the specified {@code classFilter} and {@code classNameFilter}
	 * predicates.
	 *
	 * <p>The classpath scanning algorithm searches recursively in subpackages
	 * beginning within the supplied base package.
	 *
	 * @param basePackageName the name of the base package in which to start
	 * scanning; must not be {@code null} and must be valid in terms of Java
	 * syntax
	 * @param classFilter the class type filter; never {@code null}
	 * @param classNameFilter the class name filter; never {@code null}
	 * @return an immutable list of all such classes found; never {@code null}
	 * but potentially empty
	 * @see #findAllClassesInClasspathRoot(URI, Predicate, Predicate)
	 * @see #findAllClassesInModule(String, Predicate, Predicate)
	 */
	public static List<Class<?>> findAllClassesInPackage(String basePackageName, Predicate<Class<?>> classFilter,
			Predicate<String> classNameFilter) {

		return ReflectionUtils.findAllClassesInPackage(basePackageName, classFilter, classNameFilter);
	}

	/**
	 * Find all {@linkplain Class classes} in the supplied {@code moduleName}
	 * that match the specified {@code classFilter} and {@code classNameFilter}
	 * predicates.
	 *
	 * <p>The module-path scanning algorithm searches recursively in all
	 * packages contained in the module.
	 *
	 * @param moduleName the name of the module to scan; never {@code null} or
	 * <em>empty</em>
	 * @param classFilter the class type filter; never {@code null}
	 * @param classNameFilter the class name filter; never {@code null}
	 * @return an immutable list of all such classes found; never {@code null}
	 * but potentially empty
	 * @since 1.1.1
	 * @see #findAllClassesInClasspathRoot(URI, Predicate, Predicate)
	 * @see #findAllClassesInPackage(String, Predicate, Predicate)
	 */
	public static List<Class<?>> findAllClassesInModule(String moduleName, Predicate<Class<?>> classFilter,
			Predicate<String> classNameFilter) {

		return ReflectionUtils.findAllClassesInModule(moduleName, classFilter, classNameFilter);
	}

	/**
	 * Create a new instance of the specified {@link Class} by invoking
	 * the constructor whose argument list matches the types of the supplied
	 * arguments.
	 *
	 * <p>The constructor will be made accessible if necessary, and any checked
	 * exception will be {@linkplain ExceptionUtils#throwAsUncheckedException masked}
	 * as an unchecked exception.
	 *
	 * @param clazz the class to instantiate; never {@code null}
	 * @param args the arguments to pass to the constructor, none of which may
	 * be {@code null}
	 * @return the new instance; never {@code null}
	 * @see ExceptionUtils#throwAsUncheckedException(Throwable)
	 */
	public static <T> T newInstance(Class<T> clazz, Object... args) {
		return ReflectionUtils.newInstance(clazz, args);
	}

	/**
	 * Invoke the supplied method, making it accessible if necessary and
	 * {@linkplain ExceptionUtils#throwAsUncheckedException masking} any
	 * checked exception as an unchecked exception.
	 *
	 * @param method the method to invoke; never {@code null}
	 * @param target the object on which to invoke the method; may be
	 * {@code null} if the method is {@code static}
	 * @param args the arguments to pass to the method
	 * @return the value returned by the method invocation or {@code null}
	 * if the return type is {@code void}
	 * @see ExceptionUtils#throwAsUncheckedException(Throwable)
	 */
	public static Object invokeMethod(Method method, Object target, Object... args) {
		return ReflectionUtils.invokeMethod(method, target, args);
	}

	/**
	 * Find all {@linkplain Field fields} of the supplied class or interface
	 * that match the specified {@code predicate}.
	 *
	 * <p>Fields declared in the same class or interface will be ordered using
	 * an algorithm that is deterministic but intentionally nonobvious.
	 *
	 * <p>The results will not contain fields that are <em>hidden</em> or
	 * {@linkplain Field#isSynthetic() synthetic}.
	 *
	 * @param clazz the class or interface in which to find the fields; never {@code null}
	 * @param predicate the field filter; never {@code null}
	 * @param traversalMode the hierarchy traversal mode; never {@code null}
	 * @return an immutable list of all such fields found; never {@code null}
	 * but potentially empty
	 * @since 1.4
	 */
	@API(status = MAINTAINED, since = "1.4")
	public static List<Field> findFields(Class<?> clazz, Predicate<Field> predicate,
			HierarchyTraversalMode traversalMode) {

		Preconditions.notNull(traversalMode, "HierarchyTraversalMode must not be null");

		return ReflectionUtils.findFields(clazz, predicate,
			ReflectionUtils.HierarchyTraversalMode.valueOf(traversalMode.name()));
	}

	/**
	 * Try to read the value of a potentially inaccessible field.
	 *
	 * <p>If an exception occurs while reading the field, a failed {@link Try}
	 * is returned that contains the corresponding exception.
	 *
	 * @param field the field to read; never {@code null}
	 * @param instance the instance from which the value is to be read; may
	 * be {@code null} for a static field
	 * @since 1.4
	 */
	@API(status = MAINTAINED, since = "1.4")
	public static Try<Object> tryToReadFieldValue(Field field, Object instance) {
		return ReflectionUtils.tryToReadFieldValue(field, instance);
	}

	/**
	 * Find the first {@link Method} of the supplied class or interface that
	 * meets the specified criteria, beginning with the specified class or
	 * interface and traversing up the type hierarchy until such a method is
	 * found or the type hierarchy is exhausted.
	 *
	 * <p>The algorithm does not search for methods in {@link java.lang.Object}.
	 *
	 * @param clazz the class or interface in which to find the method; never {@code null}
	 * @param methodName the name of the method to find; never {@code null} or empty
	 * @param parameterTypeNames the fully qualified names of the types of parameters
	 * accepted by the method, if any, provided as a comma-separated list
	 * @return an {@code Optional} containing the method found; never {@code null}
	 * but potentially empty if no such method could be found
	 * @see #findMethod(Class, String, Class...)
	 */
	public static Optional<Method> findMethod(Class<?> clazz, String methodName, String parameterTypeNames) {
		return ReflectionUtils.findMethod(clazz, methodName, parameterTypeNames);
	}

	/**
	 * Find the first {@link Method} of the supplied class or interface that
	 * meets the specified criteria, beginning with the specified class or
	 * interface and traversing up the type hierarchy until such a method is
	 * found or the type hierarchy is exhausted.
	 *
	 * <p>The algorithm does not search for methods in {@link java.lang.Object}.
	 *
	 * @param clazz the class or interface in which to find the method; never {@code null}
	 * @param methodName the name of the method to find; never {@code null} or empty
	 * @param parameterTypes the types of parameters accepted by the method, if any;
	 * never {@code null}
	 * @return an {@code Optional} containing the method found; never {@code null}
	 * but potentially empty if no such method could be found
	 * @see #findMethod(Class, String, String)
	 */
	public static Optional<Method> findMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
		return ReflectionUtils.findMethod(clazz, methodName, parameterTypes);
	}

	/**
	 * Find all {@linkplain Method methods} of the supplied class or interface
	 * that match the specified {@code predicate}.
	 *
	 * <p>The results will not contain instance methods that are <em>overridden</em>
	 * or {@code static} methods that are <em>hidden</em>.
	 *
	 * <p>If you're are looking for methods annotated with a certain annotation
	 * type, consider using
	 * {@link AnnotationSupport#findAnnotatedMethods(Class, Class, HierarchyTraversalMode)}.
	 *
	 * @param clazz the class or interface in which to find the methods; never {@code null}
	 * @param predicate the method filter; never {@code null}
	 * @param traversalMode the hierarchy traversal mode; never {@code null}
	 * @return an immutable list of all such methods found; never {@code null}
	 * but potentially empty
	 */
	public static List<Method> findMethods(Class<?> clazz, Predicate<Method> predicate,
			HierarchyTraversalMode traversalMode) {

		Preconditions.notNull(traversalMode, "HierarchyTraversalMode must not be null");

		return ReflectionUtils.findMethods(clazz, predicate,
			ReflectionUtils.HierarchyTraversalMode.valueOf(traversalMode.name()));
	}

	/**
	 * Find all nested classes within the given class that conform to the given
	 * predicate.
	 *
	 * @param clazz the class to be searched; never {@code null}
	 * @param predicate the predicate against which the list of nested classes is
	 * checked; never {@code null}
	 * @return an immutable list of all such classes found; never {@code null}
	 * but potentially empty
	 */
	public static List<Class<?>> findNestedClasses(Class<?> clazz, Predicate<Class<?>> predicate) {
		return ReflectionUtils.findNestedClasses(clazz, predicate);
	}

}
