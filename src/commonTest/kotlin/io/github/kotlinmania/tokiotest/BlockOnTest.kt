// port-lint: tests block_on.rs
package io.github.kotlinmania.tokiotest

import kotlin.test.Test
import kotlin.test.assertEquals

class BlockOnTest {
    @Test
    fun testAsyncBlock() {
        assertEquals(4, blockOn { 4 })
    }

    private suspend fun five(): Byte = 5

    @Test
    fun testAsyncFn() {
        assertEquals(5.toByte(), blockOn { five() })
    }

    @Test
    fun testSleep() {
        val result =
            blockOn {
                100
            }
        assertEquals(100, result)
    }
}
