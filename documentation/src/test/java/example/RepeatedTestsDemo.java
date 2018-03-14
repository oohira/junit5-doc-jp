/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package example;

// tag::user_guide[]
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.logging.Logger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.TestInfo;

class RepeatedTestsDemo {

	private Logger logger = // ...
		// end::user_guide[]
		Logger.getLogger(RepeatedTestsDemo.class.getName());
	// tag::user_guide[]

	@BeforeEach
	void beforeEach(TestInfo testInfo, RepetitionInfo repetitionInfo) {
		int currentRepetition = repetitionInfo.getCurrentRepetition();
		int totalRepetitions = repetitionInfo.getTotalRepetitions();
		String methodName = testInfo.getTestMethod().get().getName();
		logger.info(String.format("About to execute repetition %d of %d for %s", //
			currentRepetition, totalRepetitions, methodName));
	}

	@RepeatedTest(10)
	void repeatedTest() {
		// ...
	}

	@RepeatedTest(5)
	void repeatedTestWithRepetitionInfo(RepetitionInfo repetitionInfo) {
		assertEquals(5, repetitionInfo.getTotalRepetitions());
	}

	@RepeatedTest(value = 1, name = "{displayName} {currentRepetition}/{totalRepetitions}")
	@DisplayName("Repeat!")
	void customDisplayName(TestInfo testInfo) {
		assertEquals(testInfo.getDisplayName(), "Repeat! 1/1");
	}

	@RepeatedTest(value = 1, name = RepeatedTest.LONG_DISPLAY_NAME)
	@DisplayName("Details...")
	void customDisplayNameWithLongPattern(TestInfo testInfo) {
		assertEquals(testInfo.getDisplayName(), "Details... :: repetition 1 of 1");
	}

	@RepeatedTest(value = 5, name = "Wiederholung {currentRepetition} von {totalRepetitions}")
	void repeatedTestInGerman() {
		// ...
	}

}
// end::user_guide[]
