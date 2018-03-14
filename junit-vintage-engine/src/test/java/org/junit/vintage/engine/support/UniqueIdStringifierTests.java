/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Base64;

import org.junit.jupiter.api.Test;

/**
 * @since 4.12
 */
class UniqueIdStringifierTests {

	@Test
	void returnsReadableStringForKnownTypes() {
		UniqueIdStringifier stringifier = new UniqueIdStringifier();

		assertEquals("foo", stringifier.apply("foo"));
		assertEquals("42", stringifier.apply(42));
		assertEquals("42", stringifier.apply(42L));
		assertEquals("42.23", stringifier.apply(42.23d));
	}

	@Test
	void serializesUnknownTypes() throws Exception {
		UniqueIdStringifier stringifier = new UniqueIdStringifier();

		String serialized = stringifier.apply(new MyCustomId(42));

		Object deserializedObject = deserialize(decodeBase64(serialized));
		assertThat(deserializedObject).isInstanceOf(MyCustomId.class);
		assertEquals(42, ((MyCustomId) deserializedObject).getValue());
	}

	@Test
	void usesToStringWhenSerializationFails() throws Exception {
		UniqueIdStringifier stringifier = new UniqueIdStringifier();
		String serialized = stringifier.apply(new ClassWithErroneousSerialization());

		String deserializedString = new String(decodeBase64(serialized), UniqueIdStringifier.CHARSET);

		assertEquals("value from toString()", deserializedString);
	}

	private byte[] decodeBase64(String value) {
		return Base64.getDecoder().decode(value.getBytes(UniqueIdStringifier.CHARSET));
	}

	private Object deserialize(byte[] bytes) throws Exception {
		try (ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
			return inputStream.readObject();
		}
	}

	private static class MyCustomId implements Serializable {

		private static final long serialVersionUID = 1L;

		private final int value;

		MyCustomId(int value) {
			this.value = value;
		}

		int getValue() {
			return value;
		}

	}

	private static class ClassWithErroneousSerialization implements Serializable {

		private static final long serialVersionUID = 1L;

		Object writeReplace() throws ObjectStreamException {
			throw new InvalidObjectException("failed on purpose");
		}

		@Override
		public String toString() {
			return "value from toString()";
		}
	}

}
