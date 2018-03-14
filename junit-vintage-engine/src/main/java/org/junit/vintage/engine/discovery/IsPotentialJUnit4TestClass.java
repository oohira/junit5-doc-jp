/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine.discovery;

import static org.junit.platform.commons.util.ReflectionUtils.isAbstract;
import static org.junit.platform.commons.util.ReflectionUtils.isInnerClass;
import static org.junit.platform.commons.util.ReflectionUtils.isPublic;

import java.util.function.Predicate;

/**
 * @since 4.12
 */
class IsPotentialJUnit4TestClass implements Predicate<Class<?>> {

	@Override
	public boolean test(Class<?> candidate) {
		// Do not collapse into a single return statement.
		if (!isPublic(candidate)) {
			return false;
		}
		if (isAbstract(candidate)) {
			return false;
		}
		if (isInnerClass(candidate)) {
			return false;
		}

		return true;
	}

}
