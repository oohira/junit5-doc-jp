/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.migrationsupport.rules.member;

import static org.apiguardian.api.API.Status.INTERNAL;

import java.lang.reflect.Method;

import org.apiguardian.api.API;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.rules.TestRule;

/**
 * @since 5.0
 */
@API(status = INTERNAL, since = "5.1")
public final class TestRuleAnnotatedMethod extends AbstractTestRuleAnnotatedMember {

	public TestRuleAnnotatedMethod(Object testInstance, Method method) {
		super((TestRule) ReflectionUtils.invokeMethod(method, testInstance));
	}

}
