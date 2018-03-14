/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons;

import static org.apiguardian.api.API.Status.INTERNAL;

import org.apiguardian.api.API;

/**
 * Base class for all {@link RuntimeException RuntimeExceptions} thrown
 * by JUnit.
 *
 * @since 1.0
 */
@API(status = INTERNAL, since = "1.0")
public class JUnitException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public JUnitException(String message) {
		super(message);
	}

	public JUnitException(String message, Throwable cause) {
		super(message, cause);
	}

}
