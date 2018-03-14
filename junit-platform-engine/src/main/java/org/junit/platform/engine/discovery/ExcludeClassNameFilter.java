/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.discovery;

import static org.junit.platform.engine.FilterResult.excluded;
import static org.junit.platform.engine.FilterResult.included;

import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.junit.platform.engine.FilterResult;

/**
 * {@link ClassNameFilter} that matches fully qualified class names against
 * patterns in the form of regular expressions.
 *
 * <p>If the fully qualified name of a class matches against at least one
 * pattern, the class will be excluded.
 *
 * @since 1.0
 */
class ExcludeClassNameFilter extends AbstractClassNameFilter {

	ExcludeClassNameFilter(String... patterns) {
		super(patterns);
	}

	@Override
	public FilterResult apply(String className) {
		return findMatchingPattern(className) //
				.map(pattern -> excluded(formatExclusionReason(className, pattern))) //
				.orElseGet(() -> included(formatInclusionReason(className)));
	}

	private String formatExclusionReason(String className, Pattern pattern) {
		return String.format("Class name [%s] matches excluded pattern: '%s'", className, pattern);
	}

	private String formatInclusionReason(String className) {
		return String.format("Class name [%s] does not match any excluded pattern: %s", className, patternDescription);
	}

	@Override
	public Predicate<String> toPredicate() {
		return className -> !findMatchingPattern(className).isPresent();
	}

	@Override
	public String toString() {
		return "Excludes class names that match regular expression " + patternDescription;
	}

}
