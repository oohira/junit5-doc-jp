/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.hierarchical;

import java.util.concurrent.Future;

import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;

/**
 * Implementation core of all {@link TestEngine TestEngines} that wish to
 * use the {@link Node} abstraction as the driving principle for structuring
 * and executing test suites.
 *
 * <p>A {@code HierarchicalTestExecutor} is instantiated by a concrete
 * implementation of {@link HierarchicalTestEngine} and takes care of
 * executing nodes in the hierarchy in the appropriate order as well as
 * firing the necessary events in the {@link EngineExecutionListener}.
 *
 * @param <C> the type of {@code EngineExecutionContext} used by the
 * {@code HierarchicalTestEngine}
 * @since 1.0
 */
class HierarchicalTestExecutor<C extends EngineExecutionContext> {

	private final ExecutionRequest request;
	private final C rootContext;
	private final HierarchicalTestExecutorService executorService;
	private final ThrowableCollector.Factory throwableCollectorFactory;

	HierarchicalTestExecutor(ExecutionRequest request, C rootContext, HierarchicalTestExecutorService executorService,
			ThrowableCollector.Factory throwableCollectorFactory) {
		this.request = request;
		this.rootContext = rootContext;
		this.executorService = executorService;
		this.throwableCollectorFactory = throwableCollectorFactory;
	}

	Future<Void> execute() {
		TestDescriptor rootTestDescriptor = this.request.getRootTestDescriptor();
		EngineExecutionListener executionListener = this.request.getEngineExecutionListener();
		NodeExecutionAdvisor executionAdvisor = new NodeTreeWalker().walk(rootTestDescriptor);
		NodeTestTaskContext taskContext = new NodeTestTaskContext(executionListener, this.executorService,
			this.throwableCollectorFactory, executionAdvisor);
		NodeTestTask<C> rootTestTask = new NodeTestTask<>(taskContext, rootTestDescriptor);
		rootTestTask.setParentContext(this.rootContext);
		return this.executorService.submit(rootTestTask);
	}

}
