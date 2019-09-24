/*
 * Copyright 2015-2019 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine.discovery;

import static org.apiguardian.api.API.Status.INTERNAL;

import org.apiguardian.api.API;
import org.junit.platform.commons.util.ClassFilter;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.junit.platform.engine.support.discovery.EngineDiscoveryRequestResolver;
import org.junit.vintage.engine.descriptor.RunnerTestDescriptor;

/**
 * @since 4.12
 */
@API(status = INTERNAL, since = "4.12")
public class VintageDiscoverer {

	private static final IsPotentialJUnit4TestClass isPotentialJUnit4TestClass = new IsPotentialJUnit4TestClass();

	// @formatter:off
	private static final EngineDiscoveryRequestResolver<TestDescriptor> resolver = EngineDiscoveryRequestResolver.builder()
			.addClassContainerSelectorResolver(isPotentialJUnit4TestClass)
			.addSelectorResolver(context -> new ClassSelectorResolver(ClassFilter.of(context.getClassNameFilter(), isPotentialJUnit4TestClass)))
			.addSelectorResolver(new MethodSelectorResolver())
			.build();
	// @formatter:on

	public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
		EngineDescriptor engineDescriptor = new EngineDescriptor(uniqueId, "JUnit Vintage");
		resolver.resolve(discoveryRequest, engineDescriptor);
		RunnerTestDescriptorPostProcessor postProcessor = new RunnerTestDescriptorPostProcessor();
		// @formatter:off
		engineDescriptor.getChildren().stream()
				.filter(RunnerTestDescriptor.class::isInstance)
				.map(RunnerTestDescriptor.class::cast)
				.forEach(postProcessor::applyFiltersAndCreateDescendants);
		// @formatter:on
		return engineDescriptor;
	}

}
