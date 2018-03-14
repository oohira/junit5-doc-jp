/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params.converter;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

import org.apiguardian.api.API;
import org.junit.jupiter.api.extension.ParameterContext;

/**
 * {@code ArgumentConverter} is an abstraction that allows an input object to
 * be converted to an instance of a different class.
 *
 * <p>Such an {@code ArgumentConverter} is applied to the method parameter
 * of a {@link org.junit.jupiter.params.ParameterizedTest @ParameterizedTest}
 * method with the help of a
 * {@link org.junit.jupiter.params.converter.ConvertWith @ConvertWith} annotation.
 *
 * <p>See {@link SimpleArgumentConverter} in case your implementation only needs
 * to know about the target type instead of the complete
 * {@link ParameterContext}.
 *
 * <p>Implementations must provide a no-args constructor.
 *
 * @since 5.0
 * @see SimpleArgumentConverter
 * @see org.junit.jupiter.params.ParameterizedTest
 * @see org.junit.jupiter.params.converter.ConvertWith
 */
@API(status = EXPERIMENTAL, since = "5.0")
public interface ArgumentConverter {

	/**
	 * Convert the supplied {@code source} object according to the supplied
	 * {@code context}.
	 *
	 * @param source the source object to convert; may be {@code null}
	 * @param context the parameter context where the converted object will be
	 * used; never {@code null}
	 * @return the converted object; may be {@code null} but only if the target
	 * type is a reference type
	 * @throws ArgumentConversionException if an error occurs during the
	 * conversion
	 */
	Object convert(Object source, ParameterContext context) throws ArgumentConversionException;

}
