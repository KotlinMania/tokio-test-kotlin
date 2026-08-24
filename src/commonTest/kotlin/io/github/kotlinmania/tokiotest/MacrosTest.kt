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

    enum class TestEnum {
        Data,
    }

    @Test
    fun testAssertReady() {
        val poll = ready()
        assertReady(poll)
        assertReady(poll, "some message")
        assertReady(poll, Unit.toString())
        assertReady(poll, TestEnum.Data.name)
    }

    @Test
    fun testAssertReadyOnPending() {
        val poll = pending()
        assertFailsWith<AssertionError> {
            assertReady(poll)
        }
    }

    @Test
    fun testAssertPending() {
        val poll = pending()
        assertPending(poll)
        assertPending(poll, "some message")
        assertPending(poll, Unit.toString())
        assertPending(poll, TestEnum.Data.name)
    }

    @Test
    fun testAssertPendingOnReady() {
        val poll = ready()
        assertFailsWith<AssertionError> {
            assertPending(poll)
        }
    }

    @Test
    fun testAssertReadyOk() {
        val poll = readyOk()
        assertReadyOk(poll)
        assertReadyOk(poll, "some message")
        assertReadyOk(poll, Unit.toString())
        assertReadyOk(poll, TestEnum.Data.name)
    }

    @Test
    fun testAssertOkOnErr() {
        val poll = readyErr()
        assertFailsWith<AssertionError> {
            assertReadyOk(poll)
        }
    }

    @Test
    fun testAssertReadyErr() {
        val poll = readyErr()
        assertReadyErr(poll)
        assertReadyErr(poll, "some message")
        assertReadyErr(poll, Unit.toString())
        assertReadyErr(poll, TestEnum.Data.name)
    }

    @Test
    fun testAssertErrOnOk() {
        val poll = readyOk()
        assertFailsWith<AssertionError> {
            assertReadyErr(poll)
        }
    }

    @Test
    fun testAssertReadyEq() {
        val poll = ready()
        assertReadyEq(poll, Unit)
        assertReadyEq(poll, Unit, "some message")
        assertReadyEq(poll, Unit, Unit.toString())
        assertReadyEq(poll, Unit, TestEnum.Data.name)
    }

    @Test
    fun testAssertOkDirect() {
        val success = Result.success(42)
        val value = assertOk(success)
        assertEquals(42, value)

        val failure = Result.failure<Int>(IllegalStateException("err"))
        assertFailsWith<AssertionError> {
            assertOk(failure)
        }
    }

    @Test
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
