/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.logging.LogRecordListener;

/**
 * Integration tests that verify the correct behavior for methods annotated
 * with multiple testable annotations simultaneously.
 *
 * @since 5.0
 */
@TrackLogRecords
class MultipleTestableAnnotationsTests extends AbstractJupiterTestEngineTests {

	@Test
	void testAndRepeatedTest(LogRecordListener listener) {
		discoverTests(request().selectors(selectClass(TestCase.class)).build());

		// @formatter:off
		assertThat(listener.stream()
			.filter(logRecord -> logRecord.getLevel() == Level.WARNING)
			.map(LogRecord::getMessage)
			.filter(m -> m.matches("Possible configuration error: method .+ resulted in multiple TestDescriptors .+"))
			.count()
		).isEqualTo(1);
		// @formatter:on
	}

	static class TestCase {

		@Test
		@RepeatedTest(1)
		void testAndRepeatedTest(RepetitionInfo repetitionInfo) {
		}

	}

}
