/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.descriptor;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.descriptor.JupiterTestDescriptorTests.StaticTestCase.StaticTestCaseLevel2;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.MethodSource;

/**
 * Unit tests for {@link ClassTestDescriptor}, {@link NestedClassTestDescriptor},
 * and {@link TestMethodTestDescriptor}.
 *
 * @since 5.0
 * @see org.junit.jupiter.engine.descriptor.LifecycleMethodUtilsTests
 */
class JupiterTestDescriptorTests {

	private static final UniqueId uniqueId = UniqueId.root("enigma", "foo");

	private final JupiterConfiguration configuration = mock(JupiterConfiguration.class);

	@Test
	void constructFromClass() {
		ClassTestDescriptor descriptor = new ClassTestDescriptor(uniqueId, TestCase.class, configuration);

		assertEquals(TestCase.class, descriptor.getTestClass());
		assertThat(descriptor.getTags()).containsExactly(TestTag.create("inherited-class-level-tag"),
			TestTag.create("classTag1"), TestTag.create("classTag2"));
	}

	@Test
	void constructFromClassWithInvalidBeforeAllDeclaration() {
		// Note: if we can instantiate the descriptor, then the invalid configuration
		// will not be reported during the test engine discovery phase.
		ClassTestDescriptor descriptor = new ClassTestDescriptor(uniqueId, TestCaseWithInvalidBeforeAllMethod.class,
			configuration);

		assertEquals(TestCaseWithInvalidBeforeAllMethod.class, descriptor.getTestClass());
	}

	@Test
	void constructFromClassWithInvalidAfterAllDeclaration() {
		// Note: if we can instantiate the descriptor, then the invalid configuration
		// will not be reported during the test engine discovery phase.
		ClassTestDescriptor descriptor = new ClassTestDescriptor(uniqueId, TestCaseWithInvalidAfterAllMethod.class,
			configuration);

		assertEquals(TestCaseWithInvalidAfterAllMethod.class, descriptor.getTestClass());
	}

	@Test
	void constructFromClassWithInvalidBeforeEachDeclaration() {
		// Note: if we can instantiate the descriptor, then the invalid configuration
		// will not be reported during the test engine discovery phase.
		ClassTestDescriptor descriptor = new ClassTestDescriptor(uniqueId, TestCaseWithInvalidBeforeEachMethod.class,
			configuration);

		assertEquals(TestCaseWithInvalidBeforeEachMethod.class, descriptor.getTestClass());
	}

	@Test
	void constructFromClassWithInvalidAfterEachDeclaration() {
		// Note: if we can instantiate the descriptor, then the invalid configuration
		// will not be reported during the test engine discovery phase.
		ClassTestDescriptor descriptor = new ClassTestDescriptor(uniqueId, TestCaseWithInvalidAfterEachMethod.class,
			configuration);

		assertEquals(TestCaseWithInvalidAfterEachMethod.class, descriptor.getTestClass());
	}

	@Test
	void constructFromMethod() throws Exception {
		Class<?> testClass = TestCase.class;
		Method testMethod = testClass.getDeclaredMethod("test");
		TestMethodTestDescriptor descriptor = new TestMethodTestDescriptor(uniqueId, testClass, testMethod,
			configuration);

		assertEquals(uniqueId, descriptor.getUniqueId());
		assertEquals(testMethod, descriptor.getTestMethod());
		assertEquals("test()", descriptor.getDisplayName(), "display name:");
		assertEquals("test()", descriptor.getLegacyReportingName(), "legacy name:");
	}

	@Test
	void constructFromMethodWithAnnotations() throws Exception {
		JupiterTestDescriptor classDescriptor = new ClassTestDescriptor(uniqueId, TestCase.class, configuration);
		Method testMethod = TestCase.class.getDeclaredMethod("foo");
		TestMethodTestDescriptor methodDescriptor = new TestMethodTestDescriptor(uniqueId, TestCase.class, testMethod,
			configuration);
		classDescriptor.addChild(methodDescriptor);

		assertEquals(testMethod, methodDescriptor.getTestMethod());
		assertEquals("custom test name", methodDescriptor.getDisplayName(), "display name:");
		assertEquals("foo()", methodDescriptor.getLegacyReportingName(), "legacy name:");

		List<String> tags = methodDescriptor.getTags().stream().map(TestTag::getName).collect(toList());
		assertThat(tags).containsExactlyInAnyOrder("inherited-class-level-tag", "classTag1", "classTag2", "methodTag1",
			"methodTag2");
	}

	@Test
	void constructFromMethodWithCustomTestAnnotation() throws Exception {
		Method testMethod = TestCase.class.getDeclaredMethod("customTestAnnotation");
		TestMethodTestDescriptor descriptor = new TestMethodTestDescriptor(uniqueId, TestCase.class, testMethod,
			configuration);

		assertEquals(testMethod, descriptor.getTestMethod());
		assertEquals("custom name", descriptor.getDisplayName(), "display name:");
		assertEquals("customTestAnnotation()", descriptor.getLegacyReportingName(), "legacy name:");
		assertThat(descriptor.getTags()).containsExactly(TestTag.create("custom-tag"));
	}

	@Test
	void constructFromMethodWithParameters() throws Exception {
		Method testMethod = TestCase.class.getDeclaredMethod("test", String.class, BigDecimal.class);
		TestMethodTestDescriptor descriptor = new TestMethodTestDescriptor(uniqueId, TestCase.class, testMethod,
			configuration);

		assertEquals(testMethod, descriptor.getTestMethod());
		assertEquals("test(String, BigDecimal)", descriptor.getDisplayName(), "display name");
		assertEquals("test(String, BigDecimal)", descriptor.getLegacyReportingName(), "legacy name");
	}

	@Test
	void constructFromMethodWithPrimitiveArrayParameter() throws Exception {
		Method testMethod = TestCase.class.getDeclaredMethod("test", int[].class);
		TestMethodTestDescriptor descriptor = new TestMethodTestDescriptor(uniqueId, TestCase.class, testMethod,
			configuration);

		assertEquals(testMethod, descriptor.getTestMethod());
		assertEquals("test(int[])", descriptor.getDisplayName(), "display name");
		assertEquals("test(int[])", descriptor.getLegacyReportingName(), "legacy name");
	}

	@Test
	void constructFromMethodWithObjectArrayParameter() throws Exception {
		Method testMethod = TestCase.class.getDeclaredMethod("test", String[].class);
		TestMethodTestDescriptor descriptor = new TestMethodTestDescriptor(uniqueId, TestCase.class, testMethod,
			configuration);

		assertEquals(testMethod, descriptor.getTestMethod());
		assertEquals("test(String[])", descriptor.getDisplayName(), "display name");
		assertEquals("test(String[])", descriptor.getLegacyReportingName(), "legacy name");
	}

	@Test
	void constructFromMethodWithMultidimensionalPrimitiveArrayParameter() throws Exception {
		Method testMethod = TestCase.class.getDeclaredMethod("test", int[][][][][].class);
		TestMethodTestDescriptor descriptor = new TestMethodTestDescriptor(uniqueId, TestCase.class, testMethod,
			configuration);

		assertEquals(testMethod, descriptor.getTestMethod());
		assertEquals("test(int[][][][][])", descriptor.getDisplayName(), "display name");
		assertEquals("test(int[][][][][])", descriptor.getLegacyReportingName(), "legacy name");
	}

	@Test
	void constructFromMethodWithMultidimensionalObjectArrayParameter() throws Exception {
		Method testMethod = TestCase.class.getDeclaredMethod("test", String[][][][][].class);
		TestMethodTestDescriptor descriptor = new TestMethodTestDescriptor(uniqueId, TestCase.class, testMethod,
			configuration);

		assertEquals(testMethod, descriptor.getTestMethod());
		assertEquals("test(String[][][][][])", descriptor.getDisplayName(), "display name");
		assertEquals("test(String[][][][][])", descriptor.getLegacyReportingName(), "legacy name");
	}

	@Test
	void constructFromInheritedMethod() throws Exception {
		Method testMethod = ConcreteTest.class.getMethod("theTest");
		TestMethodTestDescriptor descriptor = new TestMethodTestDescriptor(uniqueId, ConcreteTest.class, testMethod,
			configuration);

		assertEquals(testMethod, descriptor.getTestMethod());

		Optional<TestSource> sourceOptional = descriptor.getSource();
		assertThat(sourceOptional).containsInstanceOf(MethodSource.class);

		MethodSource methodSource = (MethodSource) sourceOptional.orElseThrow();
		assertEquals(ConcreteTest.class.getName(), methodSource.getClassName());
		assertEquals("theTest", methodSource.getMethodName());
	}

	@Test
	void defaultDisplayNamesForTestClasses() {
		ClassTestDescriptor descriptor = new ClassTestDescriptor(uniqueId, getClass(), configuration);
		assertEquals(getClass().getSimpleName(), descriptor.getDisplayName());
		assertEquals(getClass().getName(), descriptor.getLegacyReportingName());

		descriptor = new NestedClassTestDescriptor(uniqueId, NestedTestCase.class, configuration);
		assertEquals(NestedTestCase.class.getSimpleName(), descriptor.getDisplayName());
		assertEquals(NestedTestCase.class.getName(), descriptor.getLegacyReportingName());

		descriptor = new ClassTestDescriptor(uniqueId, StaticTestCase.class, configuration);
		String staticDisplayName = getClass().getSimpleName() + "$" + StaticTestCase.class.getSimpleName();
		assertEquals(staticDisplayName, descriptor.getDisplayName());
		assertEquals(StaticTestCase.class.getName(), descriptor.getLegacyReportingName());

		descriptor = new ClassTestDescriptor(uniqueId, StaticTestCaseLevel2.class, configuration);
		staticDisplayName += "$" + StaticTestCaseLevel2.class.getSimpleName();
		assertEquals(staticDisplayName, descriptor.getDisplayName());
		assertEquals(StaticTestCaseLevel2.class.getName(), descriptor.getLegacyReportingName());
	}

	// -------------------------------------------------------------------------

	@Test
	@DisplayName("custom name")
	@Tag("  custom-tag  ")
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@interface CustomTestAnnotation {
	}

	@Tag("inherited-class-level-tag")
	private static abstract class AbstractTestCase {
	}

	@Tag("classTag1")
	@Tag("classTag2")
	@DisplayName("custom class name")
	@SuppressWarnings("unused")
	private static class TestCase extends AbstractTestCase {

		void test() {
		}

		void test(String txt, BigDecimal sum) {
		}

		void test(int[] nums) {
		}

		void test(int[][][][][] nums) {
		}

		void test(String[] info) {
		}

		void test(String[][][][][] info) {
		}

		@Test
		@DisplayName("custom test name")
		@Tag("methodTag1")
		@Tag("methodTag2")
		@Tag("tag containing whitespace")
		void foo() {
		}

		@CustomTestAnnotation
		void customTestAnnotation() {
		}

	}

	private static class TestCaseWithInvalidBeforeAllMethod {

		// must be static
		@BeforeAll
		void beforeAll() {
		}

		@Test
		void test() {
		}

	}

	private static class TestCaseWithInvalidAfterAllMethod {

		// must be static
		@AfterAll
		void afterAll() {
		}

		@Test
		void test() {
		}

	}

	private static class TestCaseWithInvalidBeforeEachMethod {

		// must NOT be static
		@BeforeEach
		static void beforeEach() {
		}

		@Test
		void test() {
		}

	}

	private static class TestCaseWithInvalidAfterEachMethod {

		// must NOT be static
		@AfterEach
		static void afterEach() {
		}

		@Test
		void test() {
		}

	}

	@Nested
	class NestedTestCase {
	}

	static class StaticTestCase {

		static class StaticTestCaseLevel2 {
		}
	}

	private abstract static class AbstractTestBase {

		@Test
		public void theTest() {
		}
	}

	private static class ConcreteTest extends AbstractTestBase {
	}

}
