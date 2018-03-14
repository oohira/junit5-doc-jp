/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.migrationsupport.rules.adapter;

import static org.apiguardian.api.API.Status.INTERNAL;

import org.apiguardian.api.API;

/**
 * @since 5.0
 */
@API(status = INTERNAL, since = "5.0")
public interface GenericBeforeAndAfterAdvice {

	default void before() {
	}

	default void handleTestExecutionException(Throwable cause) throws Throwable {
	}

	default void after() {
	}

}
