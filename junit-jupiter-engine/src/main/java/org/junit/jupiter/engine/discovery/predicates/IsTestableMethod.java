/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.discovery.predicates;

import static org.junit.platform.commons.util.AnnotationUtils.isAnnotated;
import static org.junit.platform.commons.util.ReflectionUtils.isAbstract;
import static org.junit.platform.commons.util.ReflectionUtils.isPrivate;
import static org.junit.platform.commons.util.ReflectionUtils.isStatic;
import static org.junit.platform.commons.util.ReflectionUtils.returnsVoid;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.function.Predicate;

/**
 * @since 5.0
 */
abstract class IsTestableMethod implements Predicate<Method> {

	private final Class<? extends Annotation> annotationType;
	private final boolean mustReturnVoid;

	IsTestableMethod(Class<? extends Annotation> annotationType, boolean mustReturnVoid) {
		this.annotationType = annotationType;
		this.mustReturnVoid = mustReturnVoid;
	}

	@Override
	public boolean test(Method candidate) {
		// Please do not collapse the following into a single statement.
		if (isStatic(candidate)) {
			return false;
		}
		if (isPrivate(candidate)) {
			return false;
		}
		if (isAbstract(candidate)) {
			return false;
		}
		if (returnsVoid(candidate) != this.mustReturnVoid) {
			return false;
		}

		return isAnnotated(candidate, this.annotationType);
	}

}
