/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.UniqueId.Segment;

/**
 * Used to {@link #parse} a {@link UniqueId} from a string representation
 * or to {@link #format} a {@link UniqueId} into a string representation.
 *
 * @since 1.0
 */
class UniqueIdFormat implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final UniqueIdFormat defaultFormat = new UniqueIdFormat('[', ':', ']', '/');

	static UniqueIdFormat getDefault() {
		return defaultFormat;
	}

	private static String quote(char c) {
		return Pattern.quote(String.valueOf(c));
	}

	private static String encode(char c) {
		try {
			return URLEncoder.encode(String.valueOf(c), "UTF-8");
		}
		catch (UnsupportedEncodingException e) {
			throw new AssertionError("UTF-8 should be supported", e);
		}
	}

	private final char openSegment;
	private final char closeSegment;
	private final char segmentDelimiter;
	private final char typeValueSeparator;
	private final Pattern segmentPattern;
	private final Map<Character, String> encodedCharacterMap = new TreeMap<>();

	UniqueIdFormat(char openSegment, char typeValueSeparator, char closeSegment, char segmentDelimiter) {
		this.openSegment = openSegment;
		this.typeValueSeparator = typeValueSeparator;
		this.closeSegment = closeSegment;
		this.segmentDelimiter = segmentDelimiter;
		this.segmentPattern = Pattern.compile(
			String.format("%s(.+)%s(.+)%s", quote(openSegment), quote(typeValueSeparator), quote(closeSegment)),
			Pattern.DOTALL);

		// Compute "forbidden" character encoding map.
		// Note that the map is always empty at this point. Thus the use of
		// computeIfAbsent() is purely syntactic sugar.
		encodedCharacterMap.computeIfAbsent('%', UniqueIdFormat::encode);
		encodedCharacterMap.computeIfAbsent('+', UniqueIdFormat::encode);
		encodedCharacterMap.computeIfAbsent(openSegment, UniqueIdFormat::encode);
		encodedCharacterMap.computeIfAbsent(typeValueSeparator, UniqueIdFormat::encode);
		encodedCharacterMap.computeIfAbsent(closeSegment, UniqueIdFormat::encode);
		encodedCharacterMap.computeIfAbsent(segmentDelimiter, UniqueIdFormat::encode);
	}

	/**
	 * Parse a {@code UniqueId} from the supplied string representation.
	 *
	 * @return a properly constructed {@code UniqueId}
	 * @throws JUnitException if the string cannot be parsed
	 */
	UniqueId parse(String source) throws JUnitException {
		String[] parts = source.split(String.valueOf(this.segmentDelimiter));
		List<Segment> segments = Arrays.stream(parts).map(this::createSegment).collect(toList());
		return new UniqueId(this, segments);
	}

	private Segment createSegment(String segmentString) throws JUnitException {
		Matcher segmentMatcher = this.segmentPattern.matcher(segmentString);
		if (!segmentMatcher.matches()) {
			throw new JUnitException(String.format("'%s' is not a well-formed UniqueId segment", segmentString));
		}
		String type = decode(checkAllowed(segmentMatcher.group(1)));
		String value = decode(checkAllowed(segmentMatcher.group(2)));
		return new Segment(type, value);
	}

	private String checkAllowed(String typeOrValue) {
		checkDoesNotContain(typeOrValue, this.segmentDelimiter);
		checkDoesNotContain(typeOrValue, this.typeValueSeparator);
		checkDoesNotContain(typeOrValue, this.openSegment);
		checkDoesNotContain(typeOrValue, this.closeSegment);
		return typeOrValue;
	}

	private void checkDoesNotContain(String typeOrValue, char forbiddenCharacter) {
		Preconditions.condition(typeOrValue.indexOf(forbiddenCharacter) < 0,
			() -> String.format("type or value '%s' must not contain '%s'", typeOrValue, forbiddenCharacter));
	}

	/**
	 * Format and return the string representation of the supplied {@code UniqueId}.
	 */
	String format(UniqueId uniqueId) {
		// @formatter:off
		return uniqueId.getSegments().stream()
			.map(this::describe)
			.collect(joining(String.valueOf(this.segmentDelimiter)));
		// @formatter:on
	}

	private String describe(Segment segment) {
		String body = encode(segment.getType()) + typeValueSeparator + encode(segment.getValue());
		return openSegment + body + closeSegment;
	}

	private String encode(String s) {
		StringBuilder builder = new StringBuilder();
		for (char c : s.toCharArray()) {
			String value = encodedCharacterMap.get(c);
			if (value == null) {
				builder.append(c);
				continue;
			}
			builder.append(value);
		}
		return builder.toString();
	}

	private String decode(String s) {
		try {
			return URLDecoder.decode(s, "UTF-8");
		}
		catch (UnsupportedEncodingException e) {
			throw new JUnitException("UTF-8 should be supported", e);
		}
	}

}
