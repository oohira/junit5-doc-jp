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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

import de.sormuras.bartholdy.Result;
import de.sormuras.bartholdy.tool.GradleWrapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import platform.tooling.support.Request;

class VintageGradleIntegrationTests {

	@Test
	void unsupportedVersion() {
		Result result = run("4.11");

		assertThat(result.getExitCode()).isEqualTo(0);
		assertThat(result.getOutput("out")) //
				.doesNotContain("STARTED") //
				.contains("Unsupported version of junit:junit: 4.11");
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "4.12", "4.13-beta-1" })
	void supportedVersions(String version) {
		Result result = run(version);

		assertThat(result.getExitCode()).isGreaterThan(0);
		assertThat(result.getOutput("out")) //
				.contains("com.example.vintage.VintageTest > success PASSED") //
				.contains("com.example.vintage.VintageTest > failure FAILED");

		Path testResultsDir = Request.WORKSPACE.resolve("vintage-gradle-" + version).resolve("build/test-results/test");
		assertThat(testResultsDir.resolve("TEST-com.example.vintage.VintageTest.xml")).isRegularFile();
	}

	private Result run(String version) {
		Result result = Request.builder() //
				.setTool(new GradleWrapper(Paths.get(".."))) //
				.setProject("vintage") //
				.setWorkspace("vintage-gradle-" + version) //
				.addArguments("clean", "test", "--stacktrace") //
				.addArguments("-Djunit4Version=" + version) //
				.setTimeout(Duration.ofMinutes(2)) //
				.build() //
				.run();
		assumeFalse(result.isTimedOut(), () -> "tool timed out: " + result);
		return result;
	}

}
