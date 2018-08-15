/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.condition;

import static org.apiguardian.api.API.Status.STABLE;

import java.lang.reflect.Method;

import org.apiguardian.api.API;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.commons.util.StringUtils;

/**
 * Enumeration of Java Runtime Environment (JRE) versions.
 *
 * <p>If the current JRE version cannot be detected &mdash; for example, if the
 * {@code java.version} JVM system property is undefined &mdash; then none of
 * the constants defined in this enum will be considered to be the
 * {@linkplain #isCurrentVersion current JRE version}.
 *
 * @since 5.1
 * @see #JAVA_8
 * @see #JAVA_9
 * @see #JAVA_10
 * @see #JAVA_11
 * @see #OTHER
 * @see EnabledOnJre
 * @see DisabledOnJre
 */
@API(status = STABLE, since = "5.1")
public enum JRE {

	/**
	 * Java 8.
	 */
	JAVA_8,

	/**
	 * Java 9.
	 */
	JAVA_9,

	/**
	 * Java 10.
	 */
	JAVA_10,

	/**
	 * Java 11.
	 */
	JAVA_11,

	/**
	 * A JRE version other than {@link #JAVA_8}, {@link #JAVA_9},
	 * {@link #JAVA_10}, or {@link #JAVA_11}.
	 */
	OTHER;

	private static final Logger logger = LoggerFactory.getLogger(JRE.class);

	private static final JRE CURRENT_VERSION = determineCurrentVersion();

	private static JRE determineCurrentVersion() {
		String javaVersion = System.getProperty("java.version");
		boolean javaVersionIsBlank = StringUtils.isBlank(javaVersion);

		if (javaVersionIsBlank) {
			logger.debug(
				() -> "JVM system property 'java.version' is undefined. It is therefore not possible to detect Java 8.");
		}

		if (!javaVersionIsBlank && javaVersion.startsWith("1.8")) {
			return JAVA_8;
		}

		try {
			// java.lang.Runtime.version() is a static method available on Java 9+
			// that returns an instance of java.lang.Runtime.Version which has the
			// following method: public int major()
			Method versionMethod = Runtime.class.getMethod("version");
			Object version = ReflectionUtils.invokeMethod(versionMethod, null);
			Method majorMethod = version.getClass().getMethod("major");
			int major = (int) ReflectionUtils.invokeMethod(majorMethod, version);
			switch (major) {
				case 9:
					return JAVA_9;
				case 10:
					return JAVA_10;
				case 11:
					return JAVA_11;
				default:
					return OTHER;
			}
		}
		catch (Exception ex) {
			logger.debug(ex, () -> "Failed to determine the current JRE version via java.lang.Runtime.Version.");
		}

		// null signals that the current JRE version is "unknown"
		return null;
	}

	/**
	 * @return {@code true} if <em>this</em> {@code JRE} is known to be the
	 * Java Runtime Environment version for the currently executing JVM
	 */
	public boolean isCurrentVersion() {
		return this == CURRENT_VERSION;
	}

}
