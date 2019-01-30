/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params.provider;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;
import org.junit.jupiter.params.ParameterizedTest;

/**
 * {@code @MethodSource} is an {@link ArgumentsSource} which provides access
 * to values returned from {@linkplain #value() factory methods} of the class in
 * which this annotation is declared or from static factory methods in external
 * classes referenced by <em>fully qualified method name</em>.
 *
 * <p>Each factory method must generate a <em>stream</em> of <em>arguments</em>,
 * and each set of "arguments" within the "stream" will be provided as the physical
 * arguments for individual invocations of the annotated
 * {@link ParameterizedTest @ParameterizedTest} method. Generally speaking this
 * translates to a {@link java.util.stream.Stream Stream} of {@link Arguments}
 * (i.e., {@code Stream<Arguments>}); however, the actual concrete return type
 * can take on many forms. In this context, a "stream" is anything that JUnit
 * can reliably convert into a {@code Stream}, such as
 * {@link java.util.stream.Stream Stream},
 * {@link java.util.stream.DoubleStream DoubleStream},
 * {@link java.util.stream.LongStream LongStream},
 * {@link java.util.stream.IntStream IntStream},
 * {@link java.util.Collection Collection},
 * {@link java.util.Iterator Iterator},
 * {@link Iterable}, an array of objects, or an array of primitives. Each set of
 * "arguments" within the "stream" can be supplied as an instance of
 * {@link Arguments}, an array of objects (e.g., {@code Object[]},
 * {@code String[]}, etc.), or a single <em>value</em> if the parameterized test
 * method accepts a single argument.
 *
 * <p>Please note that a one-dimensional array of objects supplied as a set of
 * "arguments" will be handled differently than other types of arguments.
 * Specifically, all of the elements of a one-dimensional array of objects will
 * be passed as individual physical arguments to the {@code @ParameterizedTest}
 * method. This behavior can be seen in the table below for the
 * {@code static Stream<Object[]> factory()} method: the {@code @ParameterizedTest}
 * method accepts individual {@code String} and {@code int} arguments rather than
 * a single {@code Object[]} array. In contrast, any multidimensional array
 * supplied as a set of "arguments" will be passed as a single physical argument
 * to the {@code @ParameterizedTest} method without modification. This behavior
 * can be seen in the table below for the {@code static Stream<int[][]> factory()}
 * and {@code static Stream<Object[][]> factory()} methods: the
 * {@code @ParameterizedTest} methods for those factories accept individual
 * {@code int[][]} and {@code Object[][]} arguments, respectively.
 *
 * <h3>Examples</h3>
 *
 * <p>The following table displays compatible method signatures for parameterized
 * test methods and their corresponding factory methods.
 *
 * <table border="1">
 * <tr><th>{@code @ParameterizedTest} method</th><th>Factory method</th></tr>
 * <tr><td>{@code void test(int)}</td><td>{@code static int[] factory()}</td></tr>
 * <tr><td>{@code void test(int)}</td><td>{@code static IntStream factory()}</td></tr>
 * <tr><td>{@code void test(String)}</td><td>{@code static String[] factory()}</td></tr>
 * <tr><td>{@code void test(String)}</td><td>{@code static List<String> factory()}</td></tr>
 * <tr><td>{@code void test(String)}</td><td>{@code static Stream<String> factory()}</td></tr>
 * <tr><td>{@code void test(String, String)}</td><td>{@code static String[][] factory()}</td></tr>
 * <tr><td>{@code void test(String, int)}</td><td>{@code static Object[][] factory()}</td></tr>
 * <tr><td>{@code void test(String, int)}</td><td>{@code static Stream<Object[]> factory()}</td></tr>
 * <tr><td>{@code void test(String, int)}</td><td>{@code static Stream<Arguments> factory()}</td></tr>
 * <tr><td>{@code void test(int[])}</td><td>{@code static int[][] factory()}</td></tr>
 * <tr><td>{@code void test(int[])}</td><td>{@code static Stream<int[]> factory()}</td></tr>
 * <tr><td>{@code void test(int[][])}</td><td>{@code static Stream<int[][]> factory()}</td></tr>
 * <tr><td>{@code void test(Object[][])}</td><td>{@code static Stream<Object[][]> factory()}</td></tr>
 * </table>
 *
 * <p>Factory methods within the test class must be {@code static} unless the
 * {@link org.junit.jupiter.api.TestInstance.Lifecycle#PER_CLASS PER_CLASS}
 * test instance lifecycle mode is used; whereas, factory methods in external
 * classes must always be {@code static}. In any case, factory methods must not
 * declare any parameters.
 *
 * @since 5.0
 * @see Arguments
 * @see ArgumentsSource
 * @see ParameterizedTest
 * @see org.junit.jupiter.api.TestInstance
 */
@Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@API(status = EXPERIMENTAL, since = "5.0")
@ArgumentsSource(MethodArgumentsProvider.class)
public @interface MethodSource {

	/**
	 * The names of factory methods within the test class or in external classes
	 * to use as sources for arguments.
	 *
	 * <p>Factory methods in external classes must be referenced by <em>fully
	 * qualified method name</em> &mdash; for example,
	 * {@code com.example.StringsProviders#blankStrings}.
	 *
	 * <p>If no factory method names are declared, a method within the test class
	 * that has the same name as the test method will be used as the factory
	 * method by default.
	 *
	 * <p>For further information, see the {@linkplain MethodSource class-level Javadoc}.
	 */
	String[] value() default "";

}
