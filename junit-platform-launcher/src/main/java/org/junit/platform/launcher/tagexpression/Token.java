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

/**
 * @since 1.1
 */
class Token {

	final int startIndex;
	final String rawString;

	Token(int startIndex, String rawString) {
		this.startIndex = startIndex;
		this.rawString = rawString;
	}

	String string() {
		return rawString.trim();
	}

	public int trimmedTokenStartIndex() {
		return startIndex + rawString.indexOf(string());
	}

	public boolean isLeftOf(Token token) {
		return lastCharacterIndex() < token.startIndex;
	}

	public int lastCharacterIndex() {
		return endIndexExclusive() - 1;
	}

	public int endIndexExclusive() {
		return startIndex + rawString.length();
	}

	public Token concatenate(Token rightOfThis) {
		String concatenatedRawString = this.rawString + rightOfThis.rawString;
		return new Token(startIndex, concatenatedRawString);
	}

}
