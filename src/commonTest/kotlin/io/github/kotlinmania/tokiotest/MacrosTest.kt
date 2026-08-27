// port-lint: tests macros.rs
package io.github.kotlinmania.tokiotest

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MacrosTest {
    private fun ready(): Poll<Unit> = Poll.Ready(Unit)

    private fun readyOk(): Poll<Result<Unit>> = Poll.Ready(Result.success(Unit))

    private fun readyErr(): Poll<Result<Unit>> = Poll.Ready(Result.failure(IllegalStateException("boom")))

    private fun pending(): Poll<Unit> = Poll.Pending

    enum class Test {
        Data,
    }

    @kotlin.test.Test
    fun assertReady() {
        val poll = ready()
        assertReady(poll)
        assertReady(poll, "some message")
        assertReady(poll, Unit.toString())
        assertReady(poll, Test.Data.name)
    }

    @kotlin.test.Test
    fun assertReadyOnPending() {
        val poll = pending()
        assertFailsWith<AssertionError> {
            assertReady(poll)
        }
    }

    @kotlin.test.Test
    fun assertPending() {
        val poll = pending()
        assertPending(poll)
        assertPending(poll, "some message")
        assertPending(poll, Unit.toString())
        assertPending(poll, Test.Data.name)
    }

    @kotlin.test.Test
    fun assertPendingOnReady() {
        val poll = ready()
        assertFailsWith<AssertionError> {
            assertPending(poll)
        }
    }

    @kotlin.test.Test
    fun assertReadyOk() {
        val poll = readyOk()
        assertReadyOk(poll)
        assertReadyOk(poll, "some message")
        assertReadyOk(poll, Unit.toString())
        assertReadyOk(poll, Test.Data.name)
    }

    @kotlin.test.Test
    fun assertOkOnErr() {
        val poll = readyErr()
        assertFailsWith<AssertionError> {
            assertReadyOk(poll)
        }
    }

    @kotlin.test.Test
    fun assertReadyErr() {
        val poll = readyErr()
        assertReadyErr(poll)
        assertReadyErr(poll, "some message")
        assertReadyErr(poll, Unit.toString())
        assertReadyErr(poll, Test.Data.name)
    }

    @kotlin.test.Test
    fun assertErrOnOk() {
        val poll = readyOk()
        assertFailsWith<AssertionError> {
            assertReadyErr(poll)
        }
    }

    @kotlin.test.Test
    fun assertReadyEq() {
        val poll = ready()
        assertReadyEq(poll, Unit)
        assertReadyEq(poll, Unit, "some message")
        assertReadyEq(poll, Unit, Unit.toString())
        assertReadyEq(poll, Unit, Test.Data.name)
    }

    @kotlin.test.Test
    fun assertEqOnNotEq() {
        val poll = readyErr()
        assertFailsWith<AssertionError> {
            assertReadyEq(poll, Result.success(Unit))
        }
    }

    @kotlin.test.Test
    fun testAssertOkDirect() {
        val success = Result.success(42)
        val value = assertOk(success)
        assertEquals(42, value)

        val failure = Result.failure<Int>(IllegalStateException("err"))
        assertFailsWith<AssertionError> {
            assertOk(failure)
        }
    }

    @kotlin.test.Test
    fun testAssertErrDirect() {
        val failure = Result.failure<Int>(IllegalStateException("err"))
        val err = assertErr(failure)
        assertEquals("err", err.message)

        val success = Result.success(42)
        assertFailsWith<AssertionError> {
            assertErr(success)
        }
    }
}


