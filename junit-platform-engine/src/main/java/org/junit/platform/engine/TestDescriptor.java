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

import static org.apiguardian.api.API.Status.STABLE;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.apiguardian.api.API;

/**
 * Mutable descriptor for a test or container that has been discovered by a
 * {@link TestEngine}.
 *
 * @see TestEngine
 * @since 1.0
 */
@API(status = STABLE, since = "1.0")
public interface TestDescriptor {

	/**
	 * Get the unique identifier (UID) for this descriptor.
	 *
	 * <p>Uniqueness must be guaranteed across an entire test plan,
	 * regardless of how many engines are used behind the scenes.
	 *
	 * @return the {@code UniqueId} for this descriptor; never {@code null}
	 */
	UniqueId getUniqueId();

	/**
	 * Get the display name for this descriptor.
	 *
	 * <p>A <em>display name</em> is a human-readable name for a test or
	 * container that is typically used for test reporting in IDEs and build
	 * tools. Display names may contain spaces, special characters, and emoji,
	 * and the format may be customized by {@link TestEngine TestEngines} or
	 * potentially by end users as well. Consequently, display names should
	 * never be parsed; rather, they should be used for display purposes only.
	 *
	 * @return the display name for this descriptor; never {@code null} or blank
	 * @see #getSource()
	 */
	String getDisplayName();

	/**
	 * Get the name of this descriptor in a format that is suitable for legacy
	 * reporting infrastructure &mdash; for example, for reporting systems built
	 * on the Ant-based XML reporting format for JUnit 4.
	 *
	 * <p>The default implementation simply delegates to {@link #getDisplayName()}.
	 *
	 * @return the legacy reporting name; never {@code null} or blank
	 */
	default String getLegacyReportingName() {
		return getDisplayName();
	}

	/**
	 * Get the set of {@linkplain TestTag tags} associated with this descriptor.
	 *
	 * @return the set of tags associated with this descriptor; never {@code null}
	 * but potentially empty
	 * @see TestTag
	 */
	Set<TestTag> getTags();

	/**
	 * Get the {@linkplain TestSource source} of the test or container described
	 * by this descriptor, if available.
	 *
	 * @see TestSource
	 */
	Optional<TestSource> getSource();

	/**
	 * Get the <em>parent</em> of this descriptor, if available.
	 */
	Optional<TestDescriptor> getParent();

	/**
	 * Set the <em>parent</em> of this descriptor.
	 *
	 * @param parent the new parent of this descriptor; may be {@code null}.
	 */
	void setParent(TestDescriptor parent);

	/**
	 * Get the immutable set of <em>children</em> of this descriptor.
	 *
	 * @return the set of children of this descriptor; neither {@code null}
	 * nor mutable, but potentially empty
	 * @see #getDescendants()
	 */
	Set<? extends TestDescriptor> getChildren();

	/**
	 * Get the immutable set of all <em>descendants</em> of this descriptor.
	 *
	 * <p>A <em>descendant</em> is a child of this descriptor or a child of one of
	 * its children, recursively.
	 *
	 * @see #getChildren()
	 */
	default Set<? extends TestDescriptor> getDescendants() {
		Set<TestDescriptor> descendants = new LinkedHashSet<>();
		descendants.addAll(getChildren());
		for (TestDescriptor child : getChildren()) {
			descendants.addAll(child.getDescendants());
		}
		return Collections.unmodifiableSet(descendants);
	}

	/**
	 * Add a <em>child</em> to this descriptor.
	 *
	 * @param descriptor the child to add to this descriptor; never {@code null}
	 */
	void addChild(TestDescriptor descriptor);

	/**
	 * Remove a <em>child</em> from this descriptor.
	 *
	 * @param descriptor the child to remove from this descriptor; never
	 * {@code null}
	 */
	void removeChild(TestDescriptor descriptor);

	/**
	 * Remove this non-root descriptor from its parent and remove all the
	 * children from this descriptor.
	 *
	 * <p>If this method is invoked on a {@linkplain #isRoot root} descriptor,
	 * this method must throw a {@link org.junit.platform.commons.JUnitException
	 * JUnitException} explaining that a root cannot be removed from the
	 * hierarchy.
	 */
	void removeFromHierarchy();

	/**
	 * Determine if this descriptor is a <em>root</em> descriptor.
	 *
	 * <p>A <em>root</em> descriptor is a descriptor without a parent.
	 */
	default boolean isRoot() {
		return !getParent().isPresent();
	}

	/**
	 * Determine the {@link Type} of this descriptor.
	 *
	 * @return the descriptor type; never {@code null}.
	 * @see #isContainer()
	 * @see #isTest()
	 */
	Type getType();

	/**
	 * Determine if this descriptor describes a container.
	 *
	 * <p>The default implementation delegates to {@link Type#isContainer()}.
	 */
	default boolean isContainer() {
		return getType().isContainer();
	}

	/**
	 * Determine if this descriptor describes a test.
	 *
	 * <p>The default implementation delegates to {@link Type#isTest()}.
	 */
	default boolean isTest() {
		return getType().isTest();
	}

	/**
	 * Determine if this descriptor may register dynamic tests during execution.
	 *
	 * <p>The default implementation assumes tests are usually known during
	 * discovery and thus returns {@code false}.
	 */
	default boolean mayRegisterTests() {
		return false;
	}

	/**
	 * Determine if the supplied descriptor or any of its descendants contains
	 * any tests.
	 */
	static boolean containsTests(TestDescriptor testDescriptor) {
		return testDescriptor.isTest() || testDescriptor.mayRegisterTests()
				|| testDescriptor.getChildren().stream().anyMatch(TestDescriptor::containsTests);
	}

	/**
	 * Remove this descriptor from the hierarchy unless it is a root or contains
	 * tests.
	 *
	 * <p>A concrete {@link TestEngine} may override this method in order to
	 * implement a different algorithm or to skip pruning altogether.
	 *
	 * @see #isRoot()
	 * @see #containsTests(TestDescriptor)
	 * @see #removeFromHierarchy()
	 */
	default void prune() {
		if (!isRoot() && !containsTests(this)) {
			removeFromHierarchy();
		}
	}

	/**
	 * Find the descriptor with the supplied unique ID.
	 *
	 * <p>The search algorithm begins with this descriptor and then searches
	 * through its descendants.
	 *
	 * @param uniqueId the {@code UniqueId} to search for; never {@code null}
	 */
	Optional<? extends TestDescriptor> findByUniqueId(UniqueId uniqueId);

	/**
	 * Accept a visitor to the subtree starting with this descriptor.
	 *
	 * @param visitor the {@code Visitor} to accept; never {@code null}
	 */
	default void accept(Visitor visitor) {
		visitor.visit(this);
		// Create a copy of the set in order to avoid a ConcurrentModificationException
		new LinkedHashSet<>(this.getChildren()).forEach(child -> child.accept(visitor));
	}

	/**
	 * Visitor for the tree-like {@link TestDescriptor} structure.
	 *
	 * @see TestDescriptor#accept
	 */
	@FunctionalInterface
	interface Visitor {

		/**
		 * Visit a {@link TestDescriptor}.
		 *
		 * @param descriptor the {@code TestDescriptor} to visit; never {@code null}
		 */
		void visit(TestDescriptor descriptor);
	}

	/**
	 * Supported types for {@link TestDescriptor TestDescriptors}.
	 */
	enum Type {

		/**
		 * Denotes that the {@link TestDescriptor} is for a <em>container</em>.
		 */
		CONTAINER,

		/**
		 * Denotes that the {@link TestDescriptor} is for a <em>test</em>.
		 */
		TEST,

		/**
		 * Denotes that the {@link TestDescriptor} is for a <em>test</em>
		 * that may potentially also be a <em>container</em>.
		 */
		CONTAINER_AND_TEST;

		/**
		 * @return {@code true} if this type represents a descriptor that can
		 * contain other descriptors
		 */
		public boolean isContainer() {
			return this == CONTAINER || this == CONTAINER_AND_TEST;
		}

		/**
		 * @return {@code true} if this type represents a descriptor for a test
		 */
		public boolean isTest() {
			return this == TEST || this == CONTAINER_AND_TEST;
		}

	}

}
