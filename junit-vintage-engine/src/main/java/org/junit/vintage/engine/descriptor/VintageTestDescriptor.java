/*
 * Copyright 2015-2019 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine.descriptor;

import static java.util.Arrays.stream;
import static java.util.function.Predicate.isEqual;
import static java.util.stream.Collectors.toList;
import static org.apiguardian.api.API.Status.INTERNAL;
import static org.junit.platform.commons.util.FunctionUtils.where;
import static org.junit.platform.commons.util.ReflectionUtils.findMethods;
import static org.junit.platform.commons.util.StringUtils.isNotBlank;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apiguardian.api.API;
import org.junit.experimental.categories.Category;
import org.junit.platform.commons.support.ModifierSupport;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.runner.Description;

/**
 * @since 4.12
 */
@API(status = INTERNAL, since = "4.12")
public class VintageTestDescriptor extends AbstractTestDescriptor {

	public static final String ENGINE_ID = "junit-vintage";
	public static final String SEGMENT_TYPE_RUNNER = "runner";
	public static final String SEGMENT_TYPE_TEST = "test";
	public static final String SEGMENT_TYPE_DYNAMIC = "dynamic";

	protected Description description;

	public VintageTestDescriptor(UniqueId uniqueId, Description description) {
		this(uniqueId, description, generateDisplayName(description), toTestSource(description));
	}

	VintageTestDescriptor(UniqueId uniqueId, Description description, String displayName, TestSource source) {
		super(uniqueId, displayName, source);
		this.description = description;
	}

	private static String generateDisplayName(Description description) {
		return description.getMethodName() != null ? description.getMethodName() : description.getDisplayName();
	}

	public Description getDescription() {
		return description;
	}

	@Override
	public String getLegacyReportingName() {
		String methodName = description.getMethodName();
		if (methodName == null) {
			String className = description.getClassName();
			if (isNotBlank(className)) {
				return className;
			}
		}
		return super.getLegacyReportingName();
	}

	@Override
	public Type getType() {
		return description.isTest() ? Type.TEST : Type.CONTAINER;
	}

	@Override
	public Set<TestTag> getTags() {
		Set<TestTag> tags = new LinkedHashSet<>();
		addTagsFromParent(tags);
		addCategoriesAsTags(tags);
		return tags;
	}

	@Override
	public void removeFromHierarchy() {
		if (canBeRemovedFromHierarchy()) {
			super.removeFromHierarchy();
		}
	}

	protected boolean canBeRemovedFromHierarchy() {
		return tryToExcludeFromRunner(this.description);
	}

	protected boolean tryToExcludeFromRunner(Description description) {
		// @formatter:off
		return getParent().map(VintageTestDescriptor.class::cast)
				.map(parent -> parent.tryToExcludeFromRunner(description))
				.orElse(false);
		// @formatter:on
	}

	void pruneDescriptorsForObsoleteDescriptions(List<Description> newSiblingDescriptions) {
		Optional<Description> newDescription = newSiblingDescriptions.stream().filter(isEqual(description)).findAny();
		if (newDescription.isPresent()) {
			List<Description> newChildren = newDescription.get().getChildren();
			new ArrayList<>(children).stream().map(VintageTestDescriptor.class::cast).forEach(
				childDescriptor -> childDescriptor.pruneDescriptorsForObsoleteDescriptions(newChildren));
		}
		else {
			super.removeFromHierarchy();
		}
	}

	private void addTagsFromParent(Set<TestTag> tags) {
		getParent().map(TestDescriptor::getTags).ifPresent(tags::addAll);
	}

	private void addCategoriesAsTags(Set<TestTag> tags) {
		Category annotation = description.getAnnotation(Category.class);
		if (annotation != null) {
			// @formatter:off
			stream(annotation.value())
					.map(ReflectionUtils::getAllAssignmentCompatibleClasses)
					.flatMap(Collection::stream)
					.distinct()
					.map(Class::getName)
					.map(TestTag::create)
					.forEachOrdered(tags::add);
			// @formatter:on
		}
	}

	private static TestSource toTestSource(Description description) {
		Class<?> testClass = description.getTestClass();
		if (testClass != null) {
			String methodName = description.getMethodName();
			if (methodName != null) {
				Method method = findMethod(testClass, methodName);
				if (method != null) {
					return MethodSource.from(testClass, method);
				}
			}
			return ClassSource.from(testClass);
		}
		return null;
	}

	private static Method findMethod(Class<?> testClass, String methodName) {
		if (methodName.contains("[") && methodName.endsWith("]")) {
			// special case for parameterized tests
			return findMethod(testClass, methodName.substring(0, methodName.indexOf("[")));
		}
		List<Method> methods = findMethods(testClass, where(Method::getName, isEqual(methodName)));
		if (methods.isEmpty()) {
			return null;
		}
		if (methods.size() == 1) {
			return methods.get(0);
		}
		methods = methods.stream().filter(ModifierSupport::isPublic).collect(toList());
		if (methods.size() == 1) {
			return methods.get(0);
		}
		return null;
	}

}
