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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;
import static org.junit.jupiter.api.parallel.Resources.SYSTEM_PROPERTIES;

import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ResourceLock;

// tag::user_guide[]
@Execution(CONCURRENT)
class SharedResourcesDemo {

	private Properties backup;

	@BeforeEach
	void backup() {
		backup = new Properties();
		backup.putAll(System.getProperties());
	}

	@AfterEach
	void restore() {
		System.setProperties(backup);
	}

	@Test
	@ResourceLock(value = SYSTEM_PROPERTIES, mode = READ)
	void customPropertyIsNotSetByDefault() {
		assertNull(System.getProperty("my.prop"));
	}

	@Test
	@ResourceLock(value = SYSTEM_PROPERTIES, mode = READ_WRITE)
	void canSetCustomPropertyToFoo() {
		System.setProperty("my.prop", "foo");
		assertEquals("foo", System.getProperty("my.prop"));
	}

	@Test
	@ResourceLock(value = SYSTEM_PROPERTIES, mode = READ_WRITE)
	void canSetCustomPropertyToBar() {
		System.setProperty("my.prop", "bar");
		assertEquals("bar", System.getProperty("my.prop"));
	}
}
// end::user_guide[]
