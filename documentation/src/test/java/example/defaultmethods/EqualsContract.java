/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package example.defaultmethods;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

// tag::user_guide[]
public interface EqualsContract<T> extends Testable<T> {

	T createNotEqualValue();

	@Test
	default void valueEqualsItself() {
		T value = createValue();
		assertEquals(value, value);
	}

	@Test
	default void valueDoesNotEqualNull() {
		T value = createValue();
		assertFalse(value.equals(null));
	}

	@Test
	default void valueDoesNotEqualDifferentValue() {
		T value = createValue();
		T differentValue = createNotEqualValue();
		assertNotEquals(value, differentValue);
		assertNotEquals(differentValue, value);
	}

}
// end::user_guide[]
