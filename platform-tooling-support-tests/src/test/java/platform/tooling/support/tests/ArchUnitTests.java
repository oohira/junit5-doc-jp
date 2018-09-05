/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package platform.tooling.support.tests;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static platform.tooling.support.Helper.loadJarFiles;

import java.util.Set;
import java.util.stream.Collectors;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.LocationProvider;
import com.tngtech.archunit.library.GeneralCodingRules;

@AnalyzeClasses(locations = ArchUnitTests.AllJars.class)
class ArchUnitTests {

	@ArchTest
	void allAreIn(JavaClasses classes) {
		// about 928 classes found in all jars
		assertTrue(classes.size() > 800, "expected more than 800 classes, got: " + classes.size());
	}

	@ArchTest
	void freeOfCycles(JavaClasses classes) {
		slices().matching("org.junit.(*)..").should().beFreeOfCycles().check(classes);
	}

	@ArchTest
	void avoidJavaUtilLogging(JavaClasses classes) {
		// LoggerFactory.java:80 -> sets field LoggerFactory$DelegatingLogger.julLogger
		var subset = classes.that(are(not(name("org.junit.platform.commons.logging.LoggerFactory$DelegatingLogger"))));
		GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING.check(subset);
	}

	@ArchTest
	void avoidThrowingGenericExceptions(JavaClasses classes) {
		// LoggerFactory.java:155 -> new Throwable()
		var subset = classes.that(are(not(name("org.junit.platform.commons.logging.LoggerFactory$DelegatingLogger"))));
		GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS.check(subset);
	}

	@ArchTest
	void avoidAccessingStandardStreams(JavaClasses classes) {
		// ConsoleLauncher, StreamInterceptor, Picocli et al...
		var subset = classes //
				.that(are(not(name("org.junit.platform.console.ConsoleLauncher")))) //
				.that(are(not(name("org.junit.platform.launcher.core.StreamInterceptor")))) //
				.that(are(not(name("org.junit.platform.runner.JUnitPlatformRunnerListener")))) //
				.that(are(not(resideInAPackage("org.junit.platform.console.shadow.picocli")))) //
				.that(are(not(resideInAPackage("org.junit.platform.console.shadow.picocli.groovy"))));
		GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS.check(subset);
	}

	static class AllJars implements LocationProvider {

		@Override
		public Set<Location> get(Class<?> testClass) {
			return loadJarFiles().stream().map(Location::of).collect(Collectors.toSet());
		}

	}

}
