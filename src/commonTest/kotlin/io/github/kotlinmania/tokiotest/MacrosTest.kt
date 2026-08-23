// port-lint: tests macros.rs
package io.github.kotlinmania.tokiotest

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MacrosTest {
    private fun ready(): Poll<Unit> = Poll.Ready(Unit)

    private fun readyOk(): Poll<Result<Unit>> = Poll.Ready(Result.success(Unit))

    private fun readyErr(): Poll<Result<Unit>> = Poll.Ready(Result.failure(IllegalStateException("test error")))

    private fun pending(): Poll<Unit> = Poll.Pending

    @Test
    fun assertReady() {
        val poll = ready()
        assertReady(poll)
        assertReady(poll, "some message")
        assertReady(poll, "Unit")
    }

    @Test
    fun assertReadyOnPending() {
        val poll = pending()
        assertFailsWith<AssertionError> {
            assertReady(poll)
        }
    }

    @Test
    fun assertPending() {
        val poll = pending()
        assertPending(poll)
        assertPending(poll, "some message")
    }

    @Test
    fun assertPendingOnReady() {
        val poll = ready()
        assertFailsWith<AssertionError> {
            assertPending(poll)
        }
    }

    @Test
    fun assertReadyOk() {
        val poll = readyOk()
        assertReadyOk(poll)
        assertReadyOk(poll, "some message")
    }

    @Test
    fun assertOkOnErr() {
        val poll = readyErr()
        assertFailsWith<AssertionError> {
            assertReadyOk(poll)
        }
    }

    @Test
    fun assertReadyErr() {
        val poll = readyErr()
        assertReadyErr(poll)
        assertReadyErr(poll, "some message")
    }

    @Test
    fun assertErrOnOk() {
        val poll = readyOk()
        assertFailsWith<AssertionError> {
            assertReadyErr(poll)
        }
    }

    @Test
    fun assertReadyEq() {
        val poll = ready()
        assertReadyEq(poll, Unit)
        assertReadyEq(poll, Unit, "some message")
    }

    @Test
    fun assertEqOnNotEq() {
        val poll = Poll.Ready(42)
        assertFailsWith<AssertionError> {
            assertReadyEq(poll, 99)
        }
    }

    @Test
    fun resultAssertionsReturnContainedValues() {
        val error = IllegalStateException("boom")
        assertEquals("ok", assertOk(Result.success("ok")))
        assertEquals(error, assertErr(Result.failure<String>(error)))
        assertEquals("ok", assertReadyOk(Poll.Ready(Result.success("ok"))))
        assertEquals(error, assertReadyErr(Poll.Ready(Result.failure<String>(error))))
    }
}
