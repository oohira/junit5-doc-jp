/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.support;

import static org.apiguardian.api.API.Status.MAINTAINED;

import java.lang.reflect.Member;

import org.apiguardian.api.API;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ReflectionUtils;

/**
 * {@code ModifierSupport} provides static utility methods for working with
 * class and member {@linkplain java.lang.reflect.Modifier modifiers} &mdash;
 * for example, to determine if a class or member is declared as
 * {@code public}, {@code private}, {@code abstract}, {@code static}, etc.
 *
 * <p>{@link org.junit.platform.engine.TestEngine TestEngine} and extension
 * authors are encouraged to use these supported methods in order to align with
 * the behavior of the JUnit Platform.
 *
 * @since 1.4
 * @see java.lang.reflect.Modifier
 * @see AnnotationSupport
 * @see ClassSupport
 * @see ReflectionSupport
 */
@API(status = MAINTAINED, since = "1.4")
public final class ModifierSupport {

	private ModifierSupport() {
		/* no-op */
	}

	/**
	 * Determine if the supplied class is {@code public}.
	 *
	 * @param clazz the class to check; never {@code null}
	 * @return {@code true} if the class is {@code public}
	 * @see java.lang.reflect.Modifier#isPublic(int)
	 */
	public static boolean isPublic(Class<?> clazz) {
		Preconditions.notNull(clazz, "Class must not be null");
		return ReflectionUtils.isPublic(clazz);
	}

	/**
	 * Determine if the supplied member is {@code public}.
	 *
	 * @param member the member to check; never {@code null}
	 * @return {@code true} if the member is {@code public}
	 * @see java.lang.reflect.Modifier#isPublic(int)
	 */
	public static boolean isPublic(Member member) {
		Preconditions.notNull(member, "Member must not be null");
		return ReflectionUtils.isPublic(member);
	}

	/**
	 * Determine if the supplied class is {@code private}.
	 *
	 * @param clazz the class to check; never {@code null}
	 * @return {@code true} if the class is {@code private}
	 * @see java.lang.reflect.Modifier#isPrivate(int)
	 */
	public static boolean isPrivate(Class<?> clazz) {
		Preconditions.notNull(clazz, "Class must not be null");
		return ReflectionUtils.isPrivate(clazz);
	}

	/**
	 * Determine if the supplied member is {@code private}.
	 *
	 * @param member the member to check; never {@code null}
	 * @return {@code true} if the member is {@code private}
	 * @see java.lang.reflect.Modifier#isPrivate(int)
	 */
	public static boolean isPrivate(Member member) {
		Preconditions.notNull(member, "Member must not be null");
		return ReflectionUtils.isPrivate(member);
	}

	/**
	 * Determine if the supplied class is not {@code private}.
	 *
	 * <p>In other words this method will return {@code true} for classes
	 * declared as {@code public}, {@code protected}, or
	 * <em>package private</em> and {@code false} for classes declared as
	 * {@code private}.
	 *
	 * @param clazz the class to check; never {@code null}
	 * @return {@code true} if the class is not {@code private}
	 * @see java.lang.reflect.Modifier#isPublic(int)
	 * @see java.lang.reflect.Modifier#isProtected(int)
	 * @see java.lang.reflect.Modifier#isPrivate(int)
	 */
	public static boolean isNotPrivate(Class<?> clazz) {
		Preconditions.notNull(clazz, "Class must not be null");
		return ReflectionUtils.isNotPrivate(clazz);
	}

	/**
	 * Determine if the supplied member is not {@code private}.
	 *
	 * <p>In other words this method will return {@code true} for members
	 * declared as {@code public}, {@code protected}, or
	 * <em>package private</em> and {@code false} for members declared as
	 * {@code private}.
	 *
	 * @param member the member to check; never {@code null}
	 * @return {@code true} if the member is not {@code private}
	 * @see java.lang.reflect.Modifier#isPublic(int)
	 * @see java.lang.reflect.Modifier#isProtected(int)
	 * @see java.lang.reflect.Modifier#isPrivate(int)
	 */
	public static boolean isNotPrivate(Member member) {
		Preconditions.notNull(member, "Member must not be null");
		return ReflectionUtils.isNotPrivate(member);
	}

	/**
	 * Determine if the supplied class is {@code abstract}.
	 *
	 * @param clazz the class to check; never {@code null}
	 * @return {@code true} if the class is {@code abstract}
	 * @see java.lang.reflect.Modifier#isAbstract(int)
	 */
	public static boolean isAbstract(Class<?> clazz) {
		Preconditions.notNull(clazz, "Class must not be null");
		return ReflectionUtils.isAbstract(clazz);
	}

	/**
	 * Determine if the supplied member is {@code abstract}.
	 *
	 * @param member the class to check; never {@code null}
	 * @return {@code true} if the member is {@code abstract}
	 * @see java.lang.reflect.Modifier#isAbstract(int)
	 */
	public static boolean isAbstract(Member member) {
		Preconditions.notNull(member, "Member must not be null");
		return ReflectionUtils.isAbstract(member);
	}

	/**
	 * Determine if the supplied class is {@code static}.
	 *
	 * @param clazz the class to check; never {@code null}
	 * @return {@code true} if the class is {@code static}
	 * @see java.lang.reflect.Modifier#isStatic(int)
	 */
	public static boolean isStatic(Class<?> clazz) {
		Preconditions.notNull(clazz, "Class must not be null");
		return ReflectionUtils.isStatic(clazz);
	}

	/**
	 * Determine if the supplied member is {@code static}.
	 *
	 * @param member the member to check; never {@code null}
	 * @return {@code true} if the member is {@code static}
	 * @see java.lang.reflect.Modifier#isStatic(int)
	 */
	public static boolean isStatic(Member member) {
		Preconditions.notNull(member, "Member must not be null");
		return ReflectionUtils.isStatic(member);
	}

	/**
	 * Determine if the supplied class is not {@code static}.
	 *
	 * @param clazz the class to check; never {@code null}
	 * @return {@code true} if the class is not {@code static}
	 * @see java.lang.reflect.Modifier#isStatic(int)
	 */
	public static boolean isNotStatic(Class<?> clazz) {
		Preconditions.notNull(clazz, "Class must not be null");
		return ReflectionUtils.isNotStatic(clazz);
	}

	/**
	 * Determine if the supplied member is not {@code static}.
	 *
	 * @param member the member to check; never {@code null}
	 * @return {@code true} if the member is not {@code static}
	 * @see java.lang.reflect.Modifier#isStatic(int)
	 */
	public static boolean isNotStatic(Member member) {
		Preconditions.notNull(member, "Member must not be null");
		return ReflectionUtils.isNotStatic(member);
	}

}
