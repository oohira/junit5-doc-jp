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

import java.io.Serializable;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.PreconditionViolationException;

/**
 * Unit tests for {@link ClassSource}.
 *
 * @since 1.0
 */
class ClassSourceTests extends AbstractTestSourceTests {

	@Override
	Stream<Serializable> createSerializableInstances() {
		return Stream.of( //
			ClassSource.from("class.source"), //
			ClassSource.from("class.and.position", FilePosition.from(1, 2)), //
			ClassSource.from(getClass()), //
			ClassSource.from(getClass(), FilePosition.from(1, 2)) //
		);
	}

	@Test
	void preconditions() {
		assertThrows(PreconditionViolationException.class, () -> ClassSource.from((String) null));
		assertThrows(PreconditionViolationException.class, () -> ClassSource.from("    "));
		assertThrows(PreconditionViolationException.class, () -> ClassSource.from((String) null, null));
		assertThrows(PreconditionViolationException.class, () -> ClassSource.from("    ", null));
		assertThrows(PreconditionViolationException.class, () -> ClassSource.from((Class<?>) null));
		assertThrows(PreconditionViolationException.class, () -> ClassSource.from((Class<?>) null, null));
	}

	@Test
	void classSourceFromName() {
		var testClassName = "com.unknown.mypackage.ClassByName";
		var source = ClassSource.from(testClassName);

		assertThat(source.getClassName()).isEqualTo(testClassName);
		assertThat(source.getPosition()).isEmpty();

		var exception = assertThrows(PreconditionViolationException.class, () -> source.getJavaClass());
		assertThat(exception).hasMessage("Could not load class with name: " + testClassName);
	}

	@Test
	void classSourceFromNameAndFilePosition() {
		var testClassName = "com.unknown.mypackage.ClassByName";
		var position = FilePosition.from(42, 23);
		var source = ClassSource.from(testClassName, position);

		assertThat(source.getClassName()).isEqualTo(testClassName);
		assertThat(source.getPosition()).isNotEmpty();
		assertThat(source.getPosition()).hasValue(position);
	}

	@Test
	void classSourceFromReference() {
		var testClass = getClass();
		var source = ClassSource.from(testClass);

		assertThat(source.getJavaClass()).isEqualTo(testClass);
		assertThat(source.getPosition()).isEmpty();
	}

	@Test
	void classSourceFromReferenceAndFilePosition() {
		var testClass = getClass();
		var position = FilePosition.from(42, 23);
		var source = ClassSource.from(testClass, position);

		assertThat(source.getJavaClass()).isEqualTo(testClass);
		assertThat(source.getPosition()).isNotEmpty();
		assertThat(source.getPosition()).hasValue(position);
	}

	@Test
	void equalsAndHashCodeForClassSourceFromName() {
		var name1 = String.class.getName();
		var name2 = Number.class.getName();

		assertEqualsAndHashCode(ClassSource.from(name1), ClassSource.from(name1), ClassSource.from(name2));
	}

	@Test
	void equalsAndHashCodeForClassSourceFromNameAndFilePosition() {
		var name1 = String.class.getName();
		var name2 = Number.class.getName();
		var position1 = FilePosition.from(42, 23);
		var position2 = FilePosition.from(1, 2);

		assertEqualsAndHashCode(ClassSource.from(name1, position1), ClassSource.from(name1, position1),
			ClassSource.from(name2, position1));
		assertEqualsAndHashCode(ClassSource.from(name1, position1), ClassSource.from(name1, position1),
			ClassSource.from(name1, position2));
	}

	@Test
	void equalsAndHashCodeForClassSourceFromReference() {
		var class1 = String.class;
		var class2 = Number.class;

		assertEqualsAndHashCode(ClassSource.from(class1), ClassSource.from(class1), ClassSource.from(class2));
	}

	@Test
	void equalsAndHashCodeForClassSourceFromReferenceAndFilePosition() {
		var class1 = String.class;
		var class2 = Number.class;
		var position1 = FilePosition.from(42, 23);
		var position2 = FilePosition.from(1, 2);

		assertEqualsAndHashCode(ClassSource.from(class1, position1), ClassSource.from(class1, position1),
			ClassSource.from(class2, position1));
		assertEqualsAndHashCode(ClassSource.from(class1, position1), ClassSource.from(class1, position1),
			ClassSource.from(class1, position2));
	}

}
