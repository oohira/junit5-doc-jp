/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.launcher.tagexpression;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class TokenTests {

	@Test
	void startIndexOfTokenString() {
		assertThat(new Token(0, "!").trimmedTokenStartIndex()).isEqualTo(0);
		assertThat(new Token(0, "  !").trimmedTokenStartIndex()).isEqualTo(2);
		assertThat(new Token(7, "!").trimmedTokenStartIndex()).isEqualTo(7);
	}

	@Test
	void endIndexExclusive() {
		assertThat(new Token(0, "!").endIndexExclusive()).isEqualTo(1);
		assertThat(new Token(0, "  !").endIndexExclusive()).isEqualTo(3);
		assertThat(new Token(7, "!").endIndexExclusive()).isEqualTo(8);
	}

	@Test
	void lastCharacterIndex() {
		assertThat(new Token(0, "!").lastCharacterIndex()).isEqualTo(0);
		assertThat(new Token(0, "  !").lastCharacterIndex()).isEqualTo(2);
		assertThat(new Token(7, "!").lastCharacterIndex()).isEqualTo(7);
	}

	@Test
	void concatenateTwoTokens() {
		List<Token> tokens = new Tokenizer().tokenize(" ! foo");
		Token one = tokens.get(0);
		Token two = tokens.get(1);
		Token joined = one.concatenate(two);
		assertThat(joined.rawString).isEqualTo(" ! foo");
		assertThat(joined.startIndex).isEqualTo(0);
	}

}
