/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.console;

import static org.apiguardian.api.API.Status.INTERNAL;
import static org.apiguardian.api.API.Status.MAINTAINED;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;

import org.apiguardian.api.API;
import org.junit.platform.console.options.CommandLineOptions;
import org.junit.platform.console.options.CommandLineOptionsParser;
import org.junit.platform.console.options.JOptSimpleCommandLineOptionsParser;
import org.junit.platform.console.tasks.ConsoleTestExecutor;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

/**
 * The {@code ConsoleLauncher} is a stand-alone application for launching the
 * JUnit Platform from the console.
 *
 * @since 1.0
 */
@API(status = MAINTAINED, since = "1.0")
public class ConsoleLauncher {

	public static void main(String... args) {
		int exitCode = execute(System.out, System.err, args).getExitCode();
		System.exit(exitCode);
	}

	@API(status = INTERNAL, since = "1.0")
	public static ConsoleLauncherExecutionResult execute(PrintStream out, PrintStream err, String... args) {
		CommandLineOptionsParser parser = new JOptSimpleCommandLineOptionsParser();
		ConsoleLauncher consoleLauncher = new ConsoleLauncher(parser, out, err);
		return consoleLauncher.execute(args);
	}

	private final CommandLineOptionsParser commandLineOptionsParser;
	private final PrintStream outStream;
	private final PrintStream errStream;
	private final Charset charset;

	ConsoleLauncher(CommandLineOptionsParser commandLineOptionsParser, PrintStream out, PrintStream err) {
		this(commandLineOptionsParser, out, err, Charset.defaultCharset());
	}

	ConsoleLauncher(CommandLineOptionsParser commandLineOptionsParser, PrintStream out, PrintStream err,
			Charset charset) {
		this.commandLineOptionsParser = commandLineOptionsParser;
		this.outStream = out;
		this.errStream = err;
		this.charset = charset;
	}

	ConsoleLauncherExecutionResult execute(String... args) {
		CommandLineOptions options = commandLineOptionsParser.parse(args);
		try (PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outStream, charset)))) {
			if (options.isDisplayHelp()) {
				commandLineOptionsParser.printHelp(out);
				return ConsoleLauncherExecutionResult.success();
			}
			return executeTests(options, out);
		}
		finally {
			outStream.flush();
			errStream.flush();
		}
	}

	private ConsoleLauncherExecutionResult executeTests(CommandLineOptions options, PrintWriter out) {
		try {
			TestExecutionSummary testExecutionSummary = new ConsoleTestExecutor(options).execute(out);
			return ConsoleLauncherExecutionResult.forSummary(testExecutionSummary);
		}
		catch (Exception exception) {
			exception.printStackTrace(errStream);
			errStream.println();
			commandLineOptionsParser.printHelp(out);
		}
		return ConsoleLauncherExecutionResult.failed();
	}

}
