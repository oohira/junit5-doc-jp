/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.migrationsupport.rules;

import static java.util.Collections.unmodifiableList;
import static org.junit.platform.commons.util.AnnotationUtils.findPublicAnnotatedFields;
import static org.junit.platform.commons.util.AnnotationUtils.isAnnotated;
import static org.junit.platform.commons.util.ReflectionUtils.findMethods;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.Rule;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.migrationsupport.rules.adapter.AbstractTestRuleAdapter;
import org.junit.jupiter.migrationsupport.rules.adapter.GenericBeforeAndAfterAdvice;
import org.junit.jupiter.migrationsupport.rules.member.TestRuleAnnotatedField;
import org.junit.jupiter.migrationsupport.rules.member.TestRuleAnnotatedMember;
import org.junit.jupiter.migrationsupport.rules.member.TestRuleAnnotatedMethod;
import org.junit.platform.commons.util.ExceptionUtils;
import org.junit.rules.TestRule;

/**
 * @since 5.0
 */
class TestRuleSupport implements BeforeEachCallback, TestExecutionExceptionHandler, AfterEachCallback {

	private static final Consumer<List<TestRuleAnnotatedMember>> NO_OP = members -> {
	};

	private final Class<? extends TestRule> ruleType;
	private final Function<TestRuleAnnotatedMember, AbstractTestRuleAdapter> adapterGenerator;

	TestRuleSupport(Function<TestRuleAnnotatedMember, AbstractTestRuleAdapter> adapterGenerator,
			Class<? extends TestRule> ruleType) {

		this.adapterGenerator = adapterGenerator;
		this.ruleType = ruleType;
	}

	/**
	 * @see org.junit.runners.BlockJUnit4ClassRunner#withRules
	 * @see org.junit.rules.RunRules
	 */
	private List<TestRuleAnnotatedMember> findRuleAnnotatedMembers(Object testInstance) {
		List<TestRuleAnnotatedMember> result = new ArrayList<>();
		// @formatter:off
		// Instantiate rules from methods by calling them
		findAnnotatedMethods(testInstance).stream()
				.map(method -> new TestRuleAnnotatedMethod(testInstance, method))
				.forEach(result::add);
		// Fields are already instantiated because we have a test instance
		findAnnotatedFields(testInstance).stream()
				.map(field -> new TestRuleAnnotatedField(testInstance, field))
				.forEach(result::add);
		// @formatter:on
		// Due to how rules are applied (see RunRules), the last rule gets called first.
		// Rules from fields get called before those from methods.
		// Thus, we first add methods and then fields and reverse the list in the end.
		Collections.reverse(result);
		return unmodifiableList(result);
	}

	private List<Method> findAnnotatedMethods(Object testInstance) {
		Predicate<Method> isRuleMethod = method -> isAnnotated(method, Rule.class);
		Predicate<Method> hasCorrectReturnType = method -> TestRule.class.isAssignableFrom(method.getReturnType());

		return findMethods(testInstance.getClass(), isRuleMethod.and(hasCorrectReturnType));
	}

	private List<Field> findAnnotatedFields(Object testInstance) {
		return findPublicAnnotatedFields(testInstance.getClass(), TestRule.class, Rule.class);
	}

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		invokeAppropriateMethodOnRuleAnnotatedMembers(context, NO_OP, GenericBeforeAndAfterAdvice::before);
	}

	@Override
	public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
		long numRuleAnnotatedMembers = invokeAppropriateMethodOnRuleAnnotatedMembers(context, Collections::reverse,
			advice -> {
				try {
					advice.handleTestExecutionException(throwable);
				}
				catch (Throwable t) {
					throw ExceptionUtils.throwAsUncheckedException(t);
				}
			});

		// If no appropriate @Rule annotated members were discovered, we then
		// have to rethrow the exception in order not to silently swallow it.
		// Fixes bug: https://github.com/junit-team/junit5/issues/1069
		if (numRuleAnnotatedMembers == 0) {
			throw throwable;
		}
	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		invokeAppropriateMethodOnRuleAnnotatedMembers(context, Collections::reverse,
			GenericBeforeAndAfterAdvice::after);
	}

	/**
	 * @return the number of appropriate rule-annotated members that were discovered
	 */
	private long invokeAppropriateMethodOnRuleAnnotatedMembers(ExtensionContext context,
			Consumer<List<TestRuleAnnotatedMember>> ordering, Consumer<GenericBeforeAndAfterAdvice> methodCaller) {
		// @formatter:off
		return Stream.of(getRuleAnnotatedMembers(context))
				.map(ArrayList::new)
				.peek(ordering::accept)
				.flatMap(Collection::stream)
				.filter(annotatedMember -> this.ruleType.isInstance(annotatedMember.getTestRule()))
				.map(this.adapterGenerator)
				.peek(methodCaller::accept)
				.count();
		// @formatter:on
	}

	@SuppressWarnings("unchecked")
	private List<TestRuleAnnotatedMember> getRuleAnnotatedMembers(ExtensionContext context) {
		Object testInstance = context.getRequiredTestInstance();
		Namespace namespace = Namespace.create(TestRuleSupport.class, context.getRequiredTestClass());
		// @formatter:off
		return context.getStore(namespace)
				.getOrComputeIfAbsent("rule-annotated-members", key -> findRuleAnnotatedMembers(testInstance), List.class);
		// @formatter:on
	}

}
