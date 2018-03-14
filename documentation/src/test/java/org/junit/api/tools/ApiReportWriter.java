/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.api.tools;

import java.io.PrintWriter;
import java.util.EnumSet;

import org.apiguardian.api.API.Status;

/**
 * @since 1.0
 */
interface ApiReportWriter {

	void printReportHeader(PrintWriter out);

	void printDeclarationInfo(PrintWriter out, EnumSet<Status> statuses);

}
