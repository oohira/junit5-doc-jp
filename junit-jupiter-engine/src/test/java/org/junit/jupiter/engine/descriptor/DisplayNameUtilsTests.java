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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.engine.TrackLogRecords;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.platform.commons.logging.LogRecordListener;

/**
 * Unit tests for {@link DisplayNameUtils}.
 *
 * @since 5.5
 */
class DisplayNameUtilsTests {

	@Nested
	class ClassDisplayNameTests {

		@Test
		void shouldGetDisplayNameFromDisplayNameAnnotation() {

			String displayName = DisplayNameUtils.determineDisplayName(MyTestCase.class, () -> "default-name");

			assertThat(displayName).isEqualTo("my-test-case");
		}

		@Test
		@TrackLogRecords
		void shouldGetDisplayNameFromSupplierIfNoDisplayNameAnnotationWithBlankStringPresent(
				LogRecordListener listener) {

			String displayName = DisplayNameUtils.determineDisplayName(BlankDisplayNameTestCase.class,
				() -> "default-name");

			assertThat(displayName).isEqualTo("default-name");
			assertThat(firstWarningLogRecord(listener).getMessage()).isEqualTo(
				"Configuration error: @DisplayName on [class org.junit.jupiter.engine.descriptor.DisplayNameUtilsTests$BlankDisplayNameTestCase] must be declared with a non-empty value.");
		}

		@Test
		void shouldGetDisplayNameFromSupplierIfNoDisplayNameAnnotationPresent() {

			String displayName = DisplayNameUtils.determineDisplayName(NotDisplayNameTestCase.class,
				() -> "default-name");

			assertThat(displayName).isEqualTo("default-name");
		}

		@Nested
		class ClassDisplayNameSupplierTests {

			private JupiterConfiguration configuration = mock(JupiterConfiguration.class);

			@Test
			void shouldGetDisplayNameFromDisplayNameGenerationAnnotation() {
				when(configuration.getDefaultDisplayNameGenerator()).thenReturn(new CustomDisplayNameGenerator());
				Supplier<String> displayName = DisplayNameUtils.createDisplayNameSupplierForClass(
					StandardDisplayNameTestCase.class, configuration);

				String name = StandardDisplayNameTestCase.class.getName();
				String expectedClassName = name.substring(name.lastIndexOf(".") + 1);
				assertThat(displayName.get()).isEqualTo(expectedClassName);
			}

			@Test
			void shouldGetUnderscoreDisplayNameFromDisplayNameGenerationAnnotation() {
				when(configuration.getDefaultDisplayNameGenerator()).thenReturn(new CustomDisplayNameGenerator());
				Supplier<String> displayName = DisplayNameUtils.createDisplayNameSupplierForClass(
					Underscore_DisplayName_TestCase.class, configuration);

				assertThat(displayName.get()).isEqualTo("DisplayNameUtilsTests$Underscore DisplayName TestCase");
			}

			@Test
			void shouldGetDisplayNameFromDefaultDisplayNameGenerator() {
				when(configuration.getDefaultDisplayNameGenerator()).thenReturn(new CustomDisplayNameGenerator());
				Supplier<String> displayName = DisplayNameUtils.createDisplayNameSupplierForClass(MyTestCase.class,
					configuration);

				assertThat(displayName.get()).isEqualTo("class-display-name");
			}
		}
	}

	@Nested
	class NestedClassDisplayNameTests {

		private JupiterConfiguration configuration = mock(JupiterConfiguration.class);

		@Test
		void shouldGetDisplayNameFromDisplayNameGenerationAnnotation() {
			when(configuration.getDefaultDisplayNameGenerator()).thenReturn(new CustomDisplayNameGenerator());
			Supplier<String> displayName = DisplayNameUtils.createDisplayNameSupplierForNestedClass(
				StandardDisplayNameTestCase.class, configuration);

			assertThat(displayName.get()).isEqualTo(StandardDisplayNameTestCase.class.getSimpleName());
		}

		@Test
		void shouldGetDisplayNameFromDefaultDisplayNameGenerator() {
			when(configuration.getDefaultDisplayNameGenerator()).thenReturn(new CustomDisplayNameGenerator());
			Supplier<String> displayName = DisplayNameUtils.createDisplayNameSupplierForNestedClass(
				NestedTestCase.class, configuration);

			assertThat(displayName.get()).isEqualTo("nested-class-display-name");
		}
	}

	@Nested
	class MethodDisplayNameTests {

		private JupiterConfiguration configuration = mock(JupiterConfiguration.class);

		@Test
		void shouldGetDisplayNameFromDisplayNameGenerationAnnotation() throws Exception {
			when(configuration.getDefaultDisplayNameGenerator()).thenReturn(new CustomDisplayNameGenerator());
			Method method = MyTestCase.class.getDeclaredMethod("test1");
			String displayName = DisplayNameUtils.determineDisplayNameForMethod(StandardDisplayNameTestCase.class,
				method, configuration);

			assertThat(displayName).isEqualTo("test1()");
		}

		@Test
		void shouldGetDisplayNameFromDefaultNameGenerator() throws Exception {
			Method method = MyTestCase.class.getDeclaredMethod("test1");
			when(configuration.getDefaultDisplayNameGenerator()).thenReturn(new CustomDisplayNameGenerator());

			String displayName = DisplayNameUtils.determineDisplayNameForMethod(NotDisplayNameTestCase.class, method,
				configuration);

			assertThat(displayName).isEqualTo("method-display-name");
		}
	}

	private LogRecord firstWarningLogRecord(LogRecordListener listener) throws AssertionError {
		return listener.stream(DisplayNameUtils.class, Level.WARNING).findFirst().orElseThrow(
			() -> new AssertionError("Failed to find warning log record"));
	}

	@DisplayName("my-test-case")
	@DisplayNameGeneration(value = CustomDisplayNameGenerator.class)
	static class MyTestCase {

		void test1() {
		}

	}

	@DisplayName("")
	static class BlankDisplayNameTestCase {
	}

	@DisplayNameGeneration(value = DisplayNameGenerator.Standard.class)
	static class StandardDisplayNameTestCase {
	}

	@DisplayNameGeneration(value = DisplayNameGenerator.ReplaceUnderscores.class)
	static class Underscore_DisplayName_TestCase {
	}

	static class NotDisplayNameTestCase {
	}

	@Nested
	class NestedTestCase {
	}

}
