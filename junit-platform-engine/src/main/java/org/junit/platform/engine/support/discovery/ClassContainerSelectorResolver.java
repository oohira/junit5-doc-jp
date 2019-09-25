/*
 * Copyright 2015-2019 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.discovery;

import static java.util.stream.Collectors.toSet;
import static org.junit.platform.commons.support.ReflectionSupport.findAllClassesInClasspathRoot;
import static org.junit.platform.commons.support.ReflectionSupport.findAllClassesInModule;
import static org.junit.platform.commons.support.ReflectionSupport.findAllClassesInPackage;
import static org.junit.platform.engine.support.discovery.SelectorResolver.Resolution.selectors;

import java.util.List;
import java.util.function.Predicate;

import org.junit.platform.engine.discovery.ClasspathRootSelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.discovery.ModuleSelector;
import org.junit.platform.engine.discovery.PackageSelector;

/**
 * @since 1.5
 */
class ClassContainerSelectorResolver implements SelectorResolver {

	private final Predicate<Class<?>> classFilter;
	private final Predicate<String> classNameFilter;

	ClassContainerSelectorResolver(Predicate<Class<?>> classFilter, Predicate<String> classNameFilter) {
		this.classFilter = classFilter;
		this.classNameFilter = classNameFilter;
	}

	@Override
	public Resolution resolve(ClasspathRootSelector selector, Context context) {
		return classSelectors(findAllClassesInClasspathRoot(selector.getClasspathRoot(), classFilter, classNameFilter));
	}

	@Override
	public Resolution resolve(ModuleSelector selector, Context context) {
		return classSelectors(findAllClassesInModule(selector.getModuleName(), classFilter, classNameFilter));
	}

	@Override
	public Resolution resolve(PackageSelector selector, Context context) {
		return classSelectors(findAllClassesInPackage(selector.getPackageName(), classFilter, classNameFilter));
	}

	private Resolution classSelectors(List<Class<?>> classes) {
		return selectors(classes.stream().map(DiscoverySelectors::selectClass).collect(toSet()));
	}

}
