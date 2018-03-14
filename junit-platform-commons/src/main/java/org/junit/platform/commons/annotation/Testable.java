/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.annotation;

import static org.apiguardian.api.API.Status.STABLE;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;

/**
 * {@code @Testable} is used to signal to IDEs and tooling vendors that the
 * annotated or meta-annotated element is <em>testable</em>.
 *
 * <p>In this context, the term "testable" means that the annotated method or
 * class can be executed by a {@code TestEngine} as a test or test container
 * on the JUnit Platform.
 *
 * <h3>Motivation for {@code @Testable}</h3>
 * <p>Some clients of the JUnit Platform, notably IDEs such as IntelliJ IDEA,
 * operate only on sources for test discovery. Thus, they cannot use the full
 * runtime discovery mechanism of the JUnit Platform since it relies on compiled
 * classes. {@code @Testable} therefore serves as an alternative mechanism for
 * IDEs to discover tests by analyzing the source code only.
 *
 * <h3>Common Use Cases</h3>
 * <p>{@code @Testable} will typically be used as a meta-annotation in order to
 * create a custom <em>composed annotation</em> that inherits the semantics
 * of {@code @Testable}. For example, the {@code @Test} and {@code @TestFactory}
 * annotations in JUnit Jupiter are meta-annotated with {@code @Testable}.
 * <p>For test programming models that do not rely on annotations, test classes
 * or test methods may be directly annotated with {@code @Testable}. Alternatively,
 * if concrete test classes extend from a base class, the base class can be
 * annotated with {@code @Testable}. Note that {@code @Testable} is an
 * {@link Inherited @Inherited} annotation.
 *
 * <h3>Requirements for IDEs and Tooling Vendors</h3>
 * <ul>
 * <li>If a top-level class, static nested class, or inner class is not
 * annotated or meta-annotated with {@code @Testable} but contains a method
 * that is annotated or meta-annotated with {@code @Testable}, the class must
 * be considered to be a <em>testable</em> class.</li>
 * <li>If annotation hierarchies containing {@code @Testable} are present on
 * classes or methods in compiled byte code (e.g., in JARs in the user's
 * classpath), IDEs and tooling vendors must also take such annotation
 * hierarchies into consideration when performing annotation processing for
 * source code.</li>
 * </ul>
 *
 * <h3>Restrictions for TestEngine Implementations</h3>
 * <p>A {@code TestEngine} must <strong>not</strong> in any way perform
 * <em>discovery</em> based on the presence of {@code @Testable}. In terms of
 * discovery, the presence of {@code @Testable} should only be meaningful to
 * clients such as IDEs and tooling vendors. A {@code TestEngine} implementation
 * is therefore required to discover tests based on information specific to
 * that test engine (e.g., annotations specific to that test engine).
 *
 * @since 1.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@API(status = STABLE, since = "1.0")
public @interface Testable {
}
