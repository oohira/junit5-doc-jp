/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.discovery;

import static org.apiguardian.api.API.Status.INTERNAL;
import static org.junit.platform.commons.util.ReflectionUtils.findAllClassesInClasspathRoot;
import static org.junit.platform.commons.util.ReflectionUtils.findAllClassesInModule;
import static org.junit.platform.commons.util.ReflectionUtils.findAllClassesInPackage;
import static org.junit.platform.engine.support.filter.ClasspathScanningSupport.buildClassFilter;

import java.util.LinkedHashSet;
import java.util.Set;

import org.apiguardian.api.API;
import org.junit.jupiter.engine.discovery.predicates.IsTestClassWithTests;
import org.junit.platform.commons.util.ClassFilter;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.ClasspathRootSelector;
import org.junit.platform.engine.discovery.MethodSelector;
import org.junit.platform.engine.discovery.ModuleSelector;
import org.junit.platform.engine.discovery.PackageSelector;
import org.junit.platform.engine.discovery.UniqueIdSelector;

/**
 * {@code DiscoverySelectorResolver} resolves selectors with the help of a
 * {@code JavaElementsResolver}.
 *
 * <p>This class is the only public entry point into the discovery package.
 *
 * @since 5.0
 * @see JavaElementsResolver
 */
@API(status = INTERNAL, since = "5.0")
public class DiscoverySelectorResolver {

	private static final IsTestClassWithTests isTestClassWithTests = new IsTestClassWithTests();

	public void resolveSelectors(EngineDiscoveryRequest request, TestDescriptor engineDescriptor) {
		ClassFilter classFilter = buildClassFilter(request, isTestClassWithTests);
		resolve(request, engineDescriptor, classFilter);
		filter(engineDescriptor, classFilter);
		pruneTree(engineDescriptor);
	}

	private void resolve(EngineDiscoveryRequest request, TestDescriptor engineDescriptor, ClassFilter classFilter) {
		JavaElementsResolver javaElementsResolver = createJavaElementsResolver(engineDescriptor);

		request.getSelectorsByType(ClasspathRootSelector.class).forEach(selector -> {
			findAllClassesInClasspathRoot(selector.getClasspathRoot(), classFilter).forEach(
				javaElementsResolver::resolveClass);
		});
		request.getSelectorsByType(ModuleSelector.class).forEach(selector -> {
			findAllClassesInModule(selector.getModuleName(), classFilter).forEach(javaElementsResolver::resolveClass);
		});
		request.getSelectorsByType(PackageSelector.class).forEach(selector -> {
			findAllClassesInPackage(selector.getPackageName(), classFilter).forEach(javaElementsResolver::resolveClass);
		});
		request.getSelectorsByType(ClassSelector.class).forEach(selector -> {
			javaElementsResolver.resolveClass(selector.getJavaClass());
		});
		request.getSelectorsByType(MethodSelector.class).forEach(selector -> {
			javaElementsResolver.resolveMethod(selector.getJavaClass(), selector.getJavaMethod());
		});
		request.getSelectorsByType(UniqueIdSelector.class).forEach(selector -> {
			javaElementsResolver.resolveUniqueId(selector.getUniqueId());
		});
	}

	private void filter(TestDescriptor engineDescriptor, ClassFilter classFilter) {
		new DiscoveryFilterApplier().applyClassNamePredicate(classFilter::match, engineDescriptor);
	}

	private void pruneTree(TestDescriptor rootDescriptor) {
		rootDescriptor.accept(TestDescriptor::prune);
	}

	private JavaElementsResolver createJavaElementsResolver(TestDescriptor engineDescriptor) {
		Set<ElementResolver> resolvers = new LinkedHashSet<>();
		resolvers.add(new TestContainerResolver());
		resolvers.add(new NestedTestsResolver());
		resolvers.add(new TestMethodResolver());
		resolvers.add(new TestFactoryMethodResolver());
		resolvers.add(new TestTemplateMethodResolver());
		return new JavaElementsResolver(engineDescriptor, resolvers);
	}

}
