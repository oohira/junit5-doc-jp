/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.util;

import static java.lang.String.join;
import static org.apiguardian.api.API.Status.INTERNAL;

import java.util.ArrayList;
import java.util.List;

import org.apiguardian.api.API;

/**
 * Simple builder for generating strings in custom implementations of
 * {@link Object#toString toString()}.
 *
 * <h3>DISCLAIMER</h3>
 *
 * <p>These utilities are intended solely for usage within the JUnit framework
 * itself. <strong>Any usage by external parties is not supported.</strong>
 * Use at your own risk!
 *
 * @since 1.0
 */
@API(status = INTERNAL, since = "1.0")
public class ToStringBuilder {

	private final Class<?> type;

	private final List<String> values = new ArrayList<>();

	public ToStringBuilder(Object obj) {
		this.type = Preconditions.notNull(obj, "Object must not be null").getClass();
	}

	public ToStringBuilder(Class<?> type) {
		this.type = Preconditions.notNull(type, "Class must not be null");
	}

	public ToStringBuilder append(String name, Object value) {
		Preconditions.notBlank(name, "Name must not be null or blank");
		this.values.add(name + " = " + toString(value));
		return this;
	}

	private String toString(Object obj) {
		return (obj instanceof CharSequence) ? ("'" + obj + "'") : StringUtils.nullSafeToString(obj);
	}

	@Override
	public String toString() {
		return this.type.getSimpleName() + " [" + join(", ", this.values) + "]";
	}

}
