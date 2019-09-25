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

import static java.util.stream.Collectors.toCollection;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apiguardian.api.API;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.DiscoveryFilter;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.Filter;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.discovery.ClassNameFilter;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.ClasspathRootSelector;
import org.junit.platform.engine.discovery.ModuleSelector;
import org.junit.platform.engine.discovery.PackageNameFilter;
import org.junit.platform.engine.discovery.PackageSelector;
import org.junit.platform.engine.support.discovery.SelectorResolver.Match;
import org.junit.platform.engine.support.discovery.SelectorResolver.Resolution;

/**
 * Configurable test discovery implementation based on {@link SelectorResolver}
 * and {@link TestDescriptor.Visitor} that can be reused by different
 * {@link org.junit.platform.engine.TestEngine TestEngines}.
 *
 * @param <T> the type of the engine's descriptor
 * @see #builder()
 * @see #resolve(EngineDiscoveryRequest, TestDescriptor)
 * @since 1.5
 */
@API(status = EXPERIMENTAL, since = "1.5")
public class EngineDiscoveryRequestResolver<T extends TestDescriptor> {

	private static final Logger logger = LoggerFactory.getLogger(EngineDiscoveryRequestResolver.class);

	private final List<Function<InitializationContext<T>, SelectorResolver>> resolverCreators;
	private final List<Function<InitializationContext<T>, TestDescriptor.Visitor>> visitorCreators;

	private EngineDiscoveryRequestResolver(List<Function<InitializationContext<T>, SelectorResolver>> resolverCreators,
			List<Function<InitializationContext<T>, TestDescriptor.Visitor>> visitorCreators) {
		this.resolverCreators = new ArrayList<>(resolverCreators);
		this.visitorCreators = new ArrayList<>(visitorCreators);
	}

	/**
	 * Resolve the supplied {@link EngineDiscoveryRequest} and collect the
	 * results into the supplied {@link TestDescriptor}.
	 *
	 * <p>The algorithm works as follows:
	 *
	 * <ol>
	 *     <li>Enqueue all selectors in the supplied
	 *     {@linkplain EngineDiscoveryRequest request} to be resolved.
	 *     </li>
	 *     <li>
	 *         While there are selectors to be resolved, get the next one.
	 *         Otherwise, the resolution is finished.
	 *         <ol>
	 *             <li>
	 *                 Iterate over all registered {@linkplain SelectorResolver
	 *                 resolvers} in the order they were registered in and find the
	 *                 first one that returns a {@linkplain Resolution resolution}
	 *                 other than {@link Resolution#unresolved() unresolved()}.
	 *             </li>
	 *             <li>
	 *                 If such a {@linkplain Resolution resolution} exists, enqueue
	 *                 its {@linkplain Resolution#getSelectors() selectors}.
	 *             </li>
	 *             <li>
	 *                 For each exact {@linkplain Match match} in the {@linkplain
	 *                 Resolution resolution}, {@linkplain Match#expand() expand}
	 *                 its children and enqueue them as well.
	 *             </li>
	 *         </ol>
	 *     </li>
	 *     <li>
	 *         Iterate over all registered {@linkplain TestDescriptor.Visitor
	 *         visitors} and let the engine test descriptor {@linkplain
	 *         TestDescriptor#accept(TestDescriptor.Visitor) accept} them.
	 *     </li>
	 * </ol>
	 *
	 * @param request the request to be resolved; never {@code null}
	 * @param engineDescriptor the engine's {@code TestDescriptor} to be used
	 * for adding direct children
	 * @see SelectorResolver
	 * @see TestDescriptor.Visitor
	 */
	public void resolve(EngineDiscoveryRequest request, T engineDescriptor) {
		Preconditions.notNull(request, "request must not be null");
		Preconditions.notNull(engineDescriptor, "engineDescriptor must not be null");
		InitializationContext<T> initializationContext = new DefaultInitializationContext<>(request, engineDescriptor);
		List<SelectorResolver> resolvers = instantiate(resolverCreators, initializationContext);
		List<TestDescriptor.Visitor> visitors = instantiate(visitorCreators, initializationContext);
		new EngineDiscoveryRequestResolution(logger, request, engineDescriptor, resolvers, visitors).run();
	}

	private <R> List<R> instantiate(List<Function<InitializationContext<T>, R>> creators,
			InitializationContext<T> context) {
		return creators.stream().map(creator -> creator.apply(context)).collect(toCollection(ArrayList::new));
	}

	/**
	 * Create a new {@link Builder} for creating a {@link EngineDiscoveryRequestResolver}.
	 *
	 * @param <T> the type of the engine's descriptor
	 * @return a new builder; never {@code null}
	 */
	public static <T extends TestDescriptor> Builder<T> builder() {
		return new Builder<>();
	}

	/**
	 * Builder for {@link EngineDiscoveryRequestResolver}.
	 *
	 * @param <T> the type of the engine's descriptor
	 * @since 1.5
	 */
	@API(status = EXPERIMENTAL, since = "1.5")
	public static class Builder<T extends TestDescriptor> {

		private final List<Function<InitializationContext<T>, SelectorResolver>> resolverCreators = new ArrayList<>();
		private final List<Function<InitializationContext<T>, TestDescriptor.Visitor>> visitorCreators = new ArrayList<>();

		private Builder() {
		}

		/**
		 * Add a predefined resolver that resolves {@link ClasspathRootSelector
		 * ClasspathRootSelectors}, {@link ModuleSelector ModuleSelectors}, and
		 * {@link PackageSelector PackageSelectors} into {@link ClassSelector
		 * ClassSelectors} by scanning for classes that satisfy the supplied
		 * predicate in the respective class containers to this builder.
		 *
		 * @param classFilter predicate the resolved classes must satisfy; never
		 * {@code null}
		 * @return this builder for method chaining
		 */
		public Builder<T> addClassContainerSelectorResolver(Predicate<Class<?>> classFilter) {
			Preconditions.notNull(classFilter, "classFilter must not be null");
			return addSelectorResolver(
				context -> new ClassContainerSelectorResolver(classFilter, context.getClassNameFilter()));
		}

		/**
		 * Add a context insensitive {@link SelectorResolver} to this builder.
		 *
		 * @param resolver the resolver to add; never {@code null}
		 * @return this builder for method chaining
		 */
		public Builder<T> addSelectorResolver(SelectorResolver resolver) {
			Preconditions.notNull(resolver, "resolver must not be null");
			return addSelectorResolver(context -> resolver);
		}

		/**
		 * Add a context sensitive {@link SelectorResolver} to this builder.
		 *
		 * @param resolverCreator the function that will be called to create the
		 * {@link SelectorResolver} to be added.
		 * @return this builder for method chaining
		 * @see InitializationContext
		 */
		public Builder<T> addSelectorResolver(Function<InitializationContext<T>, SelectorResolver> resolverCreator) {
			resolverCreators.add(resolverCreator);
			return this;
		}

		/**
		 * Add a context sensitive {@link TestDescriptor.Visitor} to this
		 * builder.
		 *
		 * @param visitorCreator the function that will be called to create the
		 * {@link TestDescriptor.Visitor} to be added.
		 * @return this builder for method chaining
		 * @see InitializationContext
		 */
		public Builder<T> addTestDescriptorVisitor(
				Function<InitializationContext<T>, TestDescriptor.Visitor> visitorCreator) {
			visitorCreators.add(visitorCreator);
			return this;
		}

		/**
		 * Build the {@link EngineDiscoveryRequestResolver} that has been
		 * configured via this builder.
		 */
		public EngineDiscoveryRequestResolver<T> build() {
			return new EngineDiscoveryRequestResolver<>(resolverCreators, visitorCreators);
		}

	}

	/**
	 * The initialization context for creating {@linkplain SelectorResolver
	 * resolvers} and {@linkplain TestDescriptor.Visitor visitors} that depend
	 * on the {@link EngineDiscoveryRequest} to be resolved or the engine
	 * descriptor that will be used to collect the results.
	 *
	 * @since 1.5
	 * @see Builder#addSelectorResolver(Function)
	 * @see Builder#addTestDescriptorVisitor(Function)
	 */
	@API(status = EXPERIMENTAL, since = "1.5")
	public interface InitializationContext<T extends TestDescriptor> {

		/**
		 * Get the {@link EngineDiscoveryRequest} that is about to be resolved.
		 *
		 * @return the {@link EngineDiscoveryRequest}; never {@code null}
		 */
		EngineDiscoveryRequest getDiscoveryRequest();

		/**
		 * Get the engine's {@link TestDescriptor} that will be used to collect
		 * the results.
		 *
		 * @return engine's {@link TestDescriptor}; never {@code null}
		 */
		T getEngineDescriptor();

		/**
		 * Get the class name filter built from the {@link ClassNameFilter
		 * ClassNameFilters} and {@link PackageNameFilter PackageNameFilters}
		 * in the {@link EngineDiscoveryRequest} that is about to be resolved.
		 *
		 * @return the predicate for filtering the resolved class names; never
		 * {@code null}
		 */
		Predicate<String> getClassNameFilter();

	}

	private static class DefaultInitializationContext<T extends TestDescriptor> implements InitializationContext<T> {

		private final EngineDiscoveryRequest request;
		private final T engineDescriptor;
		private final Predicate<String> classNameFilter;

		DefaultInitializationContext(EngineDiscoveryRequest request, T engineDescriptor) {
			this.request = request;
			this.engineDescriptor = engineDescriptor;
			this.classNameFilter = buildClassNamePredicate(request);
		}

		/**
		 * Build a {@link Predicate} for fully qualified class names to be used for
		 * classpath scanning from an {@link EngineDiscoveryRequest}.
		 *
		 * @param request the request to build a predicate from
		 */
		private Predicate<String> buildClassNamePredicate(EngineDiscoveryRequest request) {
			List<DiscoveryFilter<String>> filters = new ArrayList<>();
			filters.addAll(request.getFiltersByType(ClassNameFilter.class));
			filters.addAll(request.getFiltersByType(PackageNameFilter.class));
			return Filter.composeFilters(filters).toPredicate();
		}

		@Override
		public EngineDiscoveryRequest getDiscoveryRequest() {
			return request;
		}

		@Override
		public T getEngineDescriptor() {
			return engineDescriptor;
		}

		@Override
		public Predicate<String> getClassNameFilter() {
			return classNameFilter;
		}
	}

}
