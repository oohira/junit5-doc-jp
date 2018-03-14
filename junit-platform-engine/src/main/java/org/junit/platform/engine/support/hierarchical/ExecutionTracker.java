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

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;

/**
 * @since 1.0
 */
class ExecutionTracker {

	private final Set<UniqueId> executedUniqueIds = ConcurrentHashMap.newKeySet();

	void markExecuted(TestDescriptor testDescriptor) {
		executedUniqueIds.add(testDescriptor.getUniqueId());
	}

	boolean wasAlreadyExecuted(TestDescriptor testDescriptor) {
		return executedUniqueIds.contains(testDescriptor.getUniqueId());
	}
}
