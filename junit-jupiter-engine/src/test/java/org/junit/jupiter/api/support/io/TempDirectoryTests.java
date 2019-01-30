/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.support.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.cause;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.function.Supplier;

import com.google.common.jimfs.Jimfs;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.support.io.TempDirectory.TempDir;
import org.junit.jupiter.engine.AbstractJupiterTestEngineTests;
import org.junit.platform.testkit.engine.EngineExecutionResults;

/**
 * Integration tests for the {@link TempDirectory} extension.
 *
 * @since 5.4
 */
@DisplayName("TempDirectory extension")
class TempDirectoryTests extends AbstractJupiterTestEngineTests {

	@BeforeEach
	@AfterEach
	void resetStaticVariables() {
		BaseSharedTempDirFieldInjectionTestCase.staticTempDir = null;
		BaseSharedTempDirParameterInjectionTestCase.tempDir = null;
		BaseSeparateTempDirsFieldInjectionTestCase.tempDirs.clear();
		BaseSeparateTempDirsParameterInjectionTestCase.tempDirs.clear();
	}

	@Nested
	@DisplayName("resolves shared temp dir")
	@TestMethodOrder(OrderAnnotation.class)
	class SharedTempDir {

		@Test
		@DisplayName("when @TempDir is used on static field")
		@Order(10)
		void resolvesSharedTempDirWhenAnnotationIsUsedOnStaticField() {
			assertSharedTempDirForFieldInjection(AnnotationOnStaticFieldTestCase.class);
		}

		@Test
		@DisplayName("when @TempDir is used on static field and @BeforeAll method parameter")
		@Order(11)
		void resolvesSharedTempDirWhenAnnotationIsUsedOnStaticFieldAndBeforeAllMethodParameter() {
			assertSharedTempDirForFieldInjection(AnnotationOnStaticFieldAndBeforeAllMethodParameterTestCase.class);
		}

		@Test
		@DisplayName("when @TempDir is used on instance field and @BeforeAll method parameter")
		@Order(14)
		void resolvesSharedTempDirWhenAnnotationIsUsedOnInstanceFieldAndBeforeAllMethodParameter() {
			assertSharedTempDirForFieldInjection(AnnotationOnInstanceFieldAndBeforeAllMethodParameterTestCase.class);
		}

		@Test
		@DisplayName("when @TempDir is used on instance field and @BeforeAll method parameter with @TestInstance(PER_CLASS)")
		@Order(15)
		void resolvesSharedTempDirWhenAnnotationIsUsedOnInstanceFieldAndBeforeAllMethodParameterWithTestInstancePerClass() {
			assertSharedTempDirForFieldInjection(
				AnnotationOnInstanceFieldAndBeforeAllMethodParameterWithTestInstancePerClassTestCase.class);
		}

		@Test
		@DisplayName("when @TempDir is used on @BeforeAll method parameter")
		@Order(23)
		void resolvesSharedTempDirWhenAnnotationIsUsedOnBeforeAllMethodParameter() {
			assertSharedTempDirForParameterInjection(AnnotationOnBeforeAllMethodParameterTestCase.class);
		}

		@Test
		@DisplayName("when @TempDir is used on @BeforeAll method parameter with @TestInstance(PER_CLASS)")
		@Order(24)
		void resolvesSharedTempDirWhenAnnotationIsUsedOnBeforeAllMethodParameterWithTestInstancePerClass() {
			assertSharedTempDirForParameterInjection(
				AnnotationOnBeforeAllMethodParameterWithTestInstancePerClassTestCase.class);
		}

		private void assertSharedTempDirForFieldInjection(
				Class<? extends BaseSharedTempDirFieldInjectionTestCase> testClass) {

			assertSharedTempDirForParameterInjection(testClass,
				() -> BaseSharedTempDirFieldInjectionTestCase.staticTempDir);
		}

		private void assertSharedTempDirForParameterInjection(
				Class<? extends BaseSharedTempDirParameterInjectionTestCase> testClass) {

			assertSharedTempDirForParameterInjection(testClass,
				() -> BaseSharedTempDirParameterInjectionTestCase.tempDir);
		}

		private void assertSharedTempDirForParameterInjection(Class<?> testClass, Supplier<Path> staticTempDir) {
			var results = executeTestsForClass(testClass);

			results.tests().assertStatistics(stats -> stats.started(2).failed(0).succeeded(2));
			assertThat(staticTempDir.get()).isNotNull().doesNotExist();
		}

	}

	@Nested
	@DisplayName("resolves separate temp dirs")
	@TestMethodOrder(OrderAnnotation.class)
	class SeparateTempDirs {

		@Test
		@DisplayName("when @TempDir is used on instance field")
		@Order(11)
		void resolvesSeparateTempDirWhenAnnotationIsUsedOnInstanceField() {
			assertSeparateTempDirsForFieldInjection(
				SeparateTempDirsWhenUsedOnForEachLifecycleMethodsFieldInjectionTestCase.class);
			assertThat(BaseSeparateTempDirsFieldInjectionTestCase.tempDirs.getFirst()).doesNotExist();
			assertThat(BaseSeparateTempDirsFieldInjectionTestCase.tempDirs.getLast()).doesNotExist();
		}

		@Test
		@DisplayName("when @TempDir is used on instance field with @TestInstance(PER_CLASS)")
		@Order(12)
		void resolvesSeparateTempDirWhenAnnotationIsUsedOnInstanceFieldWithTestInstancePerClass() {
			assertSeparateTempDirsForFieldInjection(
				SeparateTempDirsWhenUsedOnForEachLifecycleMethodsWithTestInstancePerClassFieldInjectionTestCase.class);
			assertThat(BaseSeparateTempDirsFieldInjectionTestCase.tempDirs.getFirst()).doesNotExist();
			assertThat(BaseSeparateTempDirsFieldInjectionTestCase.tempDirs.getLast()).doesNotExist();
		}

		@Test
		@DisplayName("when @TempDir is used on @BeforeEach/@AfterEach method parameters")
		@Order(21)
		void resolvesSeparateTempDirsWhenUsedOnForEachLifecycleMethods() {
			assertSeparateTempDirsForParameterInjection(
				SeparateTempDirsWhenUsedOnForEachLifecycleMethodsParameterInjectionTestCase.class);
			assertThat(BaseSeparateTempDirsParameterInjectionTestCase.tempDirs.getFirst()).doesNotExist();
			assertThat(BaseSeparateTempDirsParameterInjectionTestCase.tempDirs.getLast()).doesNotExist();
		}

		@Test
		@DisplayName("when @TempDir is used on @BeforeEach/@AfterEach method parameters with @TestInstance(PER_CLASS)")
		@Order(22)
		void resolvesSeparateTempDirsWhenUsedOnForEachLifecycleMethodsWithTestInstancePerClass() {
			assertSeparateTempDirsForParameterInjection(
				SeparateTempDirsWhenUsedOnForEachLifecycleMethodsWithTestInstancePerClassParameterInjectionTestCase.class);
			assertThat(BaseSeparateTempDirsParameterInjectionTestCase.tempDirs.getFirst()).doesNotExist();
			assertThat(BaseSeparateTempDirsParameterInjectionTestCase.tempDirs.getLast()).doesNotExist();
		}

		@Test
		@DisplayName("for @AfterAll method parameter when @TempDir is not used on constructor or @BeforeAll method parameter")
		@Order(31)
		void resolvesSeparateTempDirWhenAnnotationIsUsedOnAfterAllMethodParameterOnly() {
			var results = executeTestsForClass(AnnotationOnAfterAllMethodParameterTestCase.class);

			results.tests().assertStatistics(stats -> stats.started(1).failed(0).succeeded(1));
			assertThat(AnnotationOnAfterAllMethodParameterTestCase.firstTempDir).isNotNull().doesNotExist();
			assertThat(AnnotationOnAfterAllMethodParameterTestCase.secondTempDir).isNotNull().doesNotExist();
		}

	}

	@Nested
	@DisplayName("resolves temp dir with custom parent dir")
	class WithCustomParentDir {

		@Test
		@DisplayName("from Callable<Path>")
		void resolvesTempDirWithCustomParentDirFromCallable() {
			assertSeparateTempDirsForParameterInjection(ParentDirFromCallableTestCase.class);
		}

		@Test
		@DisplayName("from ParentDirProvider")
		void resolvesTempDirWithCustomParentDirFromProvider() {
			assertSeparateTempDirsForParameterInjection(ParentDirFromProviderTestCase.class);
		}
	}

	@Nested
	@DisplayName("reports failure")
	@TestMethodOrder(OrderAnnotation.class)
	class Failures {

		@Test
		@DisplayName("when @TempDir is used on static field of an unsupported type")
		@Order(1)
		void onlySupportsStaticFieldsOfTypePathAndFile() {
			var results = executeTestsForClass(AnnotationOnStaticFieldWithUnsupportedTypeTestCase.class);

			assertSingleFailedContainer(results, ExtensionConfigurationException.class,
				"Can only resolve @TempDir field of type java.nio.file.Path or java.io.File");
		}

		@Test
		@DisplayName("when @TempDir is used on instance field of an unsupported type")
		@Order(1)
		void onlySupportsInstanceFieldsOfTypePathAndFile() {
			var results = executeTestsForClass(AnnotationOnInstanceFieldWithUnsupportedTypeTestCase.class);

			assertSingleFailedTest(results, ExtensionConfigurationException.class,
				"Can only resolve @TempDir field of type java.nio.file.Path or java.io.File");
		}

		@Test
		@DisplayName("when @TempDir is used on parameter of an unsupported type")
		@Order(2)
		void onlySupportsParametersOfTypePathAndFile() {
			var results = executeTestsForClass(InvalidTestCase.class);

			// @formatter:off
			TempDirectoryTests.assertSingleFailedTest(results,
				instanceOf(ParameterResolutionException.class),
				message(m -> m.matches("Failed to resolve parameter \\[java.lang.String .+\\] in method \\[.+\\]: .+")),
					cause(instanceOf(ExtensionConfigurationException.class)),
					cause(message("Can only resolve @TempDir parameter of type java.nio.file.Path or java.io.File but was: java.lang.String")));
			// @formatter:on
		}

		@Test
		@DisplayName("when @TempDir is used on instance field of type File with a custom FileSystem")
		@Order(3)
		void onlySupportsInstanceFieldsOfTypeFileForDefaultFileSystem() {
			var results = executeTestsForClass(InvalidFileFieldInjection.class);

			// @formatter:off
			TempDirectoryTests.assertSingleFailedTest(results,
				instanceOf(ExtensionConfigurationException.class),
				message("The configured FileSystem does not support conversion to a java.io.File; declare a java.nio.file.Path instead."),
					cause(instanceOf(UnsupportedOperationException.class))
			);
			// @formatter:on
		}

		@Test
		@DisplayName("when @TempDir is used on parameter of type File with a custom FileSystem")
		@Order(4)
		void onlySupportsParametersOfTypeFileForDefaultFileSystem() {
			var results = executeTestsForClass(InvalidFileParameterInjection.class);

			// @formatter:off
			TempDirectoryTests.assertSingleFailedTest(results,
				instanceOf(ParameterResolutionException.class),
				message(m -> m.matches("Failed to resolve parameter \\[java.io.File .+\\] in method \\[.+\\]: .+")),
					cause(instanceOf(ExtensionConfigurationException.class)),
					cause(message("The configured FileSystem does not support conversion to a java.io.File; declare a java.nio.file.Path instead.")),
						cause(cause(instanceOf(UnsupportedOperationException.class))));
			// @formatter:on
		}

		@Test
		@DisplayName("when attempt to create temp dir fails")
		@Order(5)
		void failedCreationAttemptMakesTestFail() {
			var results = executeTestsForClass(FailedCreationAttemptTestCase.class);

			// @formatter:off
			TempDirectoryTests.assertSingleFailedTest(results,
				instanceOf(ParameterResolutionException.class),
				message(m -> m.matches("Failed to resolve parameter \\[java.nio.file.Path .+\\] in method \\[.+\\]: .+")),
					cause(instanceOf(ExtensionConfigurationException.class)),
					cause(message("Failed to create custom temp directory")),
						cause(cause(instanceOf(RuntimeException.class))),
						cause(cause(message("Simulated creation failure"))));
			// @formatter:on
		}

		@Test
		@DisplayName("when attempt to delete temp dir fails")
		@Order(6)
		void failedDeletionAttemptMakesTestFail() {
			var results = executeTestsForClass(FailedDeletionAttemptTestCase.class);

			assertSingleFailedTest(results, IOException.class, "Failed to delete temp directory");
		}

		@Test
		@DisplayName("when attempt to get parent dir from ParentDirProvider fails")
		@Order(7)
		void erroneousParentDirProviderMakesTestFail() {
			var results = executeTestsForClass(ErroneousParentDirProviderTestCase.class);

			// @formatter:off
			TempDirectoryTests.assertSingleFailedTest(results,
				instanceOf(ParameterResolutionException.class),
				message(m -> m.matches("Failed to resolve parameter \\[java.nio.file.Path .+?\\] in method \\[.+?\\]: .+")),
					cause(instanceOf(ExtensionConfigurationException.class)),
					cause(message("Failed to get parent directory from provider")),
						cause(cause(instanceOf(IOException.class))),
						cause(cause(message("something went horribly wrong"))));
			// @formatter:on
		}

		@Test
		@DisplayName("when @TempDir is used on constructor parameter")
		@Order(8)
		void resolvesSharedTempDirWhenAnnotationIsUsedOnConstructorParameter() {
			var results = executeTestsForClass(AnnotationOnConstructorParameterTestCase.class);

			assertSingleFailedTest(results, ParameterResolutionException.class,
				"@TempDir is not supported on constructor parameters. Please use field injection instead.");
		}

		@Test
		@DisplayName("when @TempDir is used on constructor parameter with @TestInstance(PER_CLASS)")
		@Order(9)
		void resolvesSharedTempDirWhenAnnotationIsUsedOnConstructorParameterWithTestInstancePerClass() {
			var results = executeTestsForClass(AnnotationOnConstructorParameterWithTestInstancePerClassTestCase.class);

			assertSingleFailedContainer(results, ParameterResolutionException.class,
				"@TempDir is not supported on constructor parameters. Please use field injection instead.");
		}

	}

	private static void assertSingleFailedContainer(EngineExecutionResults results, Class<? extends Throwable> clazz,
			String message) {

		assertSingleFailedContainer(results, instanceOf(clazz), message(actual -> actual.contains(message)));
	}

	@SafeVarargs
	@SuppressWarnings("varargs")
	private static void assertSingleFailedContainer(EngineExecutionResults results,
			Condition<Throwable>... conditions) {

		results.containers().assertStatistics(stats -> stats.started(2).failed(1).succeeded(1));
		results.containers().assertThatEvents().haveExactly(1, finishedWithFailure(conditions));
	}

	private static void assertSingleFailedTest(EngineExecutionResults results, Class<? extends Throwable> clazz,
			String message) {

		assertSingleFailedTest(results, instanceOf(clazz), message(actual -> actual.contains(message)));
	}

	@SafeVarargs
	@SuppressWarnings("varargs")
	private static void assertSingleFailedTest(EngineExecutionResults results, Condition<Throwable>... conditions) {
		results.tests().assertStatistics(stats -> stats.started(1).failed(1).succeeded(0));
		results.tests().assertThatEvents().haveExactly(1, finishedWithFailure(conditions));
	}

	private void assertSeparateTempDirsForFieldInjection(
			Class<? extends BaseSeparateTempDirsFieldInjectionTestCase> testClass) {

		assertResolvesSeparateTempDirs(testClass, BaseSeparateTempDirsFieldInjectionTestCase.tempDirs);
	}

	private void assertSeparateTempDirsForParameterInjection(
			Class<? extends BaseSeparateTempDirsParameterInjectionTestCase> testClass) {

		assertResolvesSeparateTempDirs(testClass, BaseSeparateTempDirsParameterInjectionTestCase.tempDirs);
	}

	private void assertResolvesSeparateTempDirs(Class<?> testClass, Deque<Path> tempDirs) {
		var results = executeTestsForClass(testClass);

		results.tests().assertStatistics(stats -> stats.started(2).failed(0).succeeded(2));
		assertThat(tempDirs).hasSize(2);
	}

	// -------------------------------------------------------------------------

	@ExtendWith(TempDirectory.class)
	static class BaseSharedTempDirFieldInjectionTestCase {

		static Path staticTempDir;

		@TempDir
		Path tempDir;

		@BeforeEach
		void beforeEach(@TempDir Path tempDir) {
			if (BaseSharedTempDirFieldInjectionTestCase.staticTempDir != null) {
				assertSame(BaseSharedTempDirFieldInjectionTestCase.staticTempDir, tempDir);
			}
			else {
				BaseSharedTempDirFieldInjectionTestCase.staticTempDir = tempDir;
			}
			check(tempDir);
		}

		@Test
		void test1(@TempDir Path tempDir, TestInfo testInfo) throws Exception {
			check(tempDir);
			writeFile(tempDir, testInfo);
		}

		@Test
		void test2(TestInfo testInfo, @TempDir Path tempDir) throws Exception {
			check(tempDir);
			writeFile(tempDir, testInfo);
		}

		@AfterEach
		void afterEach(@TempDir Path tempDir) {
			check(tempDir);
		}

		void check(Path tempDir) {
			assertThat(BaseSharedTempDirFieldInjectionTestCase.staticTempDir)//
					.isNotNull()//
					.isSameAs(tempDir)//
					.isSameAs(this.tempDir);
			assertTrue(Files.exists(tempDir));
		}

	}

	static class AnnotationOnStaticFieldTestCase extends BaseSharedTempDirFieldInjectionTestCase {

		@TempDir
		static Path staticTempPath;

		@TempDir
		static File staticTempFile;

		@Override
		void check(Path tempDir) {
			assertThat(BaseSharedTempDirFieldInjectionTestCase.staticTempDir)//
					.isNotNull()//
					.isSameAs(AnnotationOnStaticFieldTestCase.staticTempPath)//
					.isSameAs(tempDir)//
					.isSameAs(this.tempDir);
			assertTrue(Files.exists(tempDir));
		}

	}

	static class AnnotationOnStaticFieldAndBeforeAllMethodParameterTestCase extends AnnotationOnStaticFieldTestCase {

		@BeforeAll
		static void beforeAll(@TempDir Path tempDir) {
			assertThat(BaseSharedTempDirFieldInjectionTestCase.staticTempDir).isNull();
			BaseSharedTempDirFieldInjectionTestCase.staticTempDir = tempDir;
			assertThat(AnnotationOnStaticFieldTestCase.staticTempFile).isNotNull();
			assertThat(AnnotationOnStaticFieldTestCase.staticTempPath)//
					.isNotNull()//
					.isSameAs(tempDir);
			assertThat(AnnotationOnStaticFieldTestCase.staticTempFile.toPath().toAbsolutePath())//
					.isEqualTo(AnnotationOnStaticFieldTestCase.staticTempPath.toAbsolutePath());
			assertTrue(Files.exists(tempDir));
		}

	}

	static class AnnotationOnInstanceFieldAndBeforeAllMethodParameterTestCase
			extends BaseSharedTempDirFieldInjectionTestCase {

		@BeforeAll
		static void beforeAll(@TempDir Path tempDir) {
			assertThat(BaseSharedTempDirFieldInjectionTestCase.staticTempDir).isNull();
			BaseSharedTempDirFieldInjectionTestCase.staticTempDir = tempDir;
			assertTrue(Files.exists(tempDir));
		}

	}

	@TestInstance(PER_CLASS)
	static class AnnotationOnInstanceFieldAndBeforeAllMethodParameterWithTestInstancePerClassTestCase
			extends AnnotationOnInstanceFieldAndBeforeAllMethodParameterTestCase {

	}

	@ExtendWith(TempDirectory.class)
	static class AnnotationOnStaticFieldWithUnsupportedTypeTestCase {

		@TempDir
		static String tempDir;

		@Test
		void test1() {
		}

	}

	@ExtendWith(TempDirectory.class)
	static class AnnotationOnInstanceFieldWithUnsupportedTypeTestCase {

		@TempDir
		String tempDir;

		@Test
		void test1() {
		}

	}

	@ExtendWith(TempDirectory.class)
	static class BaseSharedTempDirParameterInjectionTestCase {

		static Path tempDir;

		@BeforeEach
		void beforeEach(@TempDir Path tempDir) {
			check(tempDir);
		}

		@Test
		void test1(@TempDir Path tempDir, TestInfo testInfo) throws Exception {
			check(tempDir);
			writeFile(tempDir, testInfo);
		}

		@Test
		void test2(TestInfo testInfo, @TempDir Path tempDir) throws Exception {
			check(tempDir);
			writeFile(tempDir, testInfo);
		}

		@AfterEach
		void afterEach(@TempDir Path tempDir) {
			check(tempDir);
		}

		static void check(Path tempDir) {
			assertThat(BaseSharedTempDirParameterInjectionTestCase.tempDir).isNotNull().isSameAs(tempDir);
			assertTrue(Files.exists(tempDir));
		}

	}

	@ExtendWith(TempDirectory.class)
	static class AnnotationOnConstructorParameterTestCase {

		AnnotationOnConstructorParameterTestCase(@TempDir Path tempDir) {
			// never called
		}

		@Test
		void test() {
			// never called
		}

	}

	@TestInstance(PER_CLASS)
	static class AnnotationOnConstructorParameterWithTestInstancePerClassTestCase
			extends AnnotationOnConstructorParameterTestCase {

		AnnotationOnConstructorParameterWithTestInstancePerClassTestCase(@TempDir Path tempDir) {
			super(tempDir);
		}
	}

	static class AnnotationOnBeforeAllMethodParameterTestCase extends BaseSharedTempDirParameterInjectionTestCase {

		@BeforeAll
		static void beforeAll(@TempDir Path tempDir) {
			assertThat(BaseSharedTempDirParameterInjectionTestCase.tempDir).isNull();
			BaseSharedTempDirParameterInjectionTestCase.tempDir = tempDir;
			check(tempDir);
		}
	}

	@TestInstance(PER_CLASS)
	static class AnnotationOnBeforeAllMethodParameterWithTestInstancePerClassTestCase
			extends BaseSharedTempDirParameterInjectionTestCase {

		@BeforeAll
		void beforeAll(@TempDir Path tempDir) {
			assertThat(BaseSharedTempDirParameterInjectionTestCase.tempDir).isNull();
			BaseSharedTempDirParameterInjectionTestCase.tempDir = tempDir;
			check(tempDir);
		}
	}

	@ExtendWith(TempDirectory.class)
	static class AnnotationOnAfterAllMethodParameterTestCase {

		static Path firstTempDir = null;
		static Path secondTempDir = null;

		@Test
		void test(@TempDir Path tempDir, TestInfo testInfo) throws Exception {
			assertThat(firstTempDir).isNull();
			firstTempDir = tempDir;
			writeFile(tempDir, testInfo);
		}

		@AfterAll
		static void afterAll(@TempDir Path tempDir) {
			assertThat(firstTempDir).isNotNull();
			assertNotEquals(firstTempDir, tempDir);
			secondTempDir = tempDir;
		}
	}

	static class BaseSeparateTempDirsFieldInjectionTestCase {

		static final Deque<Path> tempDirs = new LinkedList<>();

		@TempDir
		Path tempDir;

		@BeforeEach
		void beforeEach(@TempDir Path tempDir) {
			for (Path dir : tempDirs) {
				assertThat(dir).doesNotExist();
			}
			assertThat(tempDirs).doesNotContain(tempDir);
			tempDirs.add(tempDir);
			check(tempDir);
		}

		@Test
		void test1(@TempDir Path tempDir, TestInfo testInfo) throws Exception {
			check(tempDir);
			writeFile(tempDir, testInfo);
		}

		@Test
		void test2(TestInfo testInfo, @TempDir Path tempDir) throws Exception {
			check(tempDir);
			writeFile(tempDir, testInfo);
		}

		@AfterEach
		void afterEach(@TempDir Path tempDir) {
			check(tempDir);
		}

		void check(Path tempDir) {
			assertThat(tempDirs.getLast())//
					.isNotNull()//
					.isSameAs(tempDir)//
					.isSameAs(this.tempDir);
			assertTrue(Files.exists(tempDir));
		}

	}

	@ExtendWith(TempDirectory.class)
	static class SeparateTempDirsWhenUsedOnForEachLifecycleMethodsFieldInjectionTestCase
			extends BaseSeparateTempDirsFieldInjectionTestCase {
	}

	@TestInstance(PER_CLASS)
	static class SeparateTempDirsWhenUsedOnForEachLifecycleMethodsWithTestInstancePerClassFieldInjectionTestCase
			extends SeparateTempDirsWhenUsedOnForEachLifecycleMethodsFieldInjectionTestCase {
	}

	static class BaseSeparateTempDirsParameterInjectionTestCase {

		static final Deque<Path> tempDirs = new LinkedList<>();

		@BeforeEach
		void beforeEach(@TempDir Path tempDir) {
			for (Path dir : tempDirs) {
				assertThat(dir).doesNotExist();
			}
			assertThat(tempDirs).doesNotContain(tempDir);
			tempDirs.add(tempDir);
			check(tempDir);
		}

		@Test
		void test1(@TempDir Path tempDir, TestInfo testInfo) throws Exception {
			check(tempDir);
			writeFile(tempDir, testInfo);
		}

		@Test
		void test2(TestInfo testInfo, @TempDir Path tempDir) throws Exception {
			check(tempDir);
			writeFile(tempDir, testInfo);
		}

		@AfterEach
		void afterEach(@TempDir Path tempDir) {
			check(tempDir);
		}

		void check(Path tempDir) {
			assertSame(tempDirs.getLast(), tempDir);
			assertTrue(Files.exists(tempDir));
		}
	}

	@ExtendWith(TempDirectory.class)
	static class SeparateTempDirsWhenUsedOnForEachLifecycleMethodsParameterInjectionTestCase
			extends BaseSeparateTempDirsParameterInjectionTestCase {
	}

	@TestInstance(PER_CLASS)
	static class SeparateTempDirsWhenUsedOnForEachLifecycleMethodsWithTestInstancePerClassParameterInjectionTestCase
			extends SeparateTempDirsWhenUsedOnForEachLifecycleMethodsParameterInjectionTestCase {
	}

	@ExtendWith(TempDirectory.class)
	static class InvalidTestCase {

		@Test
		void wrongParameterType(@SuppressWarnings("unused") @TempDir String ignored) {
			fail("this should never be called");
		}
	}

	@Nested
	@DisplayName("resolves java.io.File injection type")
	@ExtendWith(TempDirectory.class)
	class FileAndPathInjection {

		@TempDir
		File fileTempDir;

		@TempDir
		Path pathTempDir;

		@Test
		@DisplayName("and injected File and Path reference the same temp directory")
		void checkFile(@TempDir File tempDir, @TempDir Path ref) {
			assertFileAndPathAreEqual(tempDir, ref);
			assertFileAndPathAreEqual(this.fileTempDir, this.pathTempDir);
		}

		private void assertFileAndPathAreEqual(File tempDir, Path ref) {
			Path path = tempDir.toPath();
			assertEquals(ref.toAbsolutePath(), path.toAbsolutePath());
			assertTrue(Files.exists(path));
		}

	}

	@DisplayName("User can't rely on java.io.File parameter injection type for custom filesystems")
	static class InvalidFileParameterInjection {
		private FileSystem fileSystem;

		@BeforeEach
		void createFileSystem() {
			fileSystem = Jimfs.newFileSystem();
		}

		@AfterEach
		void closeFileSystem() throws Exception {
			fileSystem.close();
		}

		@RegisterExtension
		@SuppressWarnings("unused")
		Extension tempDirectory = TempDirectory.createInCustomDirectory(
			() -> Files.createDirectories(fileSystem.getPath("tmp")));

		@Test
		@DisplayName("File can't be injected using Jimfs FileSystem")
		void failingInjection(@TempDir File tempDir) {
			fail("this should never be called");
		}
	}

	@DisplayName("User can't rely on java.io.File field injection type for custom filesystems")
	static class InvalidFileFieldInjection {

		@TempDir
		File tempDir;

		private static FileSystem fileSystem;

		@BeforeAll
		static void createFileSystem() {
			fileSystem = Jimfs.newFileSystem();
		}

		@AfterAll
		static void closeFileSystem() throws Exception {
			fileSystem.close();
		}

		@RegisterExtension
		@SuppressWarnings("unused")
		static Extension tempDirectory = TempDirectory.createInCustomDirectory(
			() -> Files.createDirectories(fileSystem.getPath("tmp")));

		@Test
		void test() {
			fail("this should never be called");
		}
	}

	static class ParentDirFromCallableTestCase extends BaseSeparateTempDirsParameterInjectionTestCase {

		private static FileSystem fileSystem;

		@BeforeAll
		static void createFileSystem() {
			fileSystem = Jimfs.newFileSystem();
		}

		@AfterAll
		static void closeFileSystem() throws Exception {
			assertThat(tempDirs.getFirst()).doesNotExist();
			assertThat(tempDirs.getLast()).doesNotExist();
			fileSystem.close();
		}

		@RegisterExtension
		@SuppressWarnings("unused")
		Extension tempDirectory = TempDirectory.createInCustomDirectory(
			() -> Files.createDirectories(fileSystem.getPath("tmp")));

	}

	static class ParentDirFromProviderTestCase extends BaseSeparateTempDirsParameterInjectionTestCase {

		@RegisterExtension
		@SuppressWarnings("unused")
		Extension tempDirectory = TempDirectory.createInCustomDirectory((tempDirContext, extensionContext) -> {
			Store store = extensionContext.getRoot().getStore(Namespace.GLOBAL);
			FileSystem fileSystem = store.getOrComputeIfAbsent("jimfs.fileSystem", key -> new JimfsFileSystemResource(),
				JimfsFileSystemResource.class).get();
			return Files.createDirectories(fileSystem.getPath("tmp"));
		});

		static class JimfsFileSystemResource implements CloseableResource {
			private final FileSystem fileSystem;

			JimfsFileSystemResource() {
				this.fileSystem = Jimfs.newFileSystem();
			}

			FileSystem get() {
				return fileSystem;
			}

			@Override
			public void close() throws IOException {
				assertThat(tempDirs.getFirst()).doesNotExist();
				assertThat(tempDirs.getLast()).doesNotExist();
				fileSystem.close();
			}
		}
	}

	static class FailedCreationAttemptTestCase {

		private FileSystem fileSystem = mock(FileSystem.class);

		@BeforeEach
		void prepareFileSystem() {
			when(fileSystem.getPath(any())).thenAnswer(invocation -> {
				Path path = mock(Path.class, Arrays.toString(invocation.getArguments()));
				when(path.getFileSystem()).thenThrow(new RuntimeException("Simulated creation failure"));
				return path;
			});
		}

		@RegisterExtension
		@SuppressWarnings("unused")
		Extension tempDirectory = TempDirectory.createInCustomDirectory(() -> fileSystem.getPath("tmp"));

		@Test
		void test(@SuppressWarnings("unused") @TempDir Path tempDir) {
			fail("this should never be called");
		}
	}

	static class FailedDeletionAttemptTestCase {

		private FileSystem fileSystem = mock(FileSystem.class);

		@BeforeEach
		@SuppressWarnings("unchecked")
		void prepareFileSystem() throws Exception {
			FileSystemProvider provider = mock(FileSystemProvider.class);
			when(provider.readAttributes(any(), any(Class.class), any())) //
					.thenAnswer(invocation -> mock(invocation.getArgument(1)));
			doThrow(new IOException("Simulated deletion failure")).when(provider).delete(any());
			when(fileSystem.provider()).thenReturn(provider);
			when(fileSystem.getPath(any())).thenAnswer(invocation -> {
				Path path = mock(Path.class, Arrays.toString(invocation.getArguments()));
				when(path.getFileSystem()).thenReturn(fileSystem);
				when(path.toAbsolutePath()).thenReturn(path);
				when(path.resolve(any(Path.class))).thenAnswer(invocation1 -> invocation1.getArgument(0));
				when(path.toFile()).thenThrow(UnsupportedOperationException.class);
				when(path.relativize(any(Path.class))).thenAnswer(invocation1 -> invocation1.getArgument(0));
				return path;
			});
		}

		@RegisterExtension
		@SuppressWarnings("unused")
		Extension tempDirectory = TempDirectory.createInCustomDirectory(() -> fileSystem.getPath("tmp"));

		@Test
		void test(@TempDir Path tempDir) {
			assertNotNull(tempDir);
		}
	}

	static class ErroneousParentDirProviderTestCase {

		@RegisterExtension
		@SuppressWarnings("unused")
		Extension tempDirectory = TempDirectory.createInCustomDirectory(() -> {
			throw new IOException("something went horribly wrong");
		});

		@Test
		void test(@SuppressWarnings("unused") @TempDir Path tempDir) {
			fail("this should never be called");
		}
	}

	private static void writeFile(Path tempDir, TestInfo testInfo) throws IOException {
		Path file = tempDir.resolve(testInfo.getTestMethod().orElseThrow().getName() + ".txt");
		Files.write(file, testInfo.getDisplayName().getBytes());
	}

}
