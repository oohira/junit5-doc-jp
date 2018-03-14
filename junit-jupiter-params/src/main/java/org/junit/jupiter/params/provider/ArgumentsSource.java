/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params.provider;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;

/**
 * {@code @ArgumentsSource} is a {@linkplain Repeatable repeatable} annotation
 * that is used to register {@linkplain ArgumentsProvider argument providers}
 * for the annotated test method.
 *
 * <p>{@code @ArgumentsSource} may also be used as a meta-annotation in order to
 * create a custom <em>composed annotation</em> that inherits the semantics
 * of {@code @ArgumentsSource}.
 *
 * @since 5.0
 * @see org.junit.jupiter.params.provider.ArgumentsProvider
 */
@Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(ArgumentsSources.class)
@API(status = EXPERIMENTAL, since = "5.0")
public @interface ArgumentsSource {

	/**
	 * The type of {@link ArgumentsProvider} to be used.
	 */
	Class<? extends ArgumentsProvider> value();

}
