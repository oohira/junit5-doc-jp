/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params.converter;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

import org.apiguardian.api.API;
import org.junit.platform.commons.JUnitException;

/**
 * {@code ArgumentConversionException} is an exception that can
 * occur when an object is converted to another object by an implementation
 * of an {@code org.junit.jupiter.params.converter.ArgumentConverter}.
 *
 * @since 5.0
 * @see org.junit.jupiter.params.converter.ArgumentConverter
 */
@API(status = EXPERIMENTAL, since = "5.0")
public class ArgumentConversionException extends JUnitException {

	private static final long serialVersionUID = 1L;

	public ArgumentConversionException(String message) {
		super(message);
	}

	public ArgumentConversionException(String message, Throwable cause) {
		super(message, cause);
	}

}
