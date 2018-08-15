/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.descriptor;

import java.net.URI;

import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.StringUtils;

/**
 * Collection of static utility methods for working with resources.
 *
 * @since 1.3
 */
final class ResourceUtils {

	private ResourceUtils() {
		/* no-op */
	}

	/**
	 * Strip the {@link URI#getQuery() query} component from the supplied
	 * {@link URI}.
	 *
	 * @param uri the {@code URI} from which to strip the query component
	 * @return a new {@code URI} with the query component removed, or the
	 * original {@code URI} unmodified if it does not have a query component
	 */
	static URI stripQueryComponent(URI uri) {
		Preconditions.notNull(uri, "URI must not be null");

		if (StringUtils.isBlank(uri.getQuery())) {
			return uri;
		}

		String uriAsString = uri.toString();
		return URI.create(uriAsString.substring(0, uriAsString.indexOf('?')));
	}

}
