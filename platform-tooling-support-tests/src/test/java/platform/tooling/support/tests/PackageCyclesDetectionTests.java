/*
 * Copyright 2015-2019 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package platform.tooling.support.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.sormuras.bartholdy.Configuration;
import de.sormuras.bartholdy.tool.CyclesDetector;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import platform.tooling.support.Helper;

/**
 * @since 1.3
 */
class PackageCyclesDetectionTests {

	@ParameterizedTest
	@MethodSource("platform.tooling.support.Helper#loadModuleDirectoryNames")
	void moduleDoesNotContainCyclicPackageReferences(String module) {
		var jar = Helper.createJarPath(module);
		var result = new CyclesDetector(jar, this::ignore).run(Configuration.of());
		assertEquals(0, result.getExitCode(), "result=" + result);
	}

	private boolean ignore(String source, String target) {
		if (source.equals(target)) {
			return true;
		}
		if (source.startsWith("org.junit.jupiter.params.shadow.com.univocity.parsers.")) {
			return true;
		}
		//noinspection RedundantIfStatement
		if (!target.startsWith("org.junit.")) {
			return true;
		}
		return false;
	}

}
