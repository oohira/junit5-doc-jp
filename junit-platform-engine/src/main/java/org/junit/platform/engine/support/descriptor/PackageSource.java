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

import static org.apiguardian.api.API.Status.STABLE;

import java.util.Objects;

import org.apiguardian.api.API;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ToStringBuilder;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.discovery.PackageSelector;

/**
 * Package based {@link org.junit.platform.engine.TestSource}.
 *
 * <p>This class stores the package name because {@link Package} does not
 * implement {@link java.io.Serializable}.
 *
 * @since 1.0
 * @see PackageSelector
 */
@API(status = STABLE, since = "1.0")
public class PackageSource implements TestSource {

	private static final long serialVersionUID = 1L;

	/**
	 * Create a new {@code PackageSource} using the supplied Java {@link Package}.
	 *
	 * @param javaPackage the Java package; must not be {@code null}
	 */
	public static PackageSource from(Package javaPackage) {
		return new PackageSource(javaPackage);
	}

	/**
	 * Create a new {@code PackageSource} using the supplied {@code packageName}.
	 *
	 * @param packageName the package name; must not be {@code null} or blank
	 */
	public static PackageSource from(String packageName) {
		return new PackageSource(packageName);
	}

	private final String packageName;

	private PackageSource(Package javaPackage) {
		this(Preconditions.notNull(javaPackage, "package must not be null").getName());
	}

	private PackageSource(String packageName) {
		this.packageName = Preconditions.notBlank(packageName, "package name must not be null or blank");
	}

	/**
	 * Get the package name of this test source.
	 */
	public final String getPackageName() {
		return this.packageName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		PackageSource that = (PackageSource) o;
		return Objects.equals(this.packageName, that.packageName);
	}

	@Override
	public int hashCode() {
		return this.packageName.hashCode();
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("packageName", this.packageName).toString();
	}

}
