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

import java.io.File;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.PreconditionViolationException;

/**
 * Unit tests for {@link FileSource} and {@link DirectorySource}.
 *
 * @since 1.0
 */
class FileSystemSourceTests extends AbstractTestSourceTests {

	@Override
	Stream<FileSource> createSerializableInstances() {
		return Stream.of( //
			FileSource.from(new File("file.source")), //
			FileSource.from(new File("file.and.position"), FilePosition.from(42, 23)));
	}

	@Test
	void nullSourceFileOrDirectoryYieldsException() {
		assertThrows(PreconditionViolationException.class, () -> FileSource.from(null));
	}

	@Test
	void directory() throws Exception {
		File canonicalDir = new File(".").getCanonicalFile();
		File relativeDir = new File("..", canonicalDir.getName());

		DirectorySource source = DirectorySource.from(relativeDir);

		assertThat(source.getUri()).isEqualTo(canonicalDir.toURI());
		assertThat(source.getFile()).isEqualTo(canonicalDir);
	}

	@Test
	void fileWithoutPosition() throws Exception {
		File canonicalDir = new File(".").getCanonicalFile();
		File relativeDir = new File("..", canonicalDir.getName());
		File relativeFile = new File(relativeDir, "test.txt");
		File canonicalFile = relativeFile.getCanonicalFile();

		FileSource source = FileSource.from(relativeFile);

		assertThat(source.getUri()).isEqualTo(canonicalFile.toURI());
		assertThat(source.getFile()).isEqualTo(canonicalFile);
		assertThat(source.getPosition()).isEmpty();
	}

	@Test
	void fileWithPosition() {
		File file = new File("test.txt");
		FilePosition position = FilePosition.from(42, 23);
		FileSource source = FileSource.from(file, position);

		assertThat(source.getUri()).isEqualTo(file.getAbsoluteFile().toURI());
		assertThat(source.getFile()).isEqualTo(file.getAbsoluteFile());
		assertThat(source.getPosition()).hasValue(position);
	}

	@Test
	void equalsAndHashCodeForFileSource() {
		File file1 = new File("foo.txt");
		File file2 = new File("bar.txt");
		assertEqualsAndHashCode(FileSource.from(file1), FileSource.from(file1), FileSource.from(file2));

		FilePosition position = FilePosition.from(42, 23);
		assertEqualsAndHashCode(FileSource.from(file1, position), FileSource.from(file1, position),
			FileSource.from(file2, position));
	}

	@Test
	void equalsAndHashCodeForDirectorySource() {
		File dir1 = new File(".");
		File dir2 = new File("..");
		assertEqualsAndHashCode(DirectorySource.from(dir1), DirectorySource.from(dir1), DirectorySource.from(dir2));
	}

}
