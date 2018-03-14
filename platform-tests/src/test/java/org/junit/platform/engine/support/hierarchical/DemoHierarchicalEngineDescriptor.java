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

import static org.junit.platform.engine.support.hierarchical.Node.SkipResult.doNotSkip;
import static org.junit.platform.engine.support.hierarchical.Node.SkipResult.skip;

import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;

/**
 * @since 1.0
 */
public class DemoHierarchicalEngineDescriptor extends EngineDescriptor implements Node<DemoEngineExecutionContext> {

	private String skippedReason;
	private boolean skipped;
	private Runnable beforeAllBehavior = () -> {
	};

	public DemoHierarchicalEngineDescriptor(UniqueId uniqueId) {
		super(uniqueId, uniqueId.getEngineId().get());
	}

	public void markSkipped(String reason) {
		this.skipped = true;
		this.skippedReason = reason;
	}

	public void setBeforeAllBehavior(Runnable beforeAllBehavior) {
		this.beforeAllBehavior = beforeAllBehavior;
	}

	@Override
	public SkipResult shouldBeSkipped(DemoEngineExecutionContext context) {
		return skipped ? skip(skippedReason) : doNotSkip();
	}

	@Override
	public DemoEngineExecutionContext before(DemoEngineExecutionContext context) throws Exception {
		beforeAllBehavior.run();
		return context;
	}

}
