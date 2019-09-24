/*
 * Copyright 2015-2019 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.discovery;

import static java.util.stream.Collectors.joining;
import static org.junit.platform.commons.util.BlacklistedExceptions.rethrowIfBlacklisted;
import static org.junit.platform.commons.util.CollectionUtils.getOnlyElement;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.ClasspathResourceSelector;
import org.junit.platform.engine.discovery.ClasspathRootSelector;
import org.junit.platform.engine.discovery.DirectorySelector;
import org.junit.platform.engine.discovery.FileSelector;
import org.junit.platform.engine.discovery.MethodSelector;
import org.junit.platform.engine.discovery.ModuleSelector;
import org.junit.platform.engine.discovery.PackageSelector;
import org.junit.platform.engine.discovery.UniqueIdSelector;
import org.junit.platform.engine.discovery.UriSelector;
import org.junit.platform.engine.support.discovery.SelectorResolver.Context;
import org.junit.platform.engine.support.discovery.SelectorResolver.Match;
import org.junit.platform.engine.support.discovery.SelectorResolver.Resolution;

/**
 * @since 1.5
 */
class EngineDiscoveryRequestResolution {

	private final Logger logger;
	private final EngineDiscoveryRequest request;
	private final Context defaultContext;
	private final List<SelectorResolver> resolvers;
	private final List<TestDescriptor.Visitor> visitors;
	private final TestDescriptor engineDescriptor;
	private final Map<DiscoverySelector, Resolution> resolvedSelectors = new LinkedHashMap<>();
	private final Map<UniqueId, Match> resolvedUniqueIds = new LinkedHashMap<>();
	private final Queue<DiscoverySelector> remainingSelectors = new ArrayDeque<>();
	private final Map<DiscoverySelector, Context> contextBySelector = new HashMap<>();

	EngineDiscoveryRequestResolution(Logger logger, EngineDiscoveryRequest request, TestDescriptor engineDescriptor,
			List<SelectorResolver> resolvers, List<TestDescriptor.Visitor> visitors) {
		this.logger = logger;
		this.request = request;
		this.engineDescriptor = engineDescriptor;
		this.resolvers = resolvers;
		this.visitors = visitors;
		this.defaultContext = new DefaultContext(null);
		this.resolvedUniqueIds.put(engineDescriptor.getUniqueId(), Match.exact(engineDescriptor));
	}

	void run() {
		remainingSelectors.addAll(request.getSelectorsByType(DiscoverySelector.class));
		while (!remainingSelectors.isEmpty()) {
			resolveCompletely(remainingSelectors.poll());
		}
		visitors.forEach(engineDescriptor::accept);
	}

	private void resolveCompletely(DiscoverySelector selector) {
		try {
			Optional<Resolution> result = resolve(selector);
			if (result.isPresent()) {
				enqueueAdditionalSelectors(result.get());
			}
			else {
				logUnresolvedSelector(selector, null);
			}
		}
		catch (Throwable t) {
			rethrowIfBlacklisted(t);
			logUnresolvedSelector(selector, t);
		}
	}

	private void enqueueAdditionalSelectors(Resolution resolution) {
		remainingSelectors.addAll(resolution.getSelectors());
		resolution.getMatches().stream().filter(Match::isExact).forEach(match -> {
			Set<? extends DiscoverySelector> childSelectors = match.expand();
			if (!childSelectors.isEmpty()) {
				remainingSelectors.addAll(childSelectors);
				DefaultContext context = new DefaultContext(match.getTestDescriptor());
				childSelectors.forEach(selector -> contextBySelector.put(selector, context));
			}
		});
	}

	private Optional<Resolution> resolve(DiscoverySelector selector) {
		if (resolvedSelectors.containsKey(selector)) {
			return Optional.of(resolvedSelectors.get(selector));
		}
		if (selector instanceof UniqueIdSelector) {
			return resolveUniqueId((UniqueIdSelector) selector);
		}
		return resolve(selector, resolver -> {
			Context context = getContext(selector);
			if (selector instanceof ClasspathResourceSelector) {
				return resolver.resolve((ClasspathResourceSelector) selector, context);
			}
			if (selector instanceof ClasspathRootSelector) {
				return resolver.resolve((ClasspathRootSelector) selector, context);
			}
			if (selector instanceof ClassSelector) {
				return resolver.resolve((ClassSelector) selector, context);
			}
			if (selector instanceof DirectorySelector) {
				return resolver.resolve((DirectorySelector) selector, context);
			}
			if (selector instanceof FileSelector) {
				return resolver.resolve((FileSelector) selector, context);
			}
			if (selector instanceof MethodSelector) {
				return resolver.resolve((MethodSelector) selector, context);
			}
			if (selector instanceof ModuleSelector) {
				return resolver.resolve((ModuleSelector) selector, context);
			}
			if (selector instanceof PackageSelector) {
				return resolver.resolve((PackageSelector) selector, context);
			}
			if (selector instanceof UriSelector) {
				return resolver.resolve((UriSelector) selector, context);
			}
			return resolver.resolve(selector, context);
		});
	}

	private Optional<Resolution> resolveUniqueId(UniqueIdSelector selector) {
		UniqueId uniqueId = selector.getUniqueId();
		if (resolvedUniqueIds.containsKey(uniqueId)) {
			return Optional.of(Resolution.match(resolvedUniqueIds.get(uniqueId)));
		}
		if (!uniqueId.hasPrefix(engineDescriptor.getUniqueId())) {
			return Optional.empty();
		}
		return resolve(selector, resolver -> resolver.resolve(selector, getContext(selector)));
	}

	private Context getContext(DiscoverySelector selector) {
		return contextBySelector.getOrDefault(selector, defaultContext);
	}

	private Optional<Resolution> resolve(DiscoverySelector selector,
			Function<SelectorResolver, Resolution> resolutionFunction) {
		// @formatter:off
		return resolvers.stream()
				.map(resolutionFunction)
				.filter(Resolution::isResolved)
				.findFirst()
				.map(resolution -> {
					contextBySelector.remove(selector);
					resolvedSelectors.put(selector, resolution);
					resolution.getMatches()
							.forEach(match -> resolvedUniqueIds.put(match.getTestDescriptor().getUniqueId(), match));
					return resolution;
				});
		// @formatter:on
	}

	private void logUnresolvedSelector(DiscoverySelector selector, Throwable cause) {
		BiConsumer<Throwable, Supplier<String>> loggingConsumer = logger::debug;
		if (selector instanceof UniqueIdSelector) {
			UniqueId uniqueId = ((UniqueIdSelector) selector).getUniqueId();
			if (uniqueId.hasPrefix(engineDescriptor.getUniqueId())) {
				loggingConsumer = logger::warn;
			}
			else {
				return;
			}
		}
		loggingConsumer.accept(cause, () -> selector + " could not be resolved.");
	}

	private class DefaultContext implements Context {

		private final TestDescriptor parent;

		DefaultContext(TestDescriptor parent) {
			this.parent = parent;
		}

		@Override
		public <T extends TestDescriptor> Optional<T> addToParent(Function<TestDescriptor, Optional<T>> creator) {
			if (parent != null) {
				return createAndAdd(parent, creator);
			}
			return createAndAdd(engineDescriptor, creator);
		}

		@Override
		public <T extends TestDescriptor> Optional<T> addToParent(Supplier<DiscoverySelector> parentSelectorSupplier,
				Function<TestDescriptor, Optional<T>> creator) {
			if (parent != null) {
				return createAndAdd(parent, creator);
			}
			return resolve(parentSelectorSupplier.get()).flatMap(parent -> createAndAdd(parent, creator));
		}

		@Override
		public Optional<TestDescriptor> resolve(DiscoverySelector selector) {
			// @formatter:off
			return EngineDiscoveryRequestResolution.this.resolve(selector)
					.map(Resolution::getMatches)
					.flatMap(matches -> {
						if (matches.size() > 1) {
							String stringRepresentation = matches.stream()
									.map(Match::getTestDescriptor)
									.map(Objects::toString)
									.collect(joining(", "));
							throw new JUnitException(
								"Selector " + selector + " did not yield unique test descriptor: " + stringRepresentation);
						}
						if (matches.size() == 1) {
							return Optional.of(getOnlyElement(matches).getTestDescriptor());
						}
						return Optional.empty();
					});
			// @formatter:on
		}

		@SuppressWarnings("unchecked")
		private <T extends TestDescriptor> Optional<T> createAndAdd(TestDescriptor parent,
				Function<TestDescriptor, Optional<T>> creator) {
			Optional<T> child = creator.apply(parent);
			if (child.isPresent()) {
				UniqueId uniqueId = child.get().getUniqueId();
				if (resolvedUniqueIds.containsKey(uniqueId)) {
					return Optional.of((T) resolvedUniqueIds.get(uniqueId).getTestDescriptor());
				}
				parent.addChild(child.get());
			}
			return child;
		}

	}

}
