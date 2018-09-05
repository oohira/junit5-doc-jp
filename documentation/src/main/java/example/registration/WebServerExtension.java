/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package example.registration;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class WebServerExtension implements BeforeAllCallback {

	@Override
	public void beforeAll(ExtensionContext context) {
		/* no-op for demo */
	}

	public String getServerUrl() {
		return "http://example.org:8181";
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		public Builder enableSecurity(boolean b) {
			return this;
		}

		public WebServerExtension build() {
			return new WebServerExtension();
		}

	}

}
