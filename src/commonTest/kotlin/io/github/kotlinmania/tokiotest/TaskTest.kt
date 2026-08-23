// port-lint: tests task.rs
package io.github.kotlinmania.tokiotest

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TaskTest {
    private data class SizedStream(
        val lower: Int = 100,
        val upper: Int? = 200,
    )

    @Test
    fun testSpawnStreamSizeHint() {
        val spawn = spawn(SizedStream())
        assertEquals(100, spawn.intoInner().lower)
    }

    @Test
    fun testMockTaskWaking() {
        val task = spawn("work")
        assertFalse(task.isWoken())
        task.enter { cx ->
            cx.wake()
        }
        assertTrue(task.isWoken())
        task.enter { }
        assertFalse(task.isWoken())
    }
}
