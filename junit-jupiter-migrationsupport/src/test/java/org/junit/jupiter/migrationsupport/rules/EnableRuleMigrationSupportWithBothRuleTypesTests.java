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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.migrationsupport.rules.FailAfterAllHelper.fail;

import org.junit.Rule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.Verifier;

@EnableRuleMigrationSupport
public class EnableRuleMigrationSupportWithBothRuleTypesTests {

	private static boolean afterOfRule1WasExecuted = false;

	private static boolean beforeOfRule2WasExecuted = false;
	private static boolean afterOfRule2WasExecuted = false;

	private static int numberOfRule1InstancesCreated = 0;
	private static int numberOfRule2InstancesCreated = 0;

	@Rule
	public Verifier verifier1 = new Verifier() {
		{
			numberOfRule1InstancesCreated++;
		}

		@Override
		protected void verify() throws Throwable {
			afterOfRule1WasExecuted = true;
		}
	};

	@Rule
	public ExternalResource getResource2() {
		return new ExternalResource() {
			{
				numberOfRule2InstancesCreated++;
			}

			private Object instance;

			@Override
			protected void before() throws Throwable {
				instance = this;
				beforeOfRule2WasExecuted = true;
			}

			@Override
			protected void after() {
				assertNotNull(instance);
				assertSame(instance, this);
				afterOfRule2WasExecuted = true;
			}
		};
	}

	@Test
	void beforeMethodOfBothRule2WasExecuted() {
		assertTrue(beforeOfRule2WasExecuted);
	}

	@AfterAll
	static void afterMethodsOfBothRulesWereExecuted() {
		assertEquals(1, numberOfRule1InstancesCreated);
		assertEquals(1, numberOfRule2InstancesCreated);
		if (!afterOfRule1WasExecuted)
			fail();
		if (!afterOfRule2WasExecuted)
			fail();
	}

}
