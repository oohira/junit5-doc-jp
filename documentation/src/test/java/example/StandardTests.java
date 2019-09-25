/*
 * Copyright 2015-2019 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package example;

// tag::user_guide[]
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class StandardTests {

	@BeforeAll
	static void initAll() {
	}

	@BeforeEach
	void init() {
	}

	@Test
	void succeedingTest() {
	}

	// end::user_guide[]
	@extensions.ExpectToFail
	// tag::user_guide[]
	@Test
	void failingTest() {
		fail("失敗するテスト");
	}

	@Test
	@Disabled("デモ用")
	void skippedTest() {
		// 実行されない
	}

	@Test
	void abortedTest() {
		assumeTrue("abc".contains("Z"));
		fail("テストは中断されるはず");
	}

	@AfterEach
	void tearDown() {
	}

	@AfterAll
	static void tearDownAll() {
	}

}
// end::user_guide[]
