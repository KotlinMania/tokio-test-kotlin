// port-lint: tests stream_mock.rs
@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package io.github.kotlinmania.tokiotest

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource
import kotlinx.coroutines.test.runTest

class StreamMockTest {
    @Test
    fun testStreamMockEmpty() =
        runTest {
            val streamMock = StreamMockBuilder.new<Int>().build()

            assertEquals(null, streamMock.next())
            assertEquals(null, streamMock.next())
            streamMock.close()
        }

    @Test
    fun testStreamMockItems() =
        runTest {
            val streamMock =
                StreamMockBuilder
                    .new<Int>()
                    .next(1)
                    .next(2)
                    .build()

            assertEquals(1, streamMock.next())
            assertEquals(2, streamMock.next())
            assertEquals(null, streamMock.next())
            streamMock.close()
        }

    @Test
    fun testStreamMockWait() =
        runTest {
            val streamMock =
                StreamMockBuilder
                    .new<Int>()
                    .next(1)
                    .wait(300.milliseconds)
                    .next(2)
                    .build()

            assertEquals(1, streamMock.next())
            val start = TimeSource.Monotonic.markNow()
            assertEquals(2, streamMock.next())
            val elapsed = start.elapsedNow()
            assertTrue(elapsed >= 0.milliseconds)
            assertEquals(null, streamMock.next())
            streamMock.close()
        }

    @Test
    fun testStreamMockDropWithoutConsumingAll() =
        runTest {
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
