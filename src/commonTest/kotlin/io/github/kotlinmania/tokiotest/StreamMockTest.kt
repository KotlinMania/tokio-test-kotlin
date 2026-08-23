// port-lint: tests stream_mock.rs
package io.github.kotlinmania.tokiotest

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.milliseconds

class StreamMockTest {
    @Test
    fun testStreamMockEmpty() {
        val streamMock = StreamMockBuilder.new<Int>().build()
        val p1 = streamMock.pollNext()
        assertIs<Poll.Ready<Int?>>(p1)
        assertEquals(null, p1.value)
        val p2 = streamMock.pollNext()
        assertIs<Poll.Ready<Int?>>(p2)
        assertEquals(null, p2.value)
        streamMock.close()
    }

    @Test
    fun testStreamMockItems() {
        val streamMock =
            StreamMockBuilder
                .new<Int>()
                .next(1)
                .next(2)
                .build()
        val p1 = streamMock.pollNext()
        assertIs<Poll.Ready<Int?>>(p1)
        assertEquals(1, p1.value)
        val p2 = streamMock.pollNext()
        assertIs<Poll.Ready<Int?>>(p2)
        assertEquals(2, p2.value)
        val p3 = streamMock.pollNext()
        assertIs<Poll.Ready<Int?>>(p3)
        assertEquals(null, p3.value)
        streamMock.close()
    }

    @Test
    fun testStreamMockWait() {
        val streamMock =
            StreamMockBuilder
                .new<Int>()
                .next(1)
                .wait(300.milliseconds)
                .next(2)
                .build()

        val p1 = streamMock.pollNext()
        assertIs<Poll.Ready<Int?>>(p1)
        assertEquals(1, p1.value)

        val pWait = streamMock.pollNext()
        assertIs<Poll.Pending>(pWait)

        val p2 = streamMock.pollNext()
        assertIs<Poll.Ready<Int?>>(p2)
        assertEquals(2, p2.value)

        val pEnd = streamMock.pollNext()
        assertIs<Poll.Ready<Int?>>(pEnd)
        assertEquals(null, pEnd.value)

        streamMock.close()
    }

    @Test
    fun testStreamMockDropWithoutConsumingAll() {
        val streamMock =
            StreamMockBuilder
                .new<Int>()
                .next(1)
                .next(2)
                .build()
        assertFailsWith<IllegalStateException> {
            streamMock.close()
        }
    }
}
