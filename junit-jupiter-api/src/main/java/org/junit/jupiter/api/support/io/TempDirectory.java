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

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.util.stream.Collectors.joining;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotatedFields;
import static org.junit.platform.commons.util.ReflectionUtils.isPrivate;
import static org.junit.platform.commons.util.ReflectionUtils.makeAccessible;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

import org.apiguardian.api.API;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.util.ExceptionUtils;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ReflectionUtils;

/**
 * {@code TempDirectory} is a JUnit Jupiter extension that creates and cleans
 * up temporary directories.
 *
 * <p>The temporary directory is only created if a field in a test
 * class or a parameter in a test method, lifecycle method, or test class
 * constructor is annotated with {@link TempDir @TempDir}. If the field or
 * parameter type is neither {@link Path} nor {@link File} or if the temporary
 * directory could not be created, this extension will throw an
 * {@link ExtensionConfigurationException} or a
 * {@link ParameterResolutionException} as appropriate.
 *
 * <p>The scope of the temporary directory depends on where the first
 * {@code @TempDir} annotation is encountered when executing a test class. The
 * temporary directory will be shared by all tests in a class when the
 * annotation is present on a {@code static} field, on a parameter of a
 * {@link org.junit.jupiter.api.BeforeAll @BeforeAll} method, or on a parameter
 * of the test class constructor. Otherwise &mdash; for example, when only used
 * on test, {@link org.junit.jupiter.api.BeforeEach @BeforeEach}, or
 * {@link org.junit.jupiter.api.AfterEach @AfterEach} methods &mdash; each test
 * will use its own temporary directory.
 *
 * <p>When the end of the scope of a temporary directory is reached, i.e. when
 * the test method or class has finished execution, this extension will attempt
 * to recursively delete all files and directories in the temporary directory
 * and, finally, the temporary directory itself. In case deletion of a file or
 * directory fails, this extension will throw an {@link IOException} that will
 * cause the test or test class to fail.
 *
 * <p>By default, this extension will use the default
 * {@link java.nio.file.FileSystem FileSystem} to create temporary directories
 * in the default location. However, you may instantiate this extension using
 * the {@link TempDirectory#createInCustomDirectory(ParentDirProvider)} or
 * {@link TempDirectory#createInCustomDirectory(Callable)} factory methods and
 * register it via {@link org.junit.jupiter.api.extension.RegisterExtension @RegisterExtension}
 * to pass a custom provider to configure the parent directory for all temporary
 * directories created by this extension. This allows the use of this extension
 * with any third-party {@code FileSystem} implementation &mdash; for example,
 * <a href="https://github.com/google/jimfs">Jimfs</a>.
 *
 * @since 5.4
 * @see TempDir
 * @see ParentDirProvider
 * @see Files#createTempDirectory
 */
@API(status = EXPERIMENTAL, since = "5.4")
public final class TempDirectory implements BeforeAllCallback, BeforeEachCallback, ParameterResolver {

	/**
	 * {@code @TempDir} can be used to annotate a field in a test class or a
	 * parameter in a test method, lifecycle method, or test class constructor
	 * of type {@link Path} or {@link File} that should be resolved into a
	 * temporary directory.
	 *
	 * @see TempDirectory
	 */
	@Target({ ElementType.FIELD, ElementType.PARAMETER })
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	public @interface TempDir {
	}

	/**
	 * {@code TempDirContext} encapsulates the <em>context</em> in which
	 * {@link TempDir @TempDir} is declared.
	 *
	 * @see ParentDirProvider
	 */
	public interface TempDirContext {

		/**
		 * Get the {@link AnnotatedElement} associated with this context.
		 *
		 * <p>The annotated element will be the corresponding {@link Field} or
		 * {@link Parameter} on which {@link TempDir @TempDir} is declared.
		 *
		 * <p>Favor this method over more specific methods whenever the
		 * {@code AnnotatedElement} API suits the task at hand &mdash; for example,
		 * when looking up annotations regardless of concrete element type.
		 *
		 * @return the {@code AnnotatedElement}; never {@code null}
		 * @see #getField()
		 * @see #getParameterContext()
		 */
		AnnotatedElement getElement();

		/**
		 * Get the {@link Field} associated with this context, if available.
		 *
		 * <p>If this method returns an empty {@code Optional},
		 * {@link #getParameterContext()} will return a non-empty {@code Optional}.
		 *
		 * @return an {@code Optional} containing the field; never {@code null} but
		 * potentially empty
		 * @see #getElement()
		 * @see #getParameterContext()
		 */
		Optional<Field> getField();

		/**
		 * Get the {@link ParameterContext} associated with this context, if
		 * available.
		 *
		 * <p>If this method returns an empty {@code Optional},
		 * {@link #getField()} will return a non-empty {@code Optional}.
		 *
		 * @return an {@code Optional} containing the {@code ParameterContext};
		 * never {@code null} but potentially empty
		 * @see #getElement()
		 * @see #getField()
		 */
		Optional<ParameterContext> getParameterContext();

		/**
		 * Determine if an annotation of {@code annotationType} is either
		 * <em>present</em> or <em>meta-present</em> on the {@link Field} or
		 * {@link Parameter} associated with this context.
		 *
		 * <h3>WARNING</h3>
		 * <p>Favor the use of this method over directly invoking
		 * {@link Parameter#isAnnotationPresent(Class)} due to a bug in {@code javac}
		 * on JDK versions prior to JDK 9.
		 *
		 * @param annotationType the annotation type to search for; never {@code null}
		 * @return {@code true} if the annotation is present or meta-present
		 * @see #findAnnotation(Class)
		 * @see #findRepeatableAnnotations(Class)
		 */
		boolean isAnnotated(Class<? extends Annotation> annotationType);

		/**
		 * Find the first annotation of {@code annotationType} that is either
		 * <em>present</em> or <em>meta-present</em> on the {@link Field} or
		 * {@link Parameter} associated with this context.
		 *
		 * <h3>WARNING</h3>
		 * <p>Favor the use of this method over directly invoking annotation lookup
		 * methods in the {@link Parameter} API due to a bug in {@code javac} on JDK
		 * versions prior to JDK 9.
		 *
		 * @param <A> the annotation type
		 * @param annotationType the annotation type to search for; never {@code null}
		 * @return an {@code Optional} containing the annotation; never {@code null} but
		 * potentially empty
		 * @see #isAnnotated(Class)
		 * @see #findRepeatableAnnotations(Class)
		 */
		<A extends Annotation> Optional<A> findAnnotation(Class<A> annotationType);

		/**
		 * Find all <em>repeatable</em> {@linkplain Annotation annotations} of
		 * {@code annotationType} that are either <em>present</em> or
		 * <em>meta-present</em> on the {@link Field} or {@link Parameter}
		 * associated with this context.
		 *
		 * <h3>WARNING</h3>
		 * <p>Favor the use of this method over directly invoking annotation lookup
		 * methods in the {@link Parameter} API due to a bug in {@code javac} on JDK
		 * versions prior to JDK 9.
		 *
		 * @param <A> the annotation type
		 * @param annotationType the repeatable annotation type to search for; never
		 * {@code null}
		 * @return the list of all such annotations found; neither {@code null} nor
		 * mutable, but potentially empty
		 * @see #isAnnotated(Class)
		 * @see #findAnnotation(Class)
		 * @see java.lang.annotation.Repeatable
		 */
		<A extends Annotation> List<A> findRepeatableAnnotations(Class<A> annotationType);

	}

	/**
	 * {@code ParentDirProvider} can be used to configure a custom parent
	 * directory for all temporary directories created by the
	 * {@link TempDirectory} extension this is used with.
	 *
	 * @see org.junit.jupiter.api.extension.RegisterExtension
	 * @see TempDirectory#createInCustomDirectory(ParentDirProvider)
	 */
	@FunctionalInterface
	public interface ParentDirProvider {

		/**
		 * Get the parent directory for all temporary directories created by the
		 * {@link TempDirectory} extension this is used with.
		 *
		 * @param tempDirContext the context for the field or parameter for which a
		 * temporary directory should be created; never {@code null}
		 * @param extensionContext the current extension context; never {@code null}
		 * @return the parent directory for all temporary directories; never
		 * {@code null}
		 */
		Path get(TempDirContext tempDirContext, ExtensionContext extensionContext) throws Exception;
	}

	/**
	 * {@code TempDirProvider} is used internally to define how the temporary
	 * directory is created.
	 *
	 * <p>The temporary directory is by default created on the regular
	 * file system, but the user can also provide a custom file system
	 * by using the {@link ParentDirProvider}. An instance of
	 * {@code TempDirProvider} executes these (and possibly other) strategies.
	 *
	 * @see TempDirectory.ParentDirProvider
	 */
	@FunctionalInterface
	private interface TempDirProvider {

		CloseablePath get(TempDirContext tempDirContext, ExtensionContext extensionContext, String dirPrefix);
	}

	private static final Namespace NAMESPACE = Namespace.create(TempDirectory.class);
	private static final String KEY = "temp.dir";
	private static final String TEMP_DIR_PREFIX = "junit";

	private final TempDirProvider tempDirProvider;

	private TempDirectory(TempDirProvider tempDirProvider) {
		this.tempDirProvider = Preconditions.notNull(tempDirProvider, "TempDirProvider must not be null");
	}

	/**
	 * Create a new {@code TempDirectory} extension that uses the default
	 * {@link java.nio.file.FileSystem FileSystem} and creates temporary
	 * directories in the default location.
	 *
	 * <p>This constructor is used by the JUnit Jupiter Engine when the
	 * extension is registered via
	 * {@link org.junit.jupiter.api.extension.ExtendWith @ExtendWith}.
	 */
	public TempDirectory() {
		this((__, ___, dirPrefix) -> createDefaultTempDir(dirPrefix));
	}

	/**
	 * Create a {@code TempDirectory} extension that uses the default
	 * {@link java.nio.file.FileSystem FileSystem} and creates temporary
	 * directories in the default location.
	 *
	 * <p>You may use this factory method when registering this extension via
	 * {@link org.junit.jupiter.api.extension.RegisterExtension @RegisterExtension},
	 * although you might prefer the simpler registration via
	 * {@link org.junit.jupiter.api.extension.ExtendWith @ExtendWith}.
	 *
	 * @return a {@code TempDirectory} extension; never {@code null}
	 */
	public static TempDirectory createInDefaultDirectory() {
		return new TempDirectory();
	}

	/**
	 * Create a {@code TempDirectory} extension that uses the supplied
	 * {@link ParentDirProvider} to configure the parent directory for the
	 * temporary directories created by this extension.
	 *
	 * <p>You may use this factory method when registering this extension via
	 * {@link org.junit.jupiter.api.extension.RegisterExtension @RegisterExtension}.
	 *
	 * @param parentDirProvider a {@code ParentDirProvider} used to configure the
	 * parent directory for the temporary directories created by this extension;
	 * never {@code null}
	 * @return a {@code TempDirectory} extension; never {@code null}
	 */
	public static TempDirectory createInCustomDirectory(ParentDirProvider parentDirProvider) {
		Preconditions.notNull(parentDirProvider, "ParentDirProvider must not be null");

		// @formatter:off
		return new TempDirectory((tempDirContext, extensionContext, dirPrefix) ->
				createCustomTempDir(parentDirProvider, tempDirContext, extensionContext, dirPrefix));
		// @formatter:on
	}

	/**
	 * Returns a {@code TempDirectory} extension that uses the supplied
	 * {@link Callable} to configure the parent directory for the temporary
	 * directories created by this extension.
	 *
	 * <p>You may use this factory method when registering this extension via
	 * {@link org.junit.jupiter.api.extension.RegisterExtension @RegisterExtension}.
	 *
	 * @param parentDirProvider a {@code Callable} that returns a {@code Path}
	 * used to configure the parent directory for the temporary directories
	 * created by this extension; never {@code null}
	 * @return a {@code TempDirectory} extension; never {@code null}
	 */
	public static TempDirectory createInCustomDirectory(Callable<Path> parentDirProvider) {
		Preconditions.notNull(parentDirProvider, "parentDirProvider must not be null");
		return createInCustomDirectory((tempDirContext, extensionContext) -> parentDirProvider.call());
	}

	/**
	 * Perform field injection for non-private, {@code static} fields (i.e.,
	 * class fields) of type {@link Path} or {@link File} that are annotated with
	 * {@link TempDir @TempDir}.
	 */
	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		injectFields(context, null, ReflectionUtils::isStatic);
	}

	/**
	 * Perform field injection for non-private, non-static fields (i.e.,
	 * instance fields) of type {@link Path} or {@link File} that are annotated
	 * with {@link TempDir @TempDir}.
	 */
	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		injectFields(context, context.getRequiredTestInstance(), ReflectionUtils::isNotStatic);
	}

	private void injectFields(ExtensionContext context, Object testInstance, Predicate<Field> predicate) {
		findAnnotatedFields(context.getRequiredTestClass(), TempDir.class, predicate).forEach(field -> {
			assertValidFieldCandidate(field);
			try {
				makeAccessible(field).set(testInstance,
					getPathOrFile(field.getType(), DefaultTempDirContext.from(field), context));
			}
			catch (Throwable t) {
				ExceptionUtils.throwAsUncheckedException(t);
			}
		});
	}

	private void assertValidFieldCandidate(Field field) {
		assertSupportedType("field", field.getType());
		if (isPrivate(field)) {
			throw new ExtensionConfigurationException("@TempDir field [" + field + "] must not be private.");
		}
	}

	/**
	 * Determine if the {@link Parameter} in the supplied {@link ParameterContext}
	 * is annotated with {@link TempDir @TempDir}.
	 */
	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		if (parameterContext.getDeclaringExecutable() instanceof Constructor) {
			throw new ParameterResolutionException(
				"@TempDir is not supported on constructor parameters. Please use field injection instead.");
		}
		return parameterContext.isAnnotated(TempDir.class);
	}

	/**
	 * Resolve the current temporary directory for the {@link Parameter} in the
	 * supplied {@link ParameterContext}.
	 */
	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		Class<?> parameterType = parameterContext.getParameter().getType();
		assertSupportedType("parameter", parameterType);
		return getPathOrFile(parameterType, DefaultTempDirContext.from(parameterContext), extensionContext);
	}

	private void assertSupportedType(String target, Class<?> type) {
		if (type != Path.class && type != File.class) {
			throw new ExtensionConfigurationException("Can only resolve @TempDir " + target + " of type "
					+ Path.class.getName() + " or " + File.class.getName() + " but was: " + type.getName());
		}
	}

	private Object getPathOrFile(Class<?> type, TempDirContext tempDirContext, ExtensionContext extensionContext) {
		Path path = extensionContext.getStore(NAMESPACE) //
				.getOrComputeIfAbsent(KEY,
					key -> tempDirProvider.get(tempDirContext, extensionContext, TEMP_DIR_PREFIX), //
					CloseablePath.class) //
				.get();

		if (type == Path.class) {
			return path;
		}
		try {
			return path.toFile();
		}
		catch (UnsupportedOperationException ex) { // not default filesystem
			String message = String.format(
				"The configured FileSystem does not support conversion to a %s; declare a %s instead.",
				File.class.getName(), Path.class.getName());
			throw new ExtensionConfigurationException(message, ex);
		}
	}

	private static CloseablePath createDefaultTempDir(String dirPrefix) {
		try {
			return new CloseablePath(Files.createTempDirectory(dirPrefix));
		}
		catch (Exception ex) {
			throw new ExtensionConfigurationException("Failed to create default temp directory", ex);
		}
	}

	private static CloseablePath createCustomTempDir(ParentDirProvider parentDirProvider, TempDirContext tempDirContext,
			ExtensionContext extensionContext, String dirPrefix) {

		Path parentDir;
		try {
			parentDir = parentDirProvider.get(tempDirContext, extensionContext);
			Preconditions.notNull(parentDir, "ParentDirProvider returned null for the parent directory");
		}
		catch (Exception ex) {
			throw new ExtensionConfigurationException("Failed to get parent directory from provider", ex);
		}
		try {
			return new CloseablePath(Files.createTempDirectory(parentDir, dirPrefix));
		}
		catch (Exception ex) {
			throw new ExtensionConfigurationException("Failed to create custom temp directory", ex);
		}
	}

	private static class CloseablePath implements CloseableResource {

		private final Path dir;

		CloseablePath(Path dir) {
			this.dir = dir;
		}

		Path get() {
			return dir;
		}

		@Override
		public void close() throws IOException {
			SortedMap<Path, IOException> failures = deleteAllFilesAndDirectories();
			if (!failures.isEmpty()) {
				throw createIOExceptionWithAttachedFailures(failures);
			}
		}

		private SortedMap<Path, IOException> deleteAllFilesAndDirectories() throws IOException {
			SortedMap<Path, IOException> failures = new TreeMap<>();
			Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
					return deleteAndContinue(file);
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
					return deleteAndContinue(dir);
				}

				private FileVisitResult deleteAndContinue(Path path) {
					try {
						Files.delete(path);
					}
					catch (IOException ex) {
						failures.put(path, ex);
					}
					return CONTINUE;
				}
			});
			return failures;
		}

		private IOException createIOExceptionWithAttachedFailures(SortedMap<Path, IOException> failures) {
			// @formatter:off
			String joinedPaths = failures.keySet().stream()
					.peek(this::tryToDeleteOnExit)
					.map(this::relativizeSafely)
					.map(String::valueOf)
					.collect(joining(", "));
			// @formatter:on
			IOException exception = new IOException("Failed to delete temp directory " + dir.toAbsolutePath()
					+ ". The following paths could not be deleted (see suppressed exceptions for details): "
					+ joinedPaths);
			failures.values().forEach(exception::addSuppressed);
			return exception;
		}

		private void tryToDeleteOnExit(Path path) {
			try {
				path.toFile().deleteOnExit();
			}
			catch (UnsupportedOperationException ignore) {
			}
		}

		private Path relativizeSafely(Path path) {
			try {
				return dir.relativize(path);
			}
			catch (IllegalArgumentException e) {
				return path;
			}
		}
	}

}
