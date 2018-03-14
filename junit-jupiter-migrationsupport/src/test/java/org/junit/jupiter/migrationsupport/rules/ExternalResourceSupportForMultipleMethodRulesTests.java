/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.migrationsupport.rules;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.migrationsupport.rules.FailAfterAllHelper.fail;

import org.junit.Rule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.rules.ExternalResource;

@ExtendWith(ExternalResourceSupport.class)
public class ExternalResourceSupportForMultipleMethodRulesTests {

	private static boolean beforeOfRule1WasExecuted = false;
	private static boolean beforeOfRule2WasExecuted = false;

	private static boolean afterOfRule1WasExecuted = false;
	private static boolean afterOfRule2WasExecuted = false;

	@Rule
	public ExternalResource getResource1() {
		return new ExternalResource() {
			@Override
			protected void before() throws Throwable {
				beforeOfRule1WasExecuted = true;
			}

			@Override
			protected void after() {
				afterOfRule1WasExecuted = true;
			}
		};
	}

	@Rule
	public ExternalResource getResource2() {
		return new ExternalResource() {
			@Override
			protected void before() throws Throwable {
				beforeOfRule2WasExecuted = true;
			}

			@Override
			protected void after() {
				afterOfRule2WasExecuted = true;
			}
		};
	}

	@Test
	void beforeMethodsOfBothRulesWereExecuted() {
		assertTrue(beforeOfRule1WasExecuted);
		assertTrue(beforeOfRule2WasExecuted);
	}

	@AfterAll
	static void afterMethodsOfBothRulesWereExecuted() {
		if (!afterOfRule1WasExecuted)
			fail();
		if (!afterOfRule2WasExecuted)
			fail();
	}

}
