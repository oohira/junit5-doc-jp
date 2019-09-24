/*
 * Copyright 2015-2019 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.descriptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.PreconditionViolationException;

/**
 * Unit tests for {@link DefaultUriSource}.
 *
 * @since 1.3
 */
class DefaultUriSourceTests extends AbstractTestSourceTests {

	@Override
	Stream<UriSource> createSerializableInstances() {
		return Stream.of(new DefaultUriSource(URI.create("sample://instance")));
	}

	@Test
	void nullSourceUriYieldsException() {
		assertThrows(PreconditionViolationException.class, () -> new DefaultUriSource(null));
	}

	@Test
	void getterReturnsSameUriInstanceAsSuppliedToTheConstructor() throws Exception {
		URI expected = new URI("foo.txt");
		URI actual = new DefaultUriSource(expected).getUri();
		assertSame(expected, actual);
	}

	@Test
	void equalsAndHashCode() throws Exception {
		URI uri1 = new URI("foo.txt");
		URI uri2 = new URI("bar.txt");
		assertEqualsAndHashCode(new DefaultUriSource(uri1), new DefaultUriSource(uri1), new DefaultUriSource(uri2));
	}

	@Test
	void testToString() {
		String actual = new DefaultUriSource(URI.create("foo.txt")).toString();
		assertEquals("DefaultUriSource [uri = foo.txt]", actual);
	}
}
