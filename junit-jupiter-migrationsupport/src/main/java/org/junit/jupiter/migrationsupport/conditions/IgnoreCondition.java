/*
 * Copyright 2015-2019 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.migrationsupport.conditions;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

import org.apiguardian.api.API;
import org.junit.Ignore;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.StringUtils;

/**
 * {@link ExecutionCondition} that supports JUnit 4's {@link Ignore @Ignore}
 * annotation.
 *
 * @since 5.4
 * @see org.junit.Ignore @Ignore
 * @see org.junit.jupiter.api.Disabled @Disabled
 * @see #evaluateExecutionCondition(ExtensionContext)
 * @see org.junit.jupiter.migrationsupport.EnableJUnit4MigrationSupport
 */
@API(status = EXPERIMENTAL, since = "5.4")
public class IgnoreCondition implements ExecutionCondition {

	private static final ConditionEvaluationResult ENABLED = //
		ConditionEvaluationResult.enabled("@org.junit.Ignore is not present");

	/**
	 * Containers/tests are disabled if {@link Ignore @Ignore} is present on
	 * the test class or method.
	 */
	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		Optional<AnnotatedElement> element = context.getElement();
		Optional<Ignore> ignoreAnnotation = findAnnotation(element, Ignore.class);
		if (ignoreAnnotation.isPresent()) {
			String reason = ignoreAnnotation.map(Ignore::value) //
					.filter(StringUtils::isNotBlank) //
					.orElseGet(() -> element.get() + " is disabled via @org.junit.Ignore");
			return ConditionEvaluationResult.disabled(reason);
		}

		return ENABLED;
	}

}
