/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.testkit.engine;

import static java.util.function.Predicate.isEqual;
import static java.util.stream.Collectors.toCollection;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.junit.platform.commons.util.FunctionUtils.where;
import static org.junit.platform.engine.TestExecutionResult.Status.ABORTED;
import static org.junit.platform.engine.TestExecutionResult.Status.FAILED;
import static org.junit.platform.engine.TestExecutionResult.Status.SUCCESSFUL;
import static org.junit.platform.testkit.engine.Event.byPayload;
import static org.junit.platform.testkit.engine.Event.byTestDescriptor;
import static org.junit.platform.testkit.engine.Event.byType;
import static org.junit.platform.testkit.engine.EventType.DYNAMIC_TEST_REGISTERED;
import static org.junit.platform.testkit.engine.EventType.FINISHED;
import static org.junit.platform.testkit.engine.EventType.SKIPPED;
import static org.junit.platform.testkit.engine.EventType.STARTED;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.apiguardian.api.API;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestExecutionResult.Status;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;

/**
 * Collection of AssertJ {@linkplain Condition conditions} for {@link Event}.
 *
 * @since 1.4
 * @see TestExecutionResultConditions
 */
@API(status = EXPERIMENTAL, since = "1.4")
public final class EventConditions {

	private EventConditions() {
		/* no-op */
	}

	/**
	 * Create a new {@link Condition} that matches if and only if an
	 * {@link Event} matches all of the supplied conditions.
	 */
	@SafeVarargs
	@SuppressWarnings("varargs")
	public static Condition<Event> event(Condition<? super Event>... conditions) {
		return Assertions.allOf(conditions);
	}

	/**
	 * Create a new {@link Condition} that matches if and only if an
	 * {@link Event}'s {@linkplain Event#getTestDescriptor() test descriptor} is
	 * an instance of {@link EngineDescriptor}.
	 */
	public static Condition<Event> engine() {
		return new Condition<>(byTestDescriptor(EngineDescriptor.class::isInstance), "is an engine");
	}

	/**
	 * Create a new {@link Condition} that matches if and only if an
	 * {@link Event}'s {@linkplain Event#getTestDescriptor() test descriptor} is
	 * a {@linkplain TestDescriptor#isTest() test} and its
	 * {@linkplain TestDescriptor#getUniqueId() unique id} contains the supplied
	 * {@link String}.
	 *
	 * @see #test()
	 * @see #uniqueIdSubstring(String)
	 */
	public static Condition<Event> test(String uniqueIdSubstring) {
		return Assertions.allOf(test(), uniqueIdSubstring(uniqueIdSubstring));
	}

	/**
	 * Create a new {@link Condition} that matches if and only if an
	 * {@link Event}'s {@linkplain Event#getTestDescriptor() test descriptor} is
	 * a {@linkplain TestDescriptor#isTest() test}, its
	 * {@linkplain TestDescriptor#getUniqueId() unique id} contains the supplied
	 * {@link String}, and its {@linkplain TestDescriptor#getDisplayName()
	 * display name} equals the supplied {@link String}.
	 *
	 * @see #test()
	 * @see #uniqueIdSubstring(String)
	 * @see #displayName(String)
	 */
	public static Condition<Event> test(String uniqueIdSubstring, String displayName) {
		return Assertions.allOf(test(), uniqueIdSubstring(uniqueIdSubstring), displayName(displayName));
	}

	/**
	 * Create a new {@link Condition} that matches if and only if an
	 * {@link Event}'s {@linkplain Event#getTestDescriptor() test descriptor} is
	 * a {@linkplain TestDescriptor#isTest() test}.
	 */
	public static Condition<Event> test() {
		return new Condition<>(byTestDescriptor(TestDescriptor::isTest), "is a test");
	}

	/**
	 * Create a new {@link Condition} that matches if and only if an
	 * {@link Event}'s {@linkplain Event#getTestDescriptor() test descriptor} is
	 * a {@linkplain TestDescriptor#isContainer() container} and its
	 * {@linkplain TestDescriptor#getUniqueId() unique id} contains the
	 * fully-qualified name of the supplied {@link Class}.
	 */
	public static Condition<Event> container(Class<?> clazz) {
		return container(clazz.getName());
	}

	/**
	 * Create a new {@link Condition} that matches if and only if an
	 * {@link Event}'s {@linkplain Event#getTestDescriptor() test descriptor} is
	 * a {@linkplain TestDescriptor#isContainer() container} and its
	 * {@linkplain TestDescriptor#getUniqueId() unique id} contains the supplied
	 * {@link String}.
	 */
	public static Condition<Event> container(String uniqueIdSubstring) {
		return container(uniqueIdSubstring(uniqueIdSubstring));
	}

	/**
	 * Create a new {@link Condition} that matches if and only if an
	 * {@link Event} matches the supplied {@code Condition} and its
	 * {@linkplain Event#getTestDescriptor() test descriptor} is a
	 * {@linkplain TestDescriptor#isContainer() container}.
	 */
	public static Condition<Event> container(Condition<Event> condition) {
		return Assertions.allOf(container(), condition);
	}

	/**
	 * Create a new {@link Condition} that matches if and only if an
	 * {@link Event}'s {@linkplain Event#getTestDescriptor() test descriptor} is
	 * a {@linkplain TestDescriptor#isContainer() container}.
	 */
	public static Condition<Event> container() {
		return new Condition<>(byTestDescriptor(TestDescriptor::isContainer), "is a container");
	}

	/**
	 * Create a new {@link Condition} that matches if and only if an
	 * {@link Event}'s {@linkplain Event#getTestDescriptor() test descriptor} is
	 * a {@linkplain TestDescriptor#isContainer() container} and its
	 * {@linkplain TestDescriptor#getUniqueId() unique id} contains the
	 * simple name of the supplied {@link Class} as well as the fully-qualified
	 * name of its {@linkplain Class#getEnclosingClass() enclosing class}.
	 */
	public static Condition<Event> nestedContainer(Class<?> clazz) {
		return Assertions.allOf(container(clazz.getEnclosingClass()), uniqueIdSubstring(clazz.getSimpleName()));
	}

	/**
	 * Create a new {@link Condition} that matches if and only if an
	 * {@link Event}'s {@linkplain Event#getType() type} is
	 * {@link EventType#DYNAMIC_TEST_REGISTERED} and its
	 * {@linkplain TestDescriptor#getUniqueId() unique id} contains the
	 * supplied {@link String}.
	 */
	public static Condition<Event> dynamicTestRegistered(String uniqueIdSubstring) {
		return dynamicTestRegistered(uniqueIdSubstring(uniqueIdSubstring));
	}

	/**
	 * Create a new {@link Condition} that matches if and only if an
	 * {@link Event}'s {@linkplain Event#getType() type} is
	 * {@link EventType#DYNAMIC_TEST_REGISTERED} and it matches the supplied
	 * {@code Condition}.
	 */
	public static Condition<Event> dynamicTestRegistered(Condition<Event> condition) {
		return Assertions.allOf(type(DYNAMIC_TEST_REGISTERED), condition);
	}

	/**
	 * Create a new {@link Condition} that matches if and only if the
	 * {@linkplain TestDescriptor#getUniqueId() unique id} of an
	 * {@link Event}'s {@linkplain Event#getTestDescriptor() test descriptor}
	 * contains the supplied {@link String}.
	 */
	public static Condition<Event> uniqueIdSubstring(String uniqueIdSubstring) {
		Predicate<UniqueId.Segment> predicate = segment -> {
			String text = segment.getType() + ":" + segment.getValue();
			return text.contains(uniqueIdSubstring);
		};

		return new Condition<>(
			byTestDescriptor(
				where(TestDescriptor::getUniqueId, uniqueId -> uniqueId.getSegments().stream().anyMatch(predicate))),
			"descriptor with uniqueId substring '%s'", uniqueIdSubstring);
	}

	/**
	 * Create a new {@link Condition} that matches if and only if the
	 * {@linkplain TestDescriptor#getDisplayName() display name} of an
	 * {@link Event}'s {@linkplain Event#getTestDescriptor() test descriptor}
	 * is equal to the supplied {@link String}.
	 */
	public static Condition<Event> displayName(String displayName) {
		return new Condition<>(byTestDescriptor(where(TestDescriptor::getDisplayName, isEqual(displayName))),
			"descriptor with display name '%s'", displayName);
	}

	/**
	 * Create a new {@link Condition} that matches if and only if an
	 * {@link Event}'s {@linkplain Event#getType() type} is
	 * {@link EventType#SKIPPED} and the
	 * {@linkplain Event#getPayload() reason} is equal to the supplied
	 * {@link String}.
	 *
	 * @see #reason(String)
	 */
	public static Condition<Event> skippedWithReason(String expectedReason) {
		return Assertions.allOf(type(SKIPPED), reason(expectedReason));
	}

	/**
	 * Create a new {@link Condition} that matches if and only if an
	 * {@link Event}'s {@linkplain Event#getType() type} is
	 * {@link EventType#SKIPPED} and the
	 * {@linkplain Event#getPayload() reason} matches the supplied
	 * {@link Predicate}.
	 *
	 * @see #reason(Predicate)
	 */
	public static Condition<Event> skippedWithReason(Predicate<String> predicate) {
		return Assertions.allOf(type(SKIPPED), reason(predicate));
	}

	/**
	 * Create a new {@link Condition} that matches if and only if an
	 * {@link Event}'s {@linkplain Event#getType() type} is
	 * {@link EventType#STARTED}.
	 */
	public static Condition<Event> started() {
		return type(STARTED);
	}

	/**
	 * Create a new {@link Condition} that matches if and only if an
	 * {@link Event}'s {@linkplain Event#getType() type} is
	 * {@link EventType#FINISHED} and its
	 * {@linkplain Event#getPayload() result} has a
	 * {@linkplain TestExecutionResult#getStatus() status} of
	 * {@link TestExecutionResult.Status#ABORTED ABORTED} as well as a
	 * {@linkplain TestExecutionResult#getThrowable() cause} that matches all of
	 * the supplied {@code Conditions}.
	 */
	@SafeVarargs
	public static Condition<Event> abortedWithReason(Condition<Throwable>... conditions) {
		return finishedWithCause(ABORTED, conditions);
	}

	/**
	 * Create a new {@link Condition} that matches if and only if an
	 * {@link Event}'s {@linkplain Event#getType() type} is
	 * {@link EventType#FINISHED} and its
	 * {@linkplain Event#getPayload() result} has a
	 * {@linkplain TestExecutionResult#getStatus() status} of
	 * {@link TestExecutionResult.Status#FAILED FAILED} as well as a
	 * {@linkplain TestExecutionResult#getThrowable() cause} that matches all of
	 * the supplied {@code Conditions}.
	 */
	@SafeVarargs
	public static Condition<Event> finishedWithFailure(Condition<Throwable>... conditions) {
		return finishedWithCause(FAILED, conditions);
	}

	@SuppressWarnings("unchecked")
	private static Condition<Event> finishedWithCause(Status expectedStatus, Condition<Throwable>... conditions) {

		List<Condition<TestExecutionResult>> list = Arrays.stream(conditions)//
				.map(TestExecutionResultConditions::throwable)//
				.collect(toCollection(ArrayList::new));

		list.add(0, TestExecutionResultConditions.status(expectedStatus));

		return finished(Assertions.allOf(list));
	}

	/**
	 * Create a new {@link Condition} that matches if and only if an
	 * {@link Event}'s {@linkplain Event#getType() type} is
	 * {@link EventType#FINISHED} and its
	 * {@linkplain Event#getPayload() result} has a
	 * {@linkplain TestExecutionResult#getStatus() status} of
	 * {@link TestExecutionResult.Status#FAILED FAILED}.
	 */
	public static Condition<Event> finishedWithFailure() {
		return finished(TestExecutionResultConditions.status(FAILED));
	}

	/**
	 * Create a new {@link Condition} that matches if and only if an
	 * {@link Event}'s {@linkplain Event#getType() type} is
	 * {@link EventType#FINISHED} and its
	 * {@linkplain Event#getPayload() result} has a
	 * {@linkplain TestExecutionResult#getStatus() status} of
	 * {@link TestExecutionResult.Status#SUCCESSFUL SUCCESSFUL}.
	 */
	public static Condition<Event> finishedSuccessfully() {
		return finished(TestExecutionResultConditions.status(SUCCESSFUL));
	}

	/**
	 * Create a new {@link Condition} that matches if and only if an
	 * {@link Event}'s {@linkplain Event#getType() type} is
	 * {@link EventType#FINISHED} and its
	 * {@linkplain Event#getPayload() payload} is an instance of
	 * {@link TestExecutionResult} that matches the supplied {@code Condition}.
	 */
	public static Condition<Event> finished(Condition<TestExecutionResult> resultCondition) {
		return Assertions.allOf(type(FINISHED), result(resultCondition));
	}

	/**
	 * Create a new {@link Condition} that matches if and only if an
	 * {@link Event}'s {@linkplain Event#getType() type} is equal to the
	 * supplied {@link EventType}.
	 */
	public static Condition<Event> type(EventType expectedType) {
		return new Condition<>(byType(expectedType), "type is %s", expectedType);
	}

	/**
	 * Create a new {@link Condition} that matches if and only if an
	 * {@link Event}'s {@linkplain Event#getPayload() payload} is an instance of
	 * {@link TestExecutionResult} that matches the supplied {@code Condition}.
	 */
	public static Condition<Event> result(Condition<TestExecutionResult> condition) {
		return new Condition<>(byPayload(TestExecutionResult.class, condition::matches), "event with result where %s",
			condition);
	}

	/**
	 * Create a new {@link Condition} that matches if and only if an
	 * {@link Event}'s {@linkplain Event#getPayload() payload} is an instance of
	 * {@link String} that is equal to the supplied value.
	 */
	public static Condition<Event> reason(String expectedReason) {
		return new Condition<>(byPayload(String.class, isEqual(expectedReason)), "event with reason '%s'",
			expectedReason);
	}

	/**
	 * Create a new {@link Condition} that matches if and only if an
	 * {@link Event}'s {@linkplain Event#getPayload() payload} is an instance of
	 * {@link String} that matches the supplied {@link Predicate}.
	 */
	public static Condition<Event> reason(Predicate<String> predicate) {
		return new Condition<>(byPayload(String.class, predicate), "event with custom reason predicate");
	}

}
