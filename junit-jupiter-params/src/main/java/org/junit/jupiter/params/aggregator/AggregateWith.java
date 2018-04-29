/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params.aggregator;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;

/**
 * {@code @AggregateWith} is an annotation that allows one to specify an
 * {@link ArgumentsAggregator}.
 *
 * <p>This annotation may be applied to a parameter of a
 * {@link org.junit.jupiter.params.ParameterizedTest @ParameterizedTest} method
 * in order for an aggregated value to be resolved for the annotated parameter
 * when the test method is invoked.
 *
 * <p>{@code @AggregateWith} may also be used as a meta-annotation in order to
 * create a custom <em>composed annotation</em> that inherits the semantics
 * of {@code @AggregateWith}.
 *
 * @since 5.2
 * @see ArgumentsAggregator
 * @see org.junit.jupiter.params.ParameterizedTest
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER, ElementType.ANNOTATION_TYPE })
@Documented
@API(status = EXPERIMENTAL, since = "5.2")
public @interface AggregateWith {

	Class<? extends ArgumentsAggregator> value();

}
