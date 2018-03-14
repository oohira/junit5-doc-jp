/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine.execution;

import static org.apiguardian.api.API.Status.INTERNAL;
import static org.junit.platform.engine.TestExecutionResult.failed;

import org.apiguardian.api.API;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.runner.JUnitCore;
import org.junit.vintage.engine.descriptor.RunnerTestDescriptor;

/**
 * @since 4.12
 */
@API(status = INTERNAL, since = "4.12")
public class RunnerExecutor {

	private final EngineExecutionListener engineExecutionListener;

	public RunnerExecutor(EngineExecutionListener engineExecutionListener) {
		this.engineExecutionListener = engineExecutionListener;
	}

	public void execute(RunnerTestDescriptor runnerTestDescriptor) {
		TestRun testRun = new TestRun(runnerTestDescriptor);
		JUnitCore core = new JUnitCore();
		core.addListener(new RunListenerAdapter(testRun, engineExecutionListener));
		try {
			core.run(runnerTestDescriptor.toRequest());
		}
		catch (Throwable t) {
			reportUnexpectedFailure(testRun, runnerTestDescriptor, failed(t));
		}
	}

	private void reportUnexpectedFailure(TestRun testRun, RunnerTestDescriptor runnerTestDescriptor,
			TestExecutionResult result) {
		if (testRun.isNotStarted(runnerTestDescriptor)) {
			engineExecutionListener.executionStarted(runnerTestDescriptor);
		}
		engineExecutionListener.executionFinished(runnerTestDescriptor, result);
	}

}
