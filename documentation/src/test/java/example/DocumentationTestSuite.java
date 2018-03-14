/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package example;

import org.junit.platform.runner.JUnitPlatform;
import org.junit.platform.suite.api.ExcludeTags;
import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.runner.RunWith;

/**
 * <h3>Logging Configuration</h3>
 *
 * <p>In order for our log4j2 configuration to be used in an IDE, you must
 * set the following system property before running any tests &mdash; for
 * example, in <em>Run Configurations</em> in Eclipse.
 *
 * <pre class="code">
 * -Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager
 * </pre>
 *
 * @since 5.0
 */
@RunWith(JUnitPlatform.class)
@SelectPackages("example")
@IncludeClassNamePatterns(".+(Tests|Demo)$")
@ExcludeTags("exclude")
public class DocumentationTestSuite {
}
