// port-lint: tests task.rs
package io.github.kotlinmania.tokiotest

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TaskTest {
    class SizedMock(val size: Pair<Int, Int?>)

    @Test
    fun testSpawnSizeHint() {
        val stream = SizedMock(100 to 200)
        val spawn = spawn(stream)
        assertEquals(100 to 200, spawn.intoInner().size)
    }

    @Test
    fun testMockTaskWake() {
        val spawn = spawn(42)
        assertFalse(spawn.isWoken())
        assertEquals(1, spawn.wakerRefCount())

        spawn.enter { task ->
            assertFalse(task.isWoken())
            task.wake()
            assertTrue(task.isWoken())
        }

        spawn.enter { task ->
            // enter clears previous wake
            assertFalse(task.isWoken())
        }
    }
}
