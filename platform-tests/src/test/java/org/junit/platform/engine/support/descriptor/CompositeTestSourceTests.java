/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.descriptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.PreconditionViolationException;
import org.junit.platform.engine.TestSource;

/**
 * Unit tests for {@link CompositeTestSource}.
 *
 * @since 1.0
 */
class CompositeTestSourceTests extends AbstractTestSourceTests {

	@Test
	void createCompositeTestSourceFromNullList() {
		assertThrows(PreconditionViolationException.class, () -> CompositeTestSource.from(null));
	}

	@Test
	void createCompositeTestSourceFromEmptyList() {
		assertThrows(PreconditionViolationException.class, () -> CompositeTestSource.from(Collections.emptyList()));
	}

	@Test
	void createCompositeTestSourceFromClassAndFileSources() {
		FileSource fileSource = FileSource.from(new File("example.test"));
		ClassSource classSource = ClassSource.from(getClass());
		List<TestSource> sources = new ArrayList<>(Arrays.asList(fileSource, classSource));
		CompositeTestSource compositeTestSource = CompositeTestSource.from(sources);

		assertThat(compositeTestSource.getSources().size()).isEqualTo(2);
		assertThat(compositeTestSource.getSources()).contains(fileSource, classSource);

		// Ensure the supplied sources list was defensively copied.
		sources.remove(1);
		assertThat(compositeTestSource.getSources().size()).isEqualTo(2);

		// Ensure the returned sources list is immutable.
		assertThrows(UnsupportedOperationException.class, () -> compositeTestSource.getSources().add(fileSource));
	}

	@Test
	void equalsAndHashCode() {
		List<TestSource> sources1 = Arrays.asList(ClassSource.from(Number.class));
		List<TestSource> sources2 = Arrays.asList(ClassSource.from(String.class));
		assertEqualsAndHashCode(CompositeTestSource.from(sources1), CompositeTestSource.from(sources1),
			CompositeTestSource.from(sources2));
	}

}
