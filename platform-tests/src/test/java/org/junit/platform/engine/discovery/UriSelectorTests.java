/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.discovery;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.junit.platform.AbstractEqualsAndHashCodeTests;

/**
 * Unit tests for {@link UriSelector}.
 *
 * @since 1.3
 * @see DiscoverySelectorsTests
 */
class UriSelectorTests extends AbstractEqualsAndHashCodeTests {

	@Test
	void equalsAndHashCode() throws Exception {
		var selector1 = new UriSelector(new URI("http://junit.org"));
		var selector2 = new UriSelector(new URI("http://junit.org"));
		var selector3 = new UriSelector(new URI("http://example.org"));

		assertEqualsAndHashCode(selector1, selector2, selector3);
	}

}
