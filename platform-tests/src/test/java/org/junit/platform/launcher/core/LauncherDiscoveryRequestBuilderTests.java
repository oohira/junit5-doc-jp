/*
 * Copyright 2015-2019 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.core;

import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.FilterResult.excluded;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectModule;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;
import static org.junit.platform.launcher.EngineFilter.includeEngines;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.DiscoveryFilter;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.MethodSelector;
import org.junit.platform.engine.discovery.ModuleSelector;
import org.junit.platform.engine.discovery.PackageSelector;
import org.junit.platform.engine.discovery.UniqueIdSelector;
import org.junit.platform.fakes.TestEngineStub;
import org.junit.platform.launcher.DiscoveryFilterStub;
import org.junit.platform.launcher.EngineFilter;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.PostDiscoveryFilterStub;

/**
 * @since 1.0
 */
class LauncherDiscoveryRequestBuilderTests {

	@Nested
	class DiscoverySelectionTests {

		@Test
		void modulesAreStoredInDiscoveryRequest() {
			// @formatter:off
			LauncherDiscoveryRequest discoveryRequest = request()
					.selectors(
							selectModule("java.base")
					).build();
			// @formatter:on

			List<String> packageSelectors = discoveryRequest.getSelectorsByType(ModuleSelector.class).stream().map(
				ModuleSelector::getModuleName).collect(toList());
			assertThat(packageSelectors).contains("java.base");
		}

		@Test
		void packagesAreStoredInDiscoveryRequest() {
			// @formatter:off
			LauncherDiscoveryRequest discoveryRequest = request()
					.selectors(
							selectPackage("org.junit.platform.engine")
					).build();
			// @formatter:on

			List<String> packageSelectors = discoveryRequest.getSelectorsByType(PackageSelector.class).stream().map(
				PackageSelector::getPackageName).collect(toList());
			assertThat(packageSelectors).contains("org.junit.platform.engine");
		}

		@Test
		void classesAreStoredInDiscoveryRequest() {
			// @formatter:off
			LauncherDiscoveryRequest discoveryRequest = request()
					.selectors(
							selectClass(LauncherDiscoveryRequestBuilderTests.class.getName()),
							selectClass(SampleTestClass.class)
					)
				.build();
			// @formatter:on

			List<Class<?>> classes = discoveryRequest.getSelectorsByType(ClassSelector.class).stream().map(
				ClassSelector::getJavaClass).collect(toList());
			assertThat(classes).contains(SampleTestClass.class, LauncherDiscoveryRequestBuilderTests.class);
		}

		@Test
		void methodsByFullyQualifiedNameAreStoredInDiscoveryRequest() {
			// @formatter:off
			LauncherDiscoveryRequest discoveryRequest = request()
					.selectors(selectMethod(fullyQualifiedMethodName()))
					.build();
			// @formatter:on

			List<MethodSelector> methodSelectors = discoveryRequest.getSelectorsByType(MethodSelector.class);
			assertThat(methodSelectors).hasSize(1);

			MethodSelector methodSelector = methodSelectors.get(0);
			assertThat(methodSelector.getJavaClass()).isEqualTo(LauncherDiscoveryRequestBuilderTests.class);
			assertThat(methodSelector.getJavaMethod()).isEqualTo(fullyQualifiedMethod());
		}

		@Test
		void methodsByNameAreStoredInDiscoveryRequest() throws Exception {
			Class<?> testClass = SampleTestClass.class;
			Method testMethod = testClass.getDeclaredMethod("test");

			// @formatter:off
			LauncherDiscoveryRequest discoveryRequest = request()
					.selectors(selectMethod(SampleTestClass.class.getName(), "test"))
					.build();
			// @formatter:on

			List<MethodSelector> methodSelectors = discoveryRequest.getSelectorsByType(MethodSelector.class);
			assertThat(methodSelectors).hasSize(1);

			MethodSelector methodSelector = methodSelectors.get(0);
			assertThat(methodSelector.getJavaClass()).isEqualTo(testClass);
			assertThat(methodSelector.getJavaMethod()).isEqualTo(testMethod);
		}

		@Test
		void methodsByClassAreStoredInDiscoveryRequest() throws Exception {
			Class<?> testClass = SampleTestClass.class;
			Method testMethod = testClass.getDeclaredMethod("test");

			// @formatter:off
			DefaultDiscoveryRequest discoveryRequest = (DefaultDiscoveryRequest) request()
					.selectors(
							selectMethod(testClass, "test")
					).build();
			// @formatter:on

			List<MethodSelector> methodSelectors = discoveryRequest.getSelectorsByType(MethodSelector.class);
			assertThat(methodSelectors).hasSize(1);

			MethodSelector methodSelector = methodSelectors.get(0);
			assertThat(methodSelector.getJavaClass()).isEqualTo(testClass);
			assertThat(methodSelector.getJavaMethod()).isEqualTo(testMethod);
		}

		@Test
		void uniqueIdsAreStoredInDiscoveryRequest() {
			UniqueId id1 = UniqueId.forEngine("engine").append("foo", "id1");
			UniqueId id2 = UniqueId.forEngine("engine").append("foo", "id2");

			// @formatter:off
			LauncherDiscoveryRequest discoveryRequest = request()
					.selectors(
							selectUniqueId(id1),
							selectUniqueId(id2)
					).build();
			// @formatter:on

			List<String> uniqueIds = discoveryRequest.getSelectorsByType(UniqueIdSelector.class).stream().map(
				UniqueIdSelector::getUniqueId).map(Object::toString).collect(toList());

			assertThat(uniqueIds).contains(id1.toString(), id2.toString());
		}
	}

	@Nested
	class DiscoveryFilterTests {

		@Test
		void engineFiltersAreStoredInDiscoveryRequest() {
			TestEngine engine1 = new TestEngineStub("engine1");
			TestEngine engine2 = new TestEngineStub("engine2");
			TestEngine engine3 = new TestEngineStub("engine3");

			// @formatter:off
			LauncherDiscoveryRequest discoveryRequest = request()
					.filters(includeEngines(engine1.getId(), engine2.getId()))
					.build();
			// @formatter:on

			List<EngineFilter> filters = discoveryRequest.getEngineFilters();
			assertThat(filters).hasSize(1);
			EngineFilter engineFilter = filters.get(0);
			assertTrue(engineFilter.apply(engine1).included());
			assertTrue(engineFilter.apply(engine2).included());
			assertTrue(engineFilter.apply(engine3).excluded());
		}

		@Test
		void discoveryFiltersAreStoredInDiscoveryRequest() {
			var filter1 = new DiscoveryFilterStub<>("filter1");
			var filter2 = new DiscoveryFilterStub<>("filter2");
			// @formatter:off
			LauncherDiscoveryRequest discoveryRequest = request()
					.filters(filter1, filter2)
					.build();
			// @formatter:on

			var filters = discoveryRequest.getFiltersByType(DiscoveryFilter.class);
			assertThat(filters).containsOnly(filter1, filter2);
		}

		@Test
		void postDiscoveryFiltersAreStoredInDiscoveryRequest() {
			var postFilter1 = new PostDiscoveryFilterStub("postFilter1");
			var postFilter2 = new PostDiscoveryFilterStub("postFilter2");
			// @formatter:off
			LauncherDiscoveryRequest discoveryRequest = request()
					.filters(postFilter1, postFilter2)
					.build();
			// @formatter:on

			var filters = discoveryRequest.getPostDiscoveryFilters();
			assertThat(filters).containsOnly(postFilter1, postFilter2);
		}

		@Test
		void exceptionForIllegalFilterClass() {
			Exception exception = assertThrows(PreconditionViolationException.class,
				() -> request().filters(o -> excluded("reason")));

			assertThat(exception).hasMessageStartingWith("Filter");
			assertThat(exception).hasMessageEndingWith(
				"must implement EngineFilter, PostDiscoveryFilter, or DiscoveryFilter.");
		}
	}

	@Nested
	class DiscoveryConfigurationParameterTests {

		@Test
		void withoutConfigurationParametersSet_NoConfigurationParametersAreStoredInDiscoveryRequest() {
			LauncherDiscoveryRequest discoveryRequest = request().build();

			ConfigurationParameters configParams = discoveryRequest.getConfigurationParameters();
			assertThat(configParams.get("key")).isNotPresent();
		}

		@Test
		void configurationParameterAddedDirectly_isStoredInDiscoveryRequest() {
			// @formatter:off
			LauncherDiscoveryRequest discoveryRequest = request()
					.configurationParameter("key", "value")
					.build();
			// @formatter:on

			ConfigurationParameters configParams = discoveryRequest.getConfigurationParameters();
			assertThat(configParams.get("key")).contains("value");
		}

		@Test
		void configurationParameterAddedDirectlyTwice_overridesPreviousValueInDiscoveryRequest() {
			// @formatter:off
			LauncherDiscoveryRequest discoveryRequest = request()
					.configurationParameter("key", "value")
					.configurationParameter("key", "value-new")
					.build();
			// @formatter:on

			ConfigurationParameters configParams = discoveryRequest.getConfigurationParameters();
			assertThat(configParams.get("key")).contains("value-new");
		}

		@Test
		void multipleConfigurationParametersAddedDirectly_areStoredInDiscoveryRequest() {
			// @formatter:off
			LauncherDiscoveryRequest discoveryRequest = request()
					.configurationParameter("key1", "value1")
					.configurationParameter("key2", "value2")
					.build();
			// @formatter:on

			ConfigurationParameters configParams = discoveryRequest.getConfigurationParameters();
			assertThat(configParams.get("key1")).contains("value1");
			assertThat(configParams.get("key2")).contains("value2");
		}

		@Test
		void configurationParameterAddedByMap_isStoredInDiscoveryRequest() {
			// @formatter:off
			LauncherDiscoveryRequest discoveryRequest = request()
					.configurationParameters(singletonMap("key", "value"))
					.build();
			// @formatter:on

			ConfigurationParameters configParams = discoveryRequest.getConfigurationParameters();
			assertThat(configParams.get("key")).contains("value");
		}

		@Test
		void multipleConfigurationParametersAddedByMap_areStoredInDiscoveryRequest() {
			Map<String, String> configurationParams = new HashMap<>();
			configurationParams.put("key1", "value1");
			configurationParams.put("key2", "value2");

			// @formatter:off
			LauncherDiscoveryRequest discoveryRequest = request()
					.configurationParameters(configurationParams)
					.build();
			// @formatter:on

			ConfigurationParameters configParams = discoveryRequest.getConfigurationParameters();
			assertThat(configParams.get("key1")).contains("value1");
			assertThat(configParams.get("key2")).contains("value2");
		}
	}

	private static class SampleTestClass {

		@Test
		void test() {
		}
	}

	private static String fullyQualifiedMethodName() {
		return LauncherDiscoveryRequestBuilderTests.class.getName() + "#" + fullyQualifiedMethod().getName();
	}

	private static Method fullyQualifiedMethod() {
		try {
			return LauncherDiscoveryRequestBuilderTests.class.getDeclaredMethod("myTest");
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	void myTest() {
	}

}
