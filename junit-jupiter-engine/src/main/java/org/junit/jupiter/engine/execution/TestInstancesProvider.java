/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.execution;

import static org.apiguardian.api.API.Status.INTERNAL;

import java.util.Optional;

import org.apiguardian.api.API;
import org.junit.jupiter.api.extension.TestInstances;
import org.junit.jupiter.engine.extension.ExtensionRegistry;

/**
 * @since 5.0
 */
@FunctionalInterface
@API(status = INTERNAL, since = "5.0")
public interface TestInstancesProvider {

	TestInstances getTestInstances(Optional<ExtensionRegistry> childExtensionRegistry);

}
