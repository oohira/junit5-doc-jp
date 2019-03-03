/*
 * Copyright 2015-2019 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.junit.jupiter.params.provider.EnumSource.Mode.MATCH_ALL;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import example.domain.Person;
import example.domain.Person.Gender;
import example.util.StringUtils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.AggregateWith;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.aggregator.ArgumentsAggregator;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.converter.JavaTimeConversionPattern;
import org.junit.jupiter.params.converter.SimpleArgumentConverter;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class ParameterizedTestDemo {

	// tag::first_example[]
	@ParameterizedTest
	@ValueSource(strings = { "racecar", "radar", "able was I ere I saw elba" })
	void palindromes(String candidate) {
		assertTrue(StringUtils.isPalindrome(candidate));
	}
	// end::first_example[]

	// tag::ValueSource_example[]
	@ParameterizedTest
	@ValueSource(ints = { 1, 2, 3 })
	void testWithValueSource(int argument) {
		assertTrue(argument > 0 && argument < 4);
	}
	// end::ValueSource_example[]

	@Nested
	class NullAndEmptySource_1 {

		// tag::NullAndEmptySource_example1[]
		@ParameterizedTest
		@NullSource
		@EmptySource
		@ValueSource(strings = { " ", "   ", "\t", "\n" })
		void nullEmptyAndBlankStrings(String text) {
			assertTrue(text == null || text.trim().isEmpty());
		}
		// end::NullAndEmptySource_example1[]
	}

	@Nested
	class NullAndEmptySource_2 {

		// tag::NullAndEmptySource_example2[]
		@ParameterizedTest
		@NullAndEmptySource
		@ValueSource(strings = { " ", "   ", "\t", "\n" })
		void nullEmptyAndBlankStrings(String text) {
			assertTrue(text == null || text.trim().isEmpty());
		}
		// end::NullAndEmptySource_example2[]
	}

	// tag::EnumSource_example[]
	@ParameterizedTest
	@EnumSource(TimeUnit.class)
	void testWithEnumSource(TimeUnit timeUnit) {
		assertNotNull(timeUnit);
	}
	// end::EnumSource_example[]

	// tag::EnumSource_include_example[]
	@ParameterizedTest
	@EnumSource(value = TimeUnit.class, names = { "DAYS", "HOURS" })
	void testWithEnumSourceInclude(TimeUnit timeUnit) {
		assertTrue(EnumSet.of(TimeUnit.DAYS, TimeUnit.HOURS).contains(timeUnit));
	}
	// end::EnumSource_include_example[]

	// tag::EnumSource_exclude_example[]
	@ParameterizedTest
	@EnumSource(value = TimeUnit.class, mode = EXCLUDE, names = { "DAYS", "HOURS" })
	void testWithEnumSourceExclude(TimeUnit timeUnit) {
		assertFalse(EnumSet.of(TimeUnit.DAYS, TimeUnit.HOURS).contains(timeUnit));
		assertTrue(timeUnit.name().length() > 5);
	}
	// end::EnumSource_exclude_example[]

	// tag::EnumSource_regex_example[]
	@ParameterizedTest
	@EnumSource(value = TimeUnit.class, mode = MATCH_ALL, names = "^(M|N).+SECONDS$")
	void testWithEnumSourceRegex(TimeUnit timeUnit) {
		String name = timeUnit.name();
		assertTrue(name.startsWith("M") || name.startsWith("N"));
		assertTrue(name.endsWith("SECONDS"));
	}
	// end::EnumSource_regex_example[]

	// tag::simple_MethodSource_example[]
	@ParameterizedTest
	@MethodSource("stringProvider")
	void testWithExplicitLocalMethodSource(String argument) {
		assertNotNull(argument);
	}

	static Stream<String> stringProvider() {
		return Stream.of("apple", "banana");
	}
	// end::simple_MethodSource_example[]

	// tag::simple_MethodSource_without_value_example[]
	@ParameterizedTest
	@MethodSource
	void testWithDefaultLocalMethodSource(String argument) {
		assertNotNull(argument);
	}

	static Stream<String> testWithDefaultLocalMethodSource() {
		return Stream.of("apple", "banana");
	}
	// end::simple_MethodSource_without_value_example[]

	// tag::primitive_MethodSource_example[]
	@ParameterizedTest
	@MethodSource("range")
	void testWithRangeMethodSource(int argument) {
		assertNotEquals(9, argument);
	}

	static IntStream range() {
		return IntStream.range(0, 20).skip(10);
	}
	// end::primitive_MethodSource_example[]

	// @formatter:off
	// tag::multi_arg_MethodSource_example[]
	@ParameterizedTest
	@MethodSource("stringIntAndListProvider")
	void testWithMultiArgMethodSource(String str, int num, List<String> list) {
		assertEquals(5, str.length());
		assertTrue(num >=1 && num <=2);
		assertEquals(2, list.size());
	}

	static Stream<Arguments> stringIntAndListProvider() {
		return Stream.of(
			arguments("apple", 1, Arrays.asList("a", "b")),
			arguments("lemon", 2, Arrays.asList("x", "y"))
		);
	}
	// end::multi_arg_MethodSource_example[]
	// @formatter:on

	// @formatter:off
	// tag::CsvSource_example[]
	@ParameterizedTest
	@CsvSource({
		"apple,         1",
		"banana,        2",
		"'lemon, lime', 0xF1"
	})
	void testWithCsvSource(String fruit, int rank) {
		assertNotNull(fruit);
		assertNotEquals(0, rank);
	}
	// end::CsvSource_example[]
	// @formatter:on

	// tag::CsvFileSource_example[]
	@ParameterizedTest
	@CsvFileSource(resources = "/two-column.csv", numLinesToSkip = 1)
	void testWithCsvFileSource(String country, int reference) {
		assertNotNull(country);
		assertNotEquals(0, reference);
	}
	// end::CsvFileSource_example[]

	// tag::ArgumentsSource_example[]
	@ParameterizedTest
	@ArgumentsSource(MyArgumentsProvider.class)
	void testWithArgumentsSource(String argument) {
		assertNotNull(argument);
	}

	// end::ArgumentsSource_example[]
	static
	// tag::ArgumentsProvider_example[]
	public class MyArgumentsProvider implements ArgumentsProvider {

		@Override
		public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
			return Stream.of("apple", "banana").map(Arguments::of);
		}
	}
	// end::ArgumentsProvider_example[]

	// tag::ParameterResolver_example[]
	@BeforeEach
	void beforeEach(TestInfo testInfo) {
		// ...
	}

	@ParameterizedTest
	@ValueSource(strings = "apple")
	void testWithRegularParameterResolver(String argument, TestReporter testReporter) {
		testReporter.publishEntry("argument", argument);
	}

	@AfterEach
	void afterEach(TestInfo testInfo) {
		// ...
	}
	// end::ParameterResolver_example[]

	// tag::implicit_conversion_example[]
	@ParameterizedTest
	@ValueSource(strings = "SECONDS")
	void testWithImplicitArgumentConversion(TimeUnit argument) {
		assertNotNull(argument.name());
	}
	// end::implicit_conversion_example[]

	// tag::implicit_fallback_conversion_example[]
	@ParameterizedTest
	@ValueSource(strings = "42 Cats")
	void testWithImplicitFallbackArgumentConversion(Book book) {
		assertEquals("42 Cats", book.getTitle());
	}

	// end::implicit_fallback_conversion_example[]
	static
	// tag::implicit_fallback_conversion_example_Book[]
	public class Book {

		private final String title;

		private Book(String title) {
			this.title = title;
		}

		public static Book fromTitle(String title) {
			return new Book(title);
		}

		public String getTitle() {
			return this.title;
		}
	}
	// end::implicit_fallback_conversion_example_Book[]

	// @formatter:off
	// tag::explicit_conversion_example[]
	@ParameterizedTest
	@EnumSource(TimeUnit.class)
	void testWithExplicitArgumentConversion(
			@ConvertWith(ToStringArgumentConverter.class) String argument) {

		assertNotNull(TimeUnit.valueOf(argument));
	}

	// end::explicit_conversion_example[]
	static
	// tag::explicit_conversion_example_ToStringArgumentConverter[]
	public class ToStringArgumentConverter extends SimpleArgumentConverter {

		@Override
		protected Object convert(Object source, Class<?> targetType) {
			assertEquals(String.class, targetType, "Can only convert to String");
			return String.valueOf(source);
		}
	}
	// end::explicit_conversion_example_ToStringArgumentConverter[]

	// tag::explicit_java_time_converter[]
	@ParameterizedTest
	@ValueSource(strings = { "01.01.2017", "31.12.2017" })
	void testWithExplicitJavaTimeConverter(
			@JavaTimeConversionPattern("dd.MM.yyyy") LocalDate argument) {

		assertEquals(2017, argument.getYear());
	}
	// end::explicit_java_time_converter[]
	// @formatter:on

	// @formatter:off
    // tag::ArgumentsAccessor_example[]
    @ParameterizedTest
    @CsvSource({
        "Jane, Doe, F, 1990-05-20",
        "John, Doe, M, 1990-10-22"
    })
    void testWithArgumentsAccessor(ArgumentsAccessor arguments) {
        Person person = new Person(arguments.getString(0),
                                   arguments.getString(1),
                                   arguments.get(2, Gender.class),
                                   arguments.get(3, LocalDate.class));

        if (person.getFirstName().equals("Jane")) {
            assertEquals(Gender.F, person.getGender());
        }
        else {
            assertEquals(Gender.M, person.getGender());
        }
        assertEquals("Doe", person.getLastName());
        assertEquals(1990, person.getDateOfBirth().getYear());
    }
    // end::ArgumentsAccessor_example[]
	// @formatter:on

	// @formatter:off
    // tag::ArgumentsAggregator_example[]
    @ParameterizedTest
    @CsvSource({
        "Jane, Doe, F, 1990-05-20",
        "John, Doe, M, 1990-10-22"
    })
    void testWithArgumentsAggregator(@AggregateWith(PersonAggregator.class) Person person) {
        // perform assertions against person
    }

    // end::ArgumentsAggregator_example[]
    static
    // tag::ArgumentsAggregator_example_PersonAggregator[]
    public class PersonAggregator implements ArgumentsAggregator {
        @Override
        public Person aggregateArguments(ArgumentsAccessor arguments, ParameterContext context) {
            return new Person(arguments.getString(0),
                              arguments.getString(1),
                              arguments.get(2, Gender.class),
                              arguments.get(3, LocalDate.class));
        }
    }
    // end::ArgumentsAggregator_example_PersonAggregator[]
	// @formatter:on

	// @formatter:off
    // tag::ArgumentsAggregator_with_custom_annotation_example[]
    @ParameterizedTest
    @CsvSource({
        "Jane, Doe, F, 1990-05-20",
        "John, Doe, M, 1990-10-22"
    })
    void testWithCustomAggregatorAnnotation(@CsvToPerson Person person) {
        // perform assertions against person
    }
    // end::ArgumentsAggregator_with_custom_annotation_example[]

    // tag::ArgumentsAggregator_with_custom_annotation_example_CsvToPerson[]
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    @AggregateWith(PersonAggregator.class)
    public @interface CsvToPerson {
    }
    // end::ArgumentsAggregator_with_custom_annotation_example_CsvToPerson[]
	// @formatter:on

	// tag::custom_display_names[]
	@DisplayName("Display name of container")
	@ParameterizedTest(name = "{index} ==> fruit=''{0}'', rank={1}")
	@CsvSource({ "apple, 1", "banana, 2", "'lemon, lime', 3" })
	void testWithCustomDisplayNames(String fruit, int rank) {
	}
	// end::custom_display_names[]
}
