/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params;

import static java.util.Collections.singletonList;

import java.util.List;

import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;

/**
 * @since 5.0
 */
class ParameterizedTestInvocationContext implements TestTemplateInvocationContext {

	private final ParameterizedTestNameFormatter formatter;
	private final Object[] arguments;

	ParameterizedTestInvocationContext(ParameterizedTestNameFormatter formatter, Object[] arguments) {
		this.formatter = formatter;
		this.arguments = arguments;
	}

	@Override
	public String getDisplayName(int invocationIndex) {
		return formatter.format(invocationIndex, arguments);
	}

	@Override
	public List<Extension> getAdditionalExtensions() {
		return singletonList(new ParameterizedTestParameterResolver(arguments));
	}
}
