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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.platform.engine.support.descriptor.ClasspathResourceSource.CLASSPATH_SCHEME;

import java.net.URI;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.PreconditionViolationException;

/**
 * Unit tests for {@link ClasspathResourceSource}.
 *
 * @since 1.0
 */
class ClasspathResourceSourceTests extends AbstractTestSourceTests {

	private static final String FOO_RESOURCE = "test/foo.xml";
	private static final String BAR_RESOURCE = "/config/bar.json";

	private static final URI FOO_RESOURCE_URI = URI.create(CLASSPATH_SCHEME + ":/" + FOO_RESOURCE);

	@Override
	Stream<ClasspathResourceSource> createSerializableInstances() {
		return Stream.of(ClasspathResourceSource.from(FOO_RESOURCE));
	}

	@Test
	void preconditions() {
		assertThrows(PreconditionViolationException.class, () -> ClasspathResourceSource.from((String) null));
		assertThrows(PreconditionViolationException.class, () -> ClasspathResourceSource.from(""));
		assertThrows(PreconditionViolationException.class, () -> ClasspathResourceSource.from("   "));

		assertThrows(PreconditionViolationException.class, () -> ClasspathResourceSource.from((URI) null));
		assertThrows(PreconditionViolationException.class,
			() -> ClasspathResourceSource.from(URI.create("file:/foo.txt")));
	}

	@Test
	void resourceWithoutPosition() {
		ClasspathResourceSource source = ClasspathResourceSource.from(FOO_RESOURCE);

		assertThat(source).isNotNull();
		assertThat(source.getClasspathResourceName()).isEqualTo(FOO_RESOURCE);
		assertThat(source.getPosition()).isEmpty();
	}

	@Test
	void resourceWithLeadingSlashWithoutPosition() {
		ClasspathResourceSource source = ClasspathResourceSource.from("/" + FOO_RESOURCE);

		assertThat(source).isNotNull();
		assertThat(source.getClasspathResourceName()).isEqualTo(FOO_RESOURCE);
		assertThat(source.getPosition()).isEmpty();
	}

	@Test
	void resourceWithPosition() {
		FilePosition position = FilePosition.from(42, 23);
		ClasspathResourceSource source = ClasspathResourceSource.from(FOO_RESOURCE, position);

		assertThat(source).isNotNull();
		assertThat(source.getClasspathResourceName()).isEqualTo(FOO_RESOURCE);
		assertThat(source.getPosition()).hasValue(position);
	}

	@Test
	void resourcefromUriWithoutPosition() {
		ClasspathResourceSource source = ClasspathResourceSource.from(FOO_RESOURCE_URI);

		assertThat(source).isNotNull();
		assertThat(source.getClasspathResourceName()).isEqualTo(FOO_RESOURCE);
		assertThat(source.getPosition()).isEmpty();
	}

	@Test
	void resourceFromUriWithLineNumber() {
		FilePosition position = FilePosition.from(42);
		URI uri = URI.create(FOO_RESOURCE_URI + "?line=42");
		ClasspathResourceSource source = ClasspathResourceSource.from(uri);

		assertThat(source).isNotNull();
		assertThat(source.getClasspathResourceName()).isEqualTo(FOO_RESOURCE);
		assertThat(source.getPosition()).hasValue(position);
	}

	@Test
	void resourceFromUriWithLineAndColumnNumbers() {
		FilePosition position = FilePosition.from(42, 23);
		URI uri = URI.create(FOO_RESOURCE_URI + "?line=42&foo=bar&column=23");
		ClasspathResourceSource source = ClasspathResourceSource.from(uri);

		assertThat(source).isNotNull();
		assertThat(source.getClasspathResourceName()).isEqualTo(FOO_RESOURCE);
		assertThat(source.getPosition()).hasValue(position);
	}

	@Test
	void equalsAndHashCode() {
		assertEqualsAndHashCode(ClasspathResourceSource.from(FOO_RESOURCE), ClasspathResourceSource.from(FOO_RESOURCE),
			ClasspathResourceSource.from(BAR_RESOURCE));

		FilePosition position = FilePosition.from(42, 23);
		assertEqualsAndHashCode(ClasspathResourceSource.from(FOO_RESOURCE, position),
			ClasspathResourceSource.from(FOO_RESOURCE, position), ClasspathResourceSource.from(BAR_RESOURCE, position));
	}

}
