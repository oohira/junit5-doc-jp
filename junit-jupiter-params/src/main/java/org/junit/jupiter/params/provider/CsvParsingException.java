/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params.provider;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

import org.apiguardian.api.API;
import org.junit.platform.commons.JUnitException;

/**
 * Thrown if an error is encountered while parsing CSV input.
 *
 * @since 5.3
 * @see CsvSource
 * @see CsvFileSource
 */
@API(status = EXPERIMENTAL, since = "5.3")
public class CsvParsingException extends JUnitException {

	private static final long serialVersionUID = 1L;

	public CsvParsingException(String message) {
		super(message);
	}

	public CsvParsingException(String message, Throwable cause) {
		super(message, cause);
	}

}
