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

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

import org.apiguardian.api.API;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.migrationsupport.rules.adapter.VerifierAdapter;
import org.junit.rules.Verifier;

/**
 * This {@code Extension} provides native support for subclasses of
 * the {@link Verifier} rule from JUnit 4.
 *
 * <p>{@code @Rule}-annotated fields as well as methods are supported.
 *
 * <p>By using this class-level extension on a test class such
 * {@code Verifier} implementations in legacy code bases
 * can be left unchanged including the JUnit 4 rule import statements.
 *
 * <p>However, if you intend to develop a <em>new</em> extension for
 * JUnit 5 please use the new extension model of JUnit Jupiter instead
 * of the rule-based model of JUnit 4.
 *
 * @since 5.0
 * @see org.junit.rules.Verifier
 * @see org.junit.rules.TestRule
 * @see org.junit.Rule
 */
@API(status = EXPERIMENTAL, since = "5.0")
public class VerifierSupport implements AfterEachCallback {

	private final TestRuleSupport support = new TestRuleSupport(VerifierAdapter::new, Verifier.class);

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		this.support.afterEach(context);
	}

}
