/*
 * Copyright 2015-2019 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.condition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.platform.commons.PreconditionViolationException;

/**
 * Unit tests for {@link DisabledIfSystemPropertyCondition}.
 *
 * <p>Note that test method names MUST match the test method names in
 * {@link DisabledIfSystemPropertyIntegrationTests}.
 *
 * @since 5.1
 */
class DisabledIfSystemPropertyConditionTests extends AbstractExecutionConditionTests {

	@Override
	protected ExecutionCondition getExecutionCondition() {
		return new DisabledIfSystemPropertyCondition();
	}

	@Override
	protected Class<?> getTestClass() {
		return DisabledIfSystemPropertyIntegrationTests.class;
	}

	@BeforeAll
	static void setSystemProperty() {
		DisabledIfSystemPropertyIntegrationTests.setSystemProperty();
	}

	@AfterAll
	static void clearSystemProperty() {
		DisabledIfSystemPropertyIntegrationTests.clearSystemProperty();
	}

	/**
	 * @see DisabledIfSystemPropertyIntegrationTests#enabledBecauseAnnotationIsNotPresent()
	 */
	@Test
	void enabledBecauseAnnotationIsNotPresent() {
		evaluateCondition();
		assertEnabled();
		assertReasonContains("@DisabledIfSystemProperty is not present");
	}

	/**
	 * @see DisabledIfSystemPropertyIntegrationTests#blankNamedAttribute()
	 */
	@Test
	void blankNamedAttribute() {
		Exception exception = assertThrows(PreconditionViolationException.class, this::evaluateCondition);
		assertThat(exception).hasMessageContaining("The 'named' attribute must not be blank");
	}

	/**
	 * @see DisabledIfSystemPropertyIntegrationTests#blankMatchesAttribute()
	 */
	@Test
	void blankMatchesAttribute() {
		Exception exception = assertThrows(PreconditionViolationException.class, this::evaluateCondition);
		assertThat(exception).hasMessageContaining("The 'matches' attribute must not be blank");
	}

	/**
	 * @see DisabledIfSystemPropertyIntegrationTests#disabledBecauseSystemPropertyMatchesExactly()
	 */
	@Test
	void disabledBecauseSystemPropertyMatchesExactly() {
		evaluateCondition();
		assertDisabled();
		assertReasonContains("matches regular expression");
	}

	/**
	 * @see DisabledIfSystemPropertyIntegrationTests#disabledBecauseSystemPropertyMatchesPattern()
	 */
	@Test
	void disabledBecauseSystemPropertyMatchesPattern() {
		evaluateCondition();
		assertDisabled();
		assertReasonContains("matches regular expression");
	}

	/**
	 * @see DisabledIfSystemPropertyIntegrationTests#enabledBecauseSystemPropertyDoesNotMatch()
	 */
	@Test
	void enabledBecauseSystemPropertyDoesNotMatch() {
		evaluateCondition();
		assertEnabled();
		assertReasonContains("does not match regular expression");
	}

	/**
	 * @see DisabledIfSystemPropertyIntegrationTests#enabledBecauseSystemPropertyDoesNotExist()
	 */
	@Test
	void enabledBecauseSystemPropertyDoesNotExist() {
		evaluateCondition();
		assertEnabled();
		assertReasonContains("does not exist");
	}

}
