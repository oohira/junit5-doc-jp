/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.extension;

import static org.apiguardian.api.API.Status.STABLE;

import org.apiguardian.api.API;

/**
 * {@code AfterEachCallback} defines the API for {@link Extension Extensions}
 * that wish to provide additional behavior to tests after each test method
 * has been invoked.
 *
 * <p>In this context, the term <em>test</em> refers to the actual test method
 * plus any user defined teardown methods (e.g.,
 * {@link org.junit.jupiter.api.AfterEach @AfterEach} methods).
 *
 * <p>Concrete implementations often implement {@link BeforeEachCallback}
 * as well.
 *
 * <h3>Constructor Requirements</h3>
 *
 * <p>Consult the documentation in {@link Extension} for details on
 * constructor requirements.
 *
 * @since 5.0
 * @see org.junit.jupiter.api.AfterEach
 * @see BeforeEachCallback
 * @see BeforeTestExecutionCallback
 * @see AfterTestExecutionCallback
 * @see BeforeAllCallback
 * @see AfterAllCallback
 */
@FunctionalInterface
@API(status = STABLE, since = "5.0")
public interface AfterEachCallback extends Extension {

	/**
	 * Callback that is invoked <em>after</em> each test has been invoked.
	 *
	 * @param context the current extension context; never {@code null}
	 */
	void afterEach(ExtensionContext context) throws Exception;

}
