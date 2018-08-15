/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params.aggregator;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.allOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.engine.test.event.ExecutionEventConditions.event;
import static org.junit.platform.engine.test.event.ExecutionEventConditions.finishedWithFailure;
import static org.junit.platform.engine.test.event.ExecutionEventConditions.test;
import static org.junit.platform.engine.test.event.TestExecutionResultConditions.isA;
import static org.junit.platform.engine.test.event.TestExecutionResultConditions.message;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ArgumentConversionException;
import org.junit.jupiter.params.converter.ArgumentConverter;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.test.event.ExecutionEvent;
import org.junit.platform.engine.test.event.ExecutionEventRecorder;

/**
 * Integration tests for {@link ArgumentsAccessor}, {@link AggregateWith},
 * and {@link ArgumentsAggregator}.
 *
 * @since 5.2
 */
public class AggregatorIntegrationTests {

	@ParameterizedTest
	@CsvSource({ "Jane, Doe, 1980-04-16, F, red", "Jack, Smith, 2000-11-22, M, blue" })
	void personAggregator(@AggregateWith(PersonAggregator.class) Person person) {
		testPersonAggregator(person);
	}

	@ParameterizedTest
	@CsvSource({ "Jane, Doe, 1980-04-16, F, red", "Jack, Smith, 2000-11-22, M, blue" })
	void personAggregatorRegisteredViaCustomAnnotation(@CsvToPerson Person person) {
		testPersonAggregator(person);
	}

	@ParameterizedTest
	@CsvSource({ //
			"42 Peachtree Street, Atlanta, 30318", //
			"99 Peachtree Road, Atlanta, 30318"//
	})
	void addressAggregator(@CsvToAddress Address address) {
		testAddressAggegator(address);
	}

	@ParameterizedTest
	@CsvSource({ //
			"Jane, Doe, 1980-04-16, F, 42 Peachtree Street, Atlanta, 30318, red", //
			"Jack, Smith, 2000-11-22, M, 99 Peachtree Road, Atlanta, 30318, blue"//
	})
	void personAggregatorAndAddressAggregator(@CsvToPerson Person person,
			@CsvToAddress @StartIndex(4) Address address) {

		testPersonAggregator(person);
		testAddressAggegator(address);
	}

	@ParameterizedTest(name = "Mixed Mode #1: {arguments}")
	@CsvSource({ //
			"gh-11111111, Jane, Doe, 1980-04-16, F, 42 Peachtree Street, Atlanta, 30318, red", //
			"gh-22222222, Jack, Smith, 2000-11-22, M, 99 Peachtree Road, Atlanta, 30318, blue"//
	})
	void mixedMode(String issueNumber, @CsvToPerson @StartIndex(1) Person person,
			@CsvToAddress @StartIndex(5) Address address, TestInfo testInfo) {

		assertThat(issueNumber).startsWith("gh-");
		testPersonAggregator(person);
		testAddressAggegator(address);
		assertThat(testInfo.getDisplayName()).startsWith("Mixed Mode #1");
	}

	@ParameterizedTest
	@CsvSource({ "cat, bird, mouse", "mouse, cat, bird", "mouse, bird, cat" })
	void mapAggregator(@AggregateWith(MapAggregator.class) Map<String, Integer> map) {
		assertThat(map).containsOnly(entry("cat", 3), entry("bird", 4), entry("mouse", 5));
	}

	@ParameterizedTest
	@CsvSource({ "1, 2, 3, 4, 5, 6, 7, 8, 9, 10" })
	void argumentsAccessor(ArgumentsAccessor arguments) {
		assertEquals(55, IntStream.range(0, arguments.size()).map(i -> arguments.getInteger(i)).sum());
	}

	@ParameterizedTest(name = "2 ArgumentsAccessors: {arguments}")
	@CsvSource({ "1, 2, 3, 4, 5, 6, 7, 8, 9, 10" })
	void argumentsAccessors(ArgumentsAccessor arguments1, ArgumentsAccessor arguments2) {
		assertArrayEquals(arguments1.toArray(), arguments2.toArray());
	}

	@ParameterizedTest(name = "ArgumentsAccessor and TestInfo: {arguments}")
	@CsvSource({ "1, 2, 3, 4, 5, 6, 7, 8, 9, 10" })
	void argumentsAccessorAndTestInfo(ArgumentsAccessor arguments, TestInfo testInfo) {
		assertEquals(55, IntStream.range(0, arguments.size()).map(i -> arguments.getInteger(i)).sum());
		assertThat(testInfo.getDisplayName()).startsWith("ArgumentsAccessor and TestInfo");
	}

	@ParameterizedTest(name = "Indexed Arguments and ArgumentsAccessor: {arguments}")
	@CsvSource({ "1, 2, 3, 4, 5, 6, 7, 8, 9, 10" })
	void indexedArgumentsAndArgumentsAccessor(int num1, int num2, ArgumentsAccessor arguments) {
		assertEquals(1, num1);
		assertEquals(2, num2);
		assertEquals(55, IntStream.range(0, arguments.size()).map(i -> arguments.getInteger(i)).sum());
	}

	@ParameterizedTest(name = "Indexed Arguments, ArgumentsAccessor, and TestInfo: {arguments}")
	@CsvSource({ "1, 2, 3, 4, 5, 6, 7, 8, 9, 10" })
	void indexedArgumentsArgumentsAccessorAndTestInfo(int num1, int num2, ArgumentsAccessor arguments,
			TestInfo testInfo) {

		assertEquals(1, num1);
		assertEquals(2, num2);
		assertEquals(55, IntStream.range(0, arguments.size()).map(i -> arguments.getInteger(i)).sum());
		assertThat(testInfo.getDisplayName()).startsWith("Indexed Arguments, ArgumentsAccessor, and TestInfo");
	}

	@ParameterizedTest(name = "Indexed Arguments, 2 ArgumentsAccessors, and TestInfo: {arguments}")
	@CsvSource({ "1, 2, 3, 4, 5, 6, 7, 8, 9, 10" })
	void indexedArgumentsArgumentsAccessorsAndTestInfo(int num1, int num2, ArgumentsAccessor arguments1,
			ArgumentsAccessor arguments2, TestInfo testInfo) {

		assertEquals(1, num1);
		assertEquals(2, num2);
		assertArrayEquals(arguments1.toArray(), arguments2.toArray());
		assertEquals(55, IntStream.range(0, arguments1.size()).map(i -> arguments1.getInteger(i)).sum());
		assertThat(testInfo.getDisplayName()).startsWith("Indexed Arguments, 2 ArgumentsAccessors, and TestInfo");
	}

	@ParameterizedTest
	@CsvSource({ "foo, bar" })
	void nullAggregator(@AggregateWith(NullAggregator.class) Person person) {
		assertNull(person);
	}

	@Test
	void reportsExceptionForErroneousAggregator() {
		List<ExecutionEvent> executionEvents = execute(
			selectMethod(ErroneousTestCases.class, "testWithErroneousAggregator", Object.class.getName()));
		assertThat(executionEvents) //
				.haveExactly(1, event(test(), finishedWithFailure(allOf(isA(ParameterResolutionException.class), //
					message("Error aggregating arguments for parameter at index 0: something went horribly wrong")))));
	}

	private void testPersonAggregator(Person person) {
		if (person.firstName.equals("Jane")) {
			assertEquals("Jane Doe", person.getFullName());
			assertEquals(1980, person.dateOfBirth.getYear());
			assertEquals(Gender.F, person.gender);
		}

		if (person.firstName.equals("Jack")) {
			assertEquals("Jack Smith", person.getFullName());
			assertEquals(2000, person.dateOfBirth.getYear());
			assertEquals(Gender.M, person.gender);
		}
	}

	private void testAddressAggegator(Address address) {
		assertThat(address.street).contains("Peachtree");
		assertEquals("Atlanta", address.city);
		assertEquals(30318, address.zipCode);
	}

	private List<ExecutionEvent> execute(DiscoverySelector... selectors) {
		return ExecutionEventRecorder.execute(new JupiterTestEngine(), request().selectors(selectors).build());
	}

	// -------------------------------------------------------------------------

	public static class Person {

		final String firstName;
		final String lastName;
		final Gender gender;
		final LocalDate dateOfBirth;

		Person(String firstName, String lastName, LocalDate dateOfBirth, Gender gender) {
			this.firstName = firstName;
			this.lastName = lastName;
			this.gender = gender;
			this.dateOfBirth = dateOfBirth;
		}

		String getFullName() {
			return this.firstName + " " + this.lastName;
		}
	}

	enum Gender {
		F, M
	}

	static class Address {

		final String street;
		final String city;
		final int zipCode;

		Address(String street, String city, int zipCode) {
			this.street = street;
			this.city = city;
			this.zipCode = zipCode;
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	@interface StartIndex {
		int value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	@AggregateWith(PersonAggregator.class)
	public @interface CsvToPerson {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	@AggregateWith(AddressAggregator.class)
	@interface CsvToAddress {
	}

	static class PersonAggregator implements ArgumentsAggregator {

		@Override
		public Person aggregateArguments(ArgumentsAccessor arguments, ParameterContext context) {
			int startIndex = context.findAnnotation(StartIndex.class).map(StartIndex::value).orElse(0);

			// @formatter:off
			return new Person(
				arguments.getString(startIndex + 0),
				arguments.getString(startIndex + 1),
				arguments.get(startIndex + 2, LocalDate.class),
				arguments.get(startIndex + 3, Gender.class)
			);
			// @formatter:on
		}
	}

	static class AddressAggregator implements ArgumentsAggregator {

		@Override
		public Address aggregateArguments(ArgumentsAccessor arguments, ParameterContext context) {
			int startIndex = context.findAnnotation(StartIndex.class).map(StartIndex::value).orElse(0);

			// @formatter:off
			return new Address(
				arguments.getString(startIndex + 0),
				arguments.getString(startIndex + 1),
				arguments.getInteger(startIndex + 2)
			);
			// @formatter:on
		}
	}

	/**
	 * Maps from String to length of String.
	 */
	static class MapAggregator implements ArgumentsAggregator {

		@Override
		public Map<String, Integer> aggregateArguments(ArgumentsAccessor arguments, ParameterContext context) {
			// @formatter:off
			return IntStream.range(0, arguments.size())
					.mapToObj(i -> arguments.getString(i))
					.collect(toMap(s -> s, String::length));
			// @formatter:on
		}
	}

	static class NullAggregator implements ArgumentsAggregator {
		@Override
		public Object aggregateArguments(ArgumentsAccessor accessor, ParameterContext context) {
			Preconditions.condition(!context.getParameter().getType().isPrimitive(),
				() -> "only supports reference types");
			return null;
		}
	}

	static class ErroneousAggregator implements ArgumentsAggregator {
		@Override
		public Object aggregateArguments(ArgumentsAccessor accessor, ParameterContext context)
				throws ArgumentsAggregationException {
			throw new ArgumentsAggregationException("something went horribly wrong");
		}
	}

	static class ErroneousTestCases {
		@ParameterizedTest
		@ValueSource(ints = 42)
		void testWithErroneousAggregator(@AggregateWith(ErroneousAggregator.class) Object ignored) {
			fail("this should never be called");
		}
	}

	@Test
	@ResourceLock("InstanceCountingConverter.instanceCount")
	void aggregatorIsInstantiatedOnlyOnce() {
		InstanceCountingAggregator.instanceCount = 0;

		execute(selectMethod(CountingTestCase.class, "testWithCountingConverterAggregator",
			int.class.getName() + "," + Object.class.getName()));

		assertThat(InstanceCountingAggregator.instanceCount).isEqualTo(1);
	}

	@Test
	@ResourceLock("InstanceCountingConverter.instanceCount")
	void converterIsInstantiatedOnlyOnce() {
		InstanceCountingConverter.instanceCount = 0;

		execute(selectMethod(CountingTestCase.class, "testWithCountingConverterAggregator",
			int.class.getName() + "," + Object.class.getName()));

		assertThat(InstanceCountingConverter.instanceCount).isEqualTo(1);
	}

	static class InstanceCountingConverter implements ArgumentConverter {
		static int instanceCount;

		InstanceCountingConverter() {
			instanceCount++;
		}

		@Override
		public Object convert(Object source, ParameterContext context) throws ArgumentConversionException {
			return source;
		}
	}

	static class InstanceCountingAggregator implements ArgumentsAggregator {
		static int instanceCount;

		InstanceCountingAggregator() {
			instanceCount++;
		}

		@Override
		public Object aggregateArguments(ArgumentsAccessor accessor, ParameterContext context)
				throws ArgumentsAggregationException {
			return null;
		}
	}

	static class CountingTestCase {
		@ParameterizedTest
		@ValueSource(ints = { 1, 2, 3 })
		void testWithCountingConverterAggregator(@ConvertWith(InstanceCountingConverter.class) int i,
				@AggregateWith(InstanceCountingAggregator.class) Object o) {
			System.out.println("noisy test(" + i + ", " + o + ")");
		}
	}
}
