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

import static org.apiguardian.api.API.Status.STABLE;

import org.apiguardian.api.API;
import org.junit.platform.engine.DiscoveryFilter;

/**
 * {@link DiscoveryFilter} that is applied to the name of a {@link Class}.
 *
 * @since 1.0
 * @see #includeClassNamePatterns
 */
@API(status = STABLE, since = "1.0")
public interface ClassNameFilter extends DiscoveryFilter<String> {

	/**
	 * Standard include pattern in the form of a regular expression that is
	 * used to match against fully qualified class names:
	 * {@value org.junit.platform.engine.discovery.ClassNameFilter#STANDARD_INCLUDE_PATTERN}
	 * which matches against class names ending in {@code Test} or
	 * {@code Tests} (in any package).
	 */
	String STANDARD_INCLUDE_PATTERN = "^.*Tests?$";

	/**
	 * Create a new <em>include</em> {@link ClassNameFilter} based on the
	 * supplied patterns.
	 *
	 * <p>The patterns are combined using OR semantics, i.e. if the fully
	 * qualified name of a class matches against at least one of the patterns,
	 * the class will be included in the result set.
	 *
	 * @param patterns regular expressions to match against fully qualified
	 * class names; never {@code null}, empty, or containing {@code null}
	 * @see Class#getName()
	 */
	static ClassNameFilter includeClassNamePatterns(String... patterns) {
		return new IncludeClassNameFilter(patterns);
	}

	/**
	 * Create a new <em>exclude</em> {@link ClassNameFilter} based on the
	 * supplied patterns.
	 *
	 * <p>The patterns are combined using OR semantics, i.e. if the fully
	 * qualified name of a class matches against at least one of the patterns,
	 * the class will be excluded from the result set.
	 *
	 * @param patterns regular expressions to match against fully qualified
	 * class names; never {@code null}, empty, or containing {@code null}
	 * @see Class#getName()
	 */
	static ClassNameFilter excludeClassNamePatterns(String... patterns) {
		return new ExcludeClassNameFilter(patterns);
	}

}
