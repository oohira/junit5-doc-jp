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

// @formatter:off
// tag::user_guide[]
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofMinutes;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AssertionsDemo {

	// end::user_guide[]
	Person person = new Person("John", "Doe");

	// tag::user_guide[]
	@Test
	void standardAssertions() {
		assertEquals(2, 2);
		assertEquals(4, 4, "The optional assertion message is now the last parameter.");
		assertTrue('a' < 'b', () -> "Assertion messages can be lazily evaluated -- "
				+ "to avoid constructing complex messages unnecessarily.");
	}

	@Test
	void groupedAssertions() {
		// In a grouped assertion all assertions are executed, and any
		// failures will be reported together.
		assertAll("person",
			() -> assertEquals("John", person.getFirstName()),
			() -> assertEquals("Doe", person.getLastName())
		);
	}

	@Test
	void dependentAssertions() {
		// Within a code block, if an assertion fails the
		// subsequent code in the same block will be skipped.
		assertAll("properties",
			() -> {
				String firstName = person.getFirstName();
				assertNotNull(firstName);

				// Executed only if the previous assertion is valid.
				assertAll("first name",
					() -> assertTrue(firstName.startsWith("J")),
					() -> assertTrue(firstName.endsWith("n"))
				);
			},
			() -> {
				// Grouped assertion, so processed independently
				// of results of first name assertions.
				String lastName = person.getLastName();
				assertNotNull(lastName);

				// Executed only if the previous assertion is valid.
				assertAll("last name",
					() -> assertTrue(lastName.startsWith("D")),
					() -> assertTrue(lastName.endsWith("e"))
				);
			}
		);
	}

	@Test
	void exceptionTesting() {
		Throwable exception = assertThrows(IllegalArgumentException.class, () -> {
			throw new IllegalArgumentException("a message");
		});
		assertEquals("a message", exception.getMessage());
	}

	@Test
	void timeoutNotExceeded() {
		// The following assertion succeeds.
		assertTimeout(ofMinutes(2), () -> {
			// Perform task that takes less than 2 minutes.
		});
	}

	@Test
	void timeoutNotExceededWithResult() {
		// The following assertion succeeds, and returns the supplied object.
		String actualResult = assertTimeout(ofMinutes(2), () -> {
			return "a result";
		});
		assertEquals("a result", actualResult);
	}

	@Test
	void timeoutNotExceededWithMethod() {
		// The following assertion invokes a method reference and returns an object.
		String actualGreeting = assertTimeout(ofMinutes(2), AssertionsDemo::greeting);
		assertEquals("Hello, World!", actualGreeting);
	}

	// end::user_guide[]
	@extensions.ExpectToFail
	// tag::user_guide[]
	@Test
	void timeoutExceeded() {
		// The following assertion fails with an error message similar to:
		// execution exceeded timeout of 10 ms by 91 ms
		assertTimeout(ofMillis(10), () -> {
			// Simulate task that takes more than 10 ms.
			Thread.sleep(100);
		});
	}

	// end::user_guide[]
	@extensions.ExpectToFail
	// tag::user_guide[]
	@Test
	void timeoutExceededWithPreemptiveTermination() {
		// The following assertion fails with an error message similar to:
		// execution timed out after 10 ms
		assertTimeoutPreemptively(ofMillis(10), () -> {
			// Simulate task that takes more than 10 ms.
			Thread.sleep(100);
		});
	}

	private static String greeting() {
		return "Hello, World!";
	}

}
// end::user_guide[]
// @formatter:on
