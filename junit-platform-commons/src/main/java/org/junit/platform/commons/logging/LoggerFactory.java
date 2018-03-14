/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.logging;

import static org.apiguardian.api.API.Status.INTERNAL;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.apiguardian.api.API;
import org.junit.platform.commons.JUnitException;

/**
 * Factory for the {@link Logger} facade for JUL.
 *
 * @since 1.0
 */
@API(status = INTERNAL, since = "1.0")
public final class LoggerFactory {

	///CLOVER:OFF
	private LoggerFactory() {
		/* no-op */
	}
	///CLOVER:ON

	private static final Set<LogRecordListener> listeners = ConcurrentHashMap.newKeySet();

	/**
	 * Get a {@link Logger} for the specified class.
	 *
	 * @param clazz the class for which to get the logger; never {@code null}
	 * @return the logger
	 */
	public static Logger getLogger(Class<?> clazz) {
		// NOTE: we cannot use org.junit.platform.commons.util.Preconditions here
		// since that would introduce a package cycle.
		if (clazz == null) {
			throw new JUnitException("Class must not be null");
		}

		return new DelegatingLogger(clazz.getName());
	}

	/**
	 * Add the supplied {@link LogRecordListener} to the set of registered
	 * listeners.
	 */
	public static void addListener(LogRecordListener listener) {
		listeners.add(listener);
	}

	/**
	 * Remove the supplied {@link LogRecordListener} from the set of registered
	 * listeners.
	 */
	public static void removeListener(LogRecordListener listener) {
		listeners.remove(listener);
	}

	private static final class DelegatingLogger implements Logger {

		private static final String FQCN = DelegatingLogger.class.getName();

		private final String name;

		private final java.util.logging.Logger julLogger;

		DelegatingLogger(String name) {
			this.name = name;
			this.julLogger = java.util.logging.Logger.getLogger(this.name);
		}

		@Override
		public void error(Supplier<String> messageSupplier) {
			log(Level.SEVERE, null, messageSupplier);
		}

		@Override
		public void error(Throwable throwable, Supplier<String> messageSupplier) {
			log(Level.SEVERE, throwable, messageSupplier);
		}

		@Override
		public void warn(Supplier<String> messageSupplier) {
			log(Level.WARNING, null, messageSupplier);
		}

		@Override
		public void warn(Throwable throwable, Supplier<String> messageSupplier) {
			log(Level.WARNING, throwable, messageSupplier);
		}

		@Override
		public void info(Supplier<String> messageSupplier) {
			log(Level.INFO, null, messageSupplier);
		}

		@Override
		public void info(Throwable throwable, Supplier<String> messageSupplier) {
			log(Level.INFO, throwable, messageSupplier);
		}

		@Override
		public void config(Supplier<String> messageSupplier) {
			log(Level.CONFIG, null, messageSupplier);
		}

		@Override
		public void config(Throwable throwable, Supplier<String> messageSupplier) {
			log(Level.CONFIG, throwable, messageSupplier);
		}

		@Override
		public void debug(Supplier<String> messageSupplier) {
			log(Level.FINE, null, messageSupplier);
		}

		@Override
		public void debug(Throwable throwable, Supplier<String> messageSupplier) {
			log(Level.FINE, throwable, messageSupplier);
		}

		@Override
		public void trace(Supplier<String> messageSupplier) {
			log(Level.FINER, null, messageSupplier);
		}

		@Override
		public void trace(Throwable throwable, Supplier<String> messageSupplier) {
			log(Level.FINER, throwable, messageSupplier);
		}

		private void log(Level level, Throwable throwable, Supplier<String> messageSupplier) {
			boolean loggable = this.julLogger.isLoggable(level);
			if (loggable || !listeners.isEmpty()) {
				LogRecord logRecord = createLogRecord(level, throwable, messageSupplier);
				if (loggable) {
					this.julLogger.log(logRecord);
				}
				listeners.forEach(l -> l.logRecordSubmitted(logRecord));
			}
		}

		private LogRecord createLogRecord(Level level, Throwable throwable, Supplier<String> messageSupplier) {
			StackTraceElement[] stack = new Throwable().getStackTrace();
			String sourceClassName = null;
			String sourceMethodName = null;
			boolean found = false;
			for (StackTraceElement element : stack) {
				String className = element.getClassName();
				if (FQCN.equals(className)) {
					found = true;
				}
				else if (found) {
					sourceClassName = className;
					sourceMethodName = element.getMethodName();
					break;
				}
			}

			LogRecord logRecord = new LogRecord(level, nullSafeGet(messageSupplier));
			logRecord.setLoggerName(this.name);
			logRecord.setThrown(throwable);
			logRecord.setSourceClassName(sourceClassName);
			logRecord.setSourceMethodName(sourceMethodName);
			logRecord.setResourceBundleName(this.julLogger.getResourceBundleName());
			logRecord.setResourceBundle(this.julLogger.getResourceBundle());

			return logRecord;
		}

		private static String nullSafeGet(Supplier<String> messageSupplier) {
			return (messageSupplier != null ? messageSupplier.get() : null);
		}

	}

}
