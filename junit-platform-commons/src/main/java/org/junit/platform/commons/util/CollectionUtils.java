/*
 * Copyright 2015-2019 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.util;

import static java.util.Spliterator.ORDERED;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.apiguardian.api.API.Status.INTERNAL;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.apiguardian.api.API;
import org.junit.platform.commons.PreconditionViolationException;

/**
 * Collection of utilities for working with {@link Collection Collections}.
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
public final class CollectionUtils {

	private CollectionUtils() {
		/* no-op */
	}

	/**
	 * Read the only element of a collection of size 1.
	 *
	 * @param collection the collection to get the element from
	 * @return the only element of the collection
	 * @throws PreconditionViolationException if the collection is {@code null}
	 * or does not contain exactly one element
	 */
	public static <T> T getOnlyElement(Collection<T> collection) {
		Preconditions.notNull(collection, "collection must not be null");
		Preconditions.condition(collection.size() == 1,
			() -> "collection must contain exactly one element: " + collection);
		return collection.iterator().next();
	}

	/**
	 * Return a {@code Collector} that accumulates the input elements into a
	 * new unmodifiable list, in encounter order.
	 *
	 * <p>There are no guarantees on the type or serializability of the list
	 * returned, so if more control over the returned list is required,
	 * consider creating a new {@code Collector} implementation like the
	 * following:
	 *
	 * <pre class="code">
	 * public static &lt;T&gt; Collector&lt;T, ?, List&lt;T&gt;&gt; toUnmodifiableList(Supplier&lt;List&lt;T&gt;&gt; listSupplier) {
	 *     return Collectors.collectingAndThen(Collectors.toCollection(listSupplier), Collections::unmodifiableList);
	 * }
	 * </pre>
	 *
	 * @param <T> the type of the input elements
	 * @return a {@code Collector} which collects all the input elements into
	 * an unmodifiable list, in encounter order
	 */
	public static <T> Collector<T, ?, List<T>> toUnmodifiableList() {
		return collectingAndThen(toList(), Collections::unmodifiableList);
	}

	/**
	 * Convert an object of one of the following supported types into a {@code Stream}.
	 *
	 * <ul>
	 * <li>{@link Stream}</li>
	 * <li>{@link DoubleStream}</li>
	 * <li>{@link IntStream}</li>
	 * <li>{@link LongStream}</li>
	 * <li>{@link Collection}</li>
	 * <li>{@link Iterable}</li>
	 * <li>{@link Iterator}</li>
	 * <li>{@link Object} array</li>
	 * <li>primitive array</li>
	 * </ul>
	 *
	 * @param object the object to convert into a stream; never {@code null}
	 * @return the resulting stream
	 * @throws PreconditionViolationException if the supplied object is {@code null}
	 * or not one of the supported types
	 */
	public static Stream<?> toStream(Object object) {
		Preconditions.notNull(object, "Object must not be null");
		if (object instanceof Stream) {
			return (Stream<?>) object;
		}
		if (object instanceof DoubleStream) {
			return ((DoubleStream) object).boxed();
		}
		if (object instanceof IntStream) {
			return ((IntStream) object).boxed();
		}
		if (object instanceof LongStream) {
			return ((LongStream) object).boxed();
		}
		if (object instanceof Collection) {
			return ((Collection<?>) object).stream();
		}
		if (object instanceof Iterable) {
			return stream(((Iterable<?>) object).spliterator(), false);
		}
		if (object instanceof Iterator) {
			return stream(spliteratorUnknownSize((Iterator<?>) object, ORDERED), false);
		}
		if (object instanceof Object[]) {
			return Arrays.stream((Object[]) object);
		}
		if (object instanceof double[]) {
			return DoubleStream.of((double[]) object).boxed();
		}
		if (object instanceof int[]) {
			return IntStream.of((int[]) object).boxed();
		}
		if (object instanceof long[]) {
			return LongStream.of((long[]) object).boxed();
		}
		if (object.getClass().isArray() && object.getClass().getComponentType().isPrimitive()) {
			return IntStream.range(0, Array.getLength(object)).mapToObj(i -> Array.get(object, i));
		}
		throw new PreconditionViolationException(
			"Cannot convert instance of " + object.getClass().getName() + " into a Stream: " + object);
	}

}
