/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.hierarchical;

import static org.apiguardian.api.API.Status.MAINTAINED;
import static org.junit.platform.commons.util.BlacklistedExceptions.rethrowIfBlacklisted;
import static org.junit.platform.engine.TestExecutionResult.aborted;
import static org.junit.platform.engine.TestExecutionResult.failed;
import static org.junit.platform.engine.TestExecutionResult.successful;

import org.apiguardian.api.API;
import org.junit.platform.engine.TestExecutionResult;
import org.opentest4j.TestAbortedException;

/**
 * {@code SingleTestExecutor} encapsulates the execution of a single test
 * wrapped in an {@link Executable}.
 *
 * @since 1.0
 * @see #executeSafely(Executable)
 */
@API(status = MAINTAINED, since = "1.0")
public class SingleTestExecutor {

	/**
	 * Functional interface for a single test to be executed by
	 * {@link SingleTestExecutor}.
	 */
	public interface Executable {

		/**
		 * Execute the test.
		 *
		 * @throws TestAbortedException to signal abortion
		 * @throws Throwable to signal failure
		 */
		void execute() throws TestAbortedException, Throwable;

	}

	/**
	 * Execute the supplied {@link Executable} and return a
	 * {@link TestExecutionResult} based on the outcome.
	 *
	 * <p>If the {@code Executable} throws a <em>blacklisted</em> exception
	 * &mdash; for example, an {@link OutOfMemoryError} &mdash; this method will
	 * rethrow it.
	 *
	 * @param executable the test to be executed
	 * @return {@linkplain TestExecutionResult#aborted aborted} if the
	 * {@code Executable} throws a {@link TestAbortedException};
	 * {@linkplain TestExecutionResult#failed failed} if any other
	 * {@link Throwable} is thrown; and {@linkplain TestExecutionResult#successful
	 * successful} otherwise
	 */
	public TestExecutionResult executeSafely(Executable executable) {
		try {
			executable.execute();
			return successful();
		}
		catch (TestAbortedException e) {
			return aborted(e);
		}
		catch (Throwable t) {
			rethrowIfBlacklisted(t);
			return failed(t);
		}
	}

}
