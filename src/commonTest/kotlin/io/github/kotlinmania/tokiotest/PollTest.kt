// port-lint: tests tokio-test/tests/macros.rs
package io.github.kotlinmania.tokiotest

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PollTest {
    @Test
    fun testPollReady() {
        val ready = Poll.Ready(42)
        assertTrue(ready.isReady)
        assertFalse(ready.isPending)
        assertEquals(42, ready.value)
    }

    @Test
    fun testPollPending() {
        val pending = Poll.Pending
        assertFalse(pending.isReady)
        assertTrue(pending.isPending)
        assertEquals("Pending", pending.toString())
    }
}
