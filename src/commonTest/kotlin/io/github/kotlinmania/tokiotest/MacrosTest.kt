package io.github.kotlinmania.tokiotest

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MacrosTest {
    @Test
    fun assertReadyReturnsReadyValue() {
        assertEquals(42, assertReady(Poll.Ready(42)))
        assertFailsWith<AssertionError> { assertReady(Poll.Pending) }
    }

    @Test
    fun assertPendingRejectsReadyValue() {
        assertPending(Poll.Pending)
        assertFailsWith<AssertionError> { assertPending(Poll.Ready("done")) }
    }

    @Test
    fun resultAssertionsReturnContainedValues() {
        val error = IllegalStateException("boom")
        assertEquals("ok", assertOk(Result.success("ok")))
        assertEquals(error, assertErr(Result.failure<String>(error)))
        assertEquals("ok", assertReadyOk(Poll.Ready(Result.success("ok"))))
        assertEquals(error, assertReadyErr(Poll.Ready(Result.failure<String>(error))))
    }

    @Test
    fun assertReadyEqReturnsTheReadyValue() {
        assertEquals("same", assertReadyEq(Poll.Ready("same"), "same"))
        assertFailsWith<AssertionError> { assertReadyEq(Poll.Ready("actual"), "expected") }
    }
}
