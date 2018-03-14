/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package example;

// tag::imports[]
import static org.junit.platform.engine.discovery.ClassNameFilter.includeClassNamePatterns;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;

import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
// end::imports[]

/**
 * @since 5.0
 */
class UsingTheLauncherDemo {

	@org.junit.jupiter.api.Test
	@SuppressWarnings("unused")
	void discovery() {
		// @formatter:off
		// tag::discovery[]
		LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
			.selectors(
				selectPackage("com.example.mytests"),
				selectClass(MyTestClass.class)
			)
			.filters(
				includeClassNamePatterns(".*Tests")
			)
			.build();

		Launcher launcher = LauncherFactory.create();

		TestPlan testPlan = launcher.discover(request);
		// end::discovery[]
		// @formatter:on
	}

	@org.junit.jupiter.api.Test
	void execution() {
		// @formatter:off
		// tag::execution[]
		LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
			.selectors(
				selectPackage("com.example.mytests"),
				selectClass(MyTestClass.class)
			)
			.filters(
				includeClassNamePatterns(".*Tests")
			)
			.build();

		Launcher launcher = LauncherFactory.create();

		// Register a listener of your choice
		TestExecutionListener listener = new SummaryGeneratingListener();
		launcher.registerTestExecutionListeners(listener);

		launcher.execute(request);
		// end::execution[]
		// @formatter:on
	}
}

class MyTestClass {
}
