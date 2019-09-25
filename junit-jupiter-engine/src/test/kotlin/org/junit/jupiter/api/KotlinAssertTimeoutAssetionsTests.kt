/*
 * Copyright 2015-2019 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */
package org.junit.jupiter.api

import org.junit.jupiter.api.AssertionTestUtils.assertMessageEquals
import org.junit.jupiter.api.AssertionTestUtils.assertMessageStartsWith
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.platform.commons.util.ExceptionUtils
import org.opentest4j.AssertionFailedError
import java.time.Duration.ofMillis
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Unit tests for JUnit Jupiter [Assertions].
 *
 * @since 5.5
 */
internal class KotlinAssertTimeoutAssertionsTests {

    // --- executable ----------------------------------------------------------

    @Test
    fun assertTimeoutForExecutableThatCompletesBeforeTheTimeout() {
        changed.get().set(false)
        assertTimeout(ofMillis(500)) { changed.get().set(true) }
        assertTrue(changed.get().get(), "should have executed in the same thread")
        assertTimeout(ofMillis(500), "message") { }
        assertTimeout(ofMillis(500), "message") { }
    }

    @Test
    fun assertTimeoutForExecutableThatThrowsAnException() {
        val exception = assertThrows<RuntimeException> {
            assertTimeout<Any>(ofMillis(500)) {
                throw RuntimeException("not this time")
            }
        }
        assertMessageEquals(exception, "not this time")
    }

    @Test
    fun assertTimeoutForExecutableThatThrowsAnAssertionFailedError() {
        val exception = assertThrows<AssertionFailedError> {
            assertTimeout<Any>(ofMillis(500)) { fail("enigma") }
        }
        assertMessageEquals(exception, "enigma")
    }

    @Test
    fun assertTimeoutForExecutableThatCompletesAfterTheTimeout() {
        val error = assertThrows<AssertionFailedError> {
            assertTimeout(ofMillis(10)) { this.nap() }
        }
        assertMessageStartsWith(error, "execution exceeded timeout of 10 ms by")
    }

    @Test
    fun assertTimeoutWithMessageForExecutableThatCompletesAfterTheTimeout() {
        val error = assertThrows<AssertionFailedError> {
            assertTimeout(ofMillis(10), "Tempus Fugit") { this.nap() }
        }
        assertMessageStartsWith(error, "Tempus Fugit ==> execution exceeded timeout of 10 ms by")
    }

    @Test
    fun assertTimeoutWithMessageSupplierForExecutableThatCompletesAfterTheTimeout() {
        val error = assertThrows<AssertionFailedError> {
            assertTimeout(ofMillis(10), { "Tempus" + " " + "Fugit" }) { this.nap() }
        }
        assertMessageStartsWith(error, "Tempus Fugit ==> execution exceeded timeout of 10 ms by")
    }

    // --- supplier ------------------------------------------------------------

    @Test
    fun assertTimeoutForSupplierThatCompletesBeforeTheTimeout() {
        changed.get().set(false)
        val result = assertTimeout(ofMillis(500)) {
            changed.get().set(true)
            "Tempus Fugit"
        }
        assertTrue(changed.get().get(), "should have executed in the same thread")
        assertEquals("Tempus Fugit", result)
        assertEquals("Tempus Fugit", assertTimeout(ofMillis(500), "message") { "Tempus Fugit" })
        assertEquals("Tempus Fugit", assertTimeout(ofMillis(500), { "message" }, { "Tempus Fugit" }))
    }

    @Test
    fun assertTimeoutForSupplierThatThrowsAnException() {
        val exception = assertThrows<RuntimeException> {
            assertTimeout(ofMillis(500)) {
                ExceptionUtils.throwAsUncheckedException(RuntimeException("not this time"))
                "Tempus Fugit"
            }
        }
        assertMessageEquals(exception, "not this time")
    }

    @Test
    fun assertTimeoutForSupplierThatThrowsAnAssertionFailedError() {
        val exception = assertThrows<AssertionFailedError> {
            assertTimeout(ofMillis(500)) {
                fail("enigma")
            }
        }
        assertMessageEquals(exception, "enigma")
    }

    @Test
    fun assertTimeoutForSupplierThatCompletesAfterTheTimeout() {
        val error = assertThrows<AssertionFailedError> {
            assertTimeout(ofMillis(10)) {
                nap()
                "Tempus Fugit"
            }
        }
        assertMessageStartsWith(error, "execution exceeded timeout of 10 ms by")
    }

    @Test
    fun assertTimeoutWithMessageForSupplierThatCompletesAfterTheTimeout() {
        val error = assertThrows<AssertionFailedError> {
            assertTimeout(ofMillis(10), "Tempus Fugit") {
                nap()
                "Tempus Fugit"
            }
        }
        assertMessageStartsWith(error, "Tempus Fugit ==> execution exceeded timeout of 10 ms by")
    }

    @Test
    fun assertTimeoutWithMessageSupplierForSupplierThatCompletesAfterTheTimeout() {
        val error = assertThrows<AssertionFailedError> {
            assertTimeout(ofMillis(10), { "Tempus" + " " + "Fugit" }) {
                nap()
                "Tempus Fugit"
            }
        }
        assertMessageStartsWith(error, "Tempus Fugit ==> execution exceeded timeout of 10 ms by")
    }

    // -- executable - preemptively ---

    @Test
    fun assertTimeoutPreemptivelyForExecutableThatCompletesBeforeTheTimeout() {
        changed.get().set(false)
        assertTimeoutPreemptively(ofMillis(500)) { changed.get().set(true) }
        assertFalse(changed.get().get(), "should have executed in a different thread")
        assertTimeoutPreemptively(ofMillis(500), "message") {}
        assertTimeoutPreemptively(ofMillis(500), { "message" }) {}
    }

    @Test
    fun assertTimeoutPreemptivelyForExecutableThatThrowsAnException() {
        val exception = assertThrows<RuntimeException> {
            assertTimeoutPreemptively<Any>(ofMillis(500)) { throw RuntimeException("not this time") }
        }
        assertMessageEquals(exception, "not this time")
    }

    @Test
    fun assertTimeoutPreemptivelyForExecutableThatThrowsAnAssertionFailedError() {
        val exception = assertThrows<AssertionFailedError> {
            assertTimeoutPreemptively<Any>(ofMillis(500)) { fail("enigma") }
        }
        assertMessageEquals(exception, "enigma")
    }

    @Test
    fun assertTimeoutPreemptivelyForExecutableThatCompletesAfterTheTimeout() {
        val error = assertThrows<AssertionFailedError> {
            assertTimeoutPreemptively(ofMillis(10)) { this.nap() }
        }
        assertMessageEquals(error, "execution timed out after 10 ms")
    }

    @Test
    fun assertTimeoutPreemptivelyWithMessageForExecutableThatCompletesAfterTheTimeout() {
        val error = assertThrows<AssertionFailedError> {
            assertTimeoutPreemptively(ofMillis(10), "Tempus Fugit") { this.nap() }
        }
        assertMessageEquals(error, "Tempus Fugit ==> execution timed out after 10 ms")
    }

    @Test
    fun assertTimeoutPreemptivelyWithMessageSupplierForExecutableThatCompletesAfterTheTimeout() {
        val error = assertThrows<AssertionFailedError> {
            assertTimeoutPreemptively(ofMillis(10), { "Tempus" + " " + "Fugit" }) { this.nap() }
        }
        assertMessageEquals(error, "Tempus Fugit ==> execution timed out after 10 ms")
    }

    @Test
    fun assertTimeoutPreemptivelyWithMessageSupplierForExecutableThatCompletesBeforeTheTimeout() {
        assertTimeoutPreemptively(ofMillis(500), { "Tempus" + " " + "Fugit" }) {}
    }

    // -- supplier - preemptively ---

    @Test
    fun assertTimeoutPreemptivelyForSupplierThatCompletesBeforeTheTimeout() {
        changed.get().set(false)
        val result = assertTimeoutPreemptively(ofMillis(500)) {
            changed.get().set(true)
            "Tempus Fugit"
        }
        assertFalse(changed.get().get(), "should have executed in a different thread")
        assertEquals("Tempus Fugit", result)
        assertEquals("Tempus Fugit", assertTimeoutPreemptively(ofMillis(500), "message") { "Tempus Fugit" })
        assertEquals("Tempus Fugit", assertTimeoutPreemptively(ofMillis(500), { "message" }) { "Tempus Fugit" })
    }

    @Test
    fun assertTimeoutPreemptivelyForSupplierThatThrowsAnException() {
        val exception = assertThrows<RuntimeException> {
            assertTimeoutPreemptively(ofMillis(500)) {
                ExceptionUtils.throwAsUncheckedException(RuntimeException("not this time"))
                "Tempus Fugit"
            }
        }
        assertMessageEquals(exception, "not this time")
    }

    @Test
    fun assertTimeoutPreemptivelyForSupplierThatThrowsAnAssertionFailedError() {
        val exception = assertThrows<AssertionFailedError> {
            assertTimeoutPreemptively(ofMillis(500)) {
                fail("enigma")
            }
        }
        assertMessageEquals(exception, "enigma")
    }

    @Test
    fun assertTimeoutPreemptivelyForSupplierThatCompletesAfterTheTimeout() {
        val error = assertThrows<AssertionFailedError> {
            assertTimeoutPreemptively(ofMillis(10)) {
                nap()
                "Tempus Fugit"
            }
        }
        assertMessageEquals(error, "execution timed out after 10 ms")
    }

    @Test
    fun assertTimeoutPreemptivelyWithMessageForSupplierThatCompletesAfterTheTimeout() {
        val error = assertThrows<AssertionFailedError> {
            assertTimeoutPreemptively(ofMillis(10), "Tempus Fugit") {
                nap()
                "Tempus Fugit"
            }
        }
        assertMessageEquals(error, "Tempus Fugit ==> execution timed out after 10 ms")
    }

    @Test
    fun assertTimeoutPreemptivelyWithMessageSupplierForSupplierThatCompletesAfterTheTimeout() {
        val error = assertThrows<AssertionFailedError> {
            assertTimeoutPreemptively(ofMillis(10), { "Tempus" + " " + "Fugit" }) {
                nap()
                "Tempus Fugit"
            }
        }
        assertMessageEquals(error, "Tempus Fugit ==> execution timed out after 10 ms")
    }

    /**
     * Take a nap for 100 milliseconds.
     */
    private fun nap() {
        Thread.sleep(100)
    }

    companion object {
        private val changed = ThreadLocal.withInitial { AtomicBoolean(false) }
    }
}
