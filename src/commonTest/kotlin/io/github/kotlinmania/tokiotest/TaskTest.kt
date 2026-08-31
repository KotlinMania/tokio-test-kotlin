// port-lint: tests tokio-test/src/task.rs
package io.github.kotlinmania.tokiotest

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TaskTest {
    class SizedStream(
        val size: Pair<Int, Int?>,
    )

    @Test
    fun testSpawnStreamSizeHint() {
        val stream = Pair(100, 200)
        val spawn = spawn(stream)
        assertEquals(Pair(100, 200), spawn.sizeHint())
    }

    @Test
    fun testSpawnSizeHint() {
        val stream = SizedStream(100 to 200)
        val spawn = spawn(stream)
        assertEquals(100 to 200, spawn.intoInner().size)
    }

    @Test
    fun testSpawnDeref() {
        val spawn = spawn(123)
        assertEquals(123, spawn.deref())
        assertEquals(123, spawn.derefMut())
        assertTrue(spawn.poll().isReady)
        assertTrue(spawn.pollNext().isReady)
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

    @Test
    fun testThreadWaker() {
        val waker = ThreadWaker.new()
        assertFalse(waker.isWoken())
        waker.wake()
        assertTrue(waker.isWoken())
        waker.clear()
        assertFalse(waker.isWoken())
        waker.wakeByRef()
        assertTrue(waker.isWoken())

        val raw = waker.toRaw()
        val restored = ThreadWaker.fromRaw(raw)
        assertTrue(restored.isWoken())
        val cloned = waker.clone()
        assertTrue(cloned.isWoken())
        ThreadWaker.dropWaker(raw)
    }
}
