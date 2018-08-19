/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.core;

import static java.util.Collections.unmodifiableCollection;

import java.util.Collection;

import org.junit.platform.engine.TestEngine;
import org.junit.platform.launcher.TestExecutionListener;

/**
 * Default implementation of the {@link LauncherConfig} API.
 *
 * @since 1.3
 */
class DefaultLauncherConfig implements LauncherConfig {

	private final boolean testEngineAutoRegistrationEnabled;

	private final boolean testExecutionListenerAutoRegistrationEnabled;

	private final Collection<TestEngine> additionalTestEngines;

	private final Collection<TestExecutionListener> additionalTestExecutionListeners;

	DefaultLauncherConfig(boolean testEngineAutoRegistrationEnabled,
			boolean testExecutionListenerAutoRegistrationEnabled, Collection<TestEngine> additionalTestEngines,
			Collection<TestExecutionListener> additionalTestExecutionListeners) {

		this.testExecutionListenerAutoRegistrationEnabled = testExecutionListenerAutoRegistrationEnabled;
		this.testEngineAutoRegistrationEnabled = testEngineAutoRegistrationEnabled;
		this.additionalTestEngines = unmodifiableCollection(additionalTestEngines);
		this.additionalTestExecutionListeners = unmodifiableCollection(additionalTestExecutionListeners);
	}

	@Override
	public boolean isTestEngineAutoRegistrationEnabled() {
		return this.testEngineAutoRegistrationEnabled;
	}

	@Override
	public boolean isTestExecutionListenerAutoRegistrationEnabled() {
		return this.testExecutionListenerAutoRegistrationEnabled;
	}

	@Override
	public Collection<TestEngine> getAdditionalTestEngines() {
		return this.additionalTestEngines;
	}

	@Override
	public Collection<TestExecutionListener> getAdditionalTestExecutionListeners() {
		return this.additionalTestExecutionListeners;
	}

}
