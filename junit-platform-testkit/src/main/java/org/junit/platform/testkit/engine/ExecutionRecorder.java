/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.testkit.engine;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.ReportEntry;

/**
 * {@code ExecutionRecorder} is an {@link EngineExecutionListener} that records
 * data from every event that occurs during the engine execution lifecycle and
 * provides functionality for retrieving execution state via
 * {@link EngineExecutionResults}.
 *
 * @since 1.4
 * @see EngineExecutionResults
 * @see Event
 * @see Execution
 */
class ExecutionRecorder implements EngineExecutionListener {

	private final List<Event> events = new CopyOnWriteArrayList<>();

	/**
	 * Record an {@link Event} for a dynamically registered container
	 * or test.
	 */
	@Override
	public void dynamicTestRegistered(TestDescriptor testDescriptor) {
		this.events.add(Event.dynamicTestRegistered(testDescriptor));
	}

	/**
	 * Record an {@link Event} for a container or test that was skipped.
	 */
	@Override
	public void executionSkipped(TestDescriptor testDescriptor, String reason) {
		this.events.add(Event.executionSkipped(testDescriptor, reason));
	}

	/**
	 * Record an {@link Event} for a container or test that started.
	 */
	@Override
	public void executionStarted(TestDescriptor testDescriptor) {
		this.events.add(Event.executionStarted(testDescriptor));
	}

	/**
	 * Record an {@link Event} for a container or test that completed
	 * with the provided {@link TestExecutionResult}.
	 */
	@Override
	public void executionFinished(TestDescriptor testDescriptor, TestExecutionResult testExecutionResult) {
		this.events.add(Event.executionFinished(testDescriptor, testExecutionResult));
	}

	/**
	 * Record an {@link Event} for a published {@link ReportEntry}.
	 */
	@Override
	public void reportingEntryPublished(TestDescriptor testDescriptor, ReportEntry entry) {
		this.events.add(Event.reportingEntryPublished(testDescriptor, entry));
	}

	/**
	 * Get the state of the engine's execution in the form of {@link EngineExecutionResults}.
	 *
	 * @return the {@code EngineExecutionResults} containing all current state information
	 */
	EngineExecutionResults getExecutionResults() {
		return new EngineExecutionResults(this.events);
	}

}
