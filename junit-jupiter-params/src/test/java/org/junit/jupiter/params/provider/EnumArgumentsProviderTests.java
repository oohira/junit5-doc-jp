/*
 * Copyright 2015-2019 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.EnumArgumentsProviderTests.EnumWithTwoConstants.BAR;
import static org.junit.jupiter.params.provider.EnumArgumentsProviderTests.EnumWithTwoConstants.FOO;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.junit.platform.commons.PreconditionViolationException;

/**
 * @since 5.0
 */
class EnumArgumentsProviderTests {

	@Test
	void providesAllEnumConstants() {
		Stream<Object[]> arguments = provideArguments(EnumWithTwoConstants.class);

		assertThat(arguments).containsExactly(new Object[] { FOO }, new Object[] { BAR });
	}

	@Test
	void provideSingleEnumConstant() {
		Stream<Object[]> arguments = provideArguments(EnumWithTwoConstants.class, "FOO");

		assertThat(arguments).containsExactly(new Object[] { FOO });
	}

	@Test
	void provideAllEnumConstantsWithNamingAll() {
		Stream<Object[]> arguments = provideArguments(EnumWithTwoConstants.class, "FOO", "BAR");

		assertThat(arguments).containsExactly(new Object[] { FOO }, new Object[] { BAR });
	}

	@Test
	void duplicateConstantNameIsDetected() {
		Exception exception = assertThrows(PreconditionViolationException.class,
			() -> provideArguments(EnumWithTwoConstants.class, "FOO", "BAR", "FOO"));
		assertThat(exception).hasMessageContaining("Duplicate enum constant name(s) found");
	}

	@Test
	void invalidConstantNameIsDetected() {
		Exception exception = assertThrows(PreconditionViolationException.class,
			() -> provideArguments(EnumWithTwoConstants.class, "FO0", "B4R"));
		assertThat(exception).hasMessageContaining("Invalid enum constant name(s) in");
	}

	@Test
	void invalidPatternIsDetected() {
		Exception exception = assertThrows(PreconditionViolationException.class,
			() -> provideArguments(EnumWithTwoConstants.class, Mode.MATCH_ALL, "(", ")"));
		assertThat(exception).hasMessageContaining("Pattern compilation failed");
	}

	enum EnumWithTwoConstants {
		FOO, BAR
	}

	private <E extends Enum<E>> Stream<Object[]> provideArguments(Class<E> enumClass, String... names) {
		return provideArguments(enumClass, Mode.INCLUDE, names);
	}

	private <E extends Enum<E>> Stream<Object[]> provideArguments(Class<E> enumClass, Mode mode, String... names) {
		EnumSource annotation = mock(EnumSource.class);
		when(annotation.value()).thenAnswer(invocation -> enumClass);
		when(annotation.mode()).thenAnswer(invocation -> mode);
		when(annotation.names()).thenAnswer(invocation -> names);
		when(annotation.toString()).thenReturn(String.format("@EnumSource(value=%s.class, mode=%s, names=%s)",
			enumClass.getSimpleName(), mode, Arrays.toString(names)));

		EnumArgumentsProvider provider = new EnumArgumentsProvider();
		provider.accept(annotation);
		return provider.provideArguments(null).map(Arguments::get);
	}

}
