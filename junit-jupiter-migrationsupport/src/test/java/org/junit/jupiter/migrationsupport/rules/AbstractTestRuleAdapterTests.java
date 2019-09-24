/*
 * Copyright 2015-2019 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.migrationsupport.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.adapter.AbstractTestRuleAdapter;
import org.junit.jupiter.migrationsupport.rules.member.TestRuleAnnotatedMember;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.rules.ErrorCollector;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.rules.Verifier;

/**
 * @since 5.0
 */
public class AbstractTestRuleAdapterTests {

	@Test
	void constructionWithAssignableArgumentsIsSuccessful() {
		new TestableTestRuleAdapter(new SimpleRuleAnnotatedMember(new ErrorCollector()), Verifier.class);
	}

	@Test
	void constructionWithUnassignableArgumentsFails() {
		PreconditionViolationException exception = assertThrows(PreconditionViolationException.class,
			() -> new TestableTestRuleAdapter(new SimpleRuleAnnotatedMember(new TemporaryFolder()), Verifier.class));

		assertEquals(exception.getMessage(),
			"class org.junit.rules.Verifier is not assignable from class org.junit.rules.TemporaryFolder");
	}

	@Test
	void exceptionsDuringMethodLookupAreWrappedAndThrown() {
		AbstractTestRuleAdapter adapter = new AbstractTestRuleAdapter(
			new SimpleRuleAnnotatedMember(new ErrorCollector()), Verifier.class) {

			@Override
			public void before() {
				super.executeMethod("foo");
			}
		};

		JUnitException exception = assertThrows(JUnitException.class, adapter::before);

		assertEquals(exception.getMessage(), "Failed to find method foo() in class org.junit.rules.ErrorCollector");
	}

	private static class TestableTestRuleAdapter extends AbstractTestRuleAdapter {

		TestableTestRuleAdapter(TestRuleAnnotatedMember annotatedMember, Class<? extends TestRule> adapteeClass) {
			super(annotatedMember, adapteeClass);
		}
	}

	private static class SimpleRuleAnnotatedMember implements TestRuleAnnotatedMember {

		private final TestRule testRule;

		SimpleRuleAnnotatedMember(TestRule testRule) {
			this.testRule = testRule;
		}

		@Override
		public TestRule getTestRule() {
			return this.testRule;
		}

	}

}
