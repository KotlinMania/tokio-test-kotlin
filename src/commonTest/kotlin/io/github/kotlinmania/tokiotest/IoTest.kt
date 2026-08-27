// port-lint: tests io.rs
@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package io.github.kotlinmania.tokiotest

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class IoTest {
    companion object {
        private const val FIRST_WAIT = 1000L
        private const val SECOND_WAIT = 1000L
    }

    @Test
    fun read() =
        runTest {
            val mock =
                Builder
                    .new()
                    .read("hello ".encodeToByteArray())
                    .read("world!".encodeToByteArray())
                    .build()

            val buf = ByteArray(256)

            val n1 = mock.read(buf)
            assertEquals("hello ", buf.decodeToString(0, n1))

            val n2 = mock.read(buf)
            assertEquals("world!", buf.decodeToString(0, n2))

            mock.close()
        }

    @Test
    fun readError() =
        runTest {
            val error = IllegalStateException("cruel")
            val mock =
                Builder
                    .new()
                    .read("hello ".encodeToByteArray())
                    .readError(error)
                    .read("world!".encodeToByteArray())
                    .build()
            val buf = ByteArray(256)

            val n1 = mock.read(buf)
            assertEquals("hello ", buf.decodeToString(0, n1))

            val ex =
                assertFailsWith<IllegalStateException> {
                    mock.read(buf)
                }
            assertEquals("cruel", ex.message)

            val n2 = mock.read(buf)
            assertEquals("world!", buf.decodeToString(0, n2))

            mock.close()
        }

    @Test
    fun write() =
        runTest {
            val mock =
                Builder
                    .new()
                    .write("hello ".encodeToByteArray())
                    .write("world!".encodeToByteArray())
                    .build()

            mock.writeAll("hello ".encodeToByteArray())
            mock.writeAll("world!".encodeToByteArray())

            mock.close()
        }

    @Test
    fun writeWithHandle() =
        runTest {
            val (mock, handle) = Builder.new().buildWithHandle()
            handle.write("hello ".encodeToByteArray())
            handle.write("world!".encodeToByteArray())

            mock.writeAll("hello ".encodeToByteArray())
            mock.writeAll("world!".encodeToByteArray())

            mock.close()
        }

    @Test
    fun readWithHandle() =
        runTest {
            val (mock, handle) = Builder.new().buildWithHandle()
            handle.read("hello ".encodeToByteArray())
            handle.read("world!".encodeToByteArray())

            val buf = ByteArray(6)
            mock.readExact(buf)
            assertEquals("hello ", buf.decodeToString())
            mock.readExact(buf)
            assertEquals("world!", buf.decodeToString())

            mock.close()
        }

    @Test
    fun writeError() =
        runTest {
            val error = IllegalStateException("cruel")
            val mock =
                Builder
                    .new()
                    .write("hello ".encodeToByteArray())
                    .writeError(error)
                    .write("world!".encodeToByteArray())
                    .build()

            mock.writeAll("hello ".encodeToByteArray())

            val ex =
                assertFailsWith<IllegalStateException> {
                    mock.writeAll("whoa".encodeToByteArray())
                }
            assertEquals("cruel", ex.message)

            mock.writeAll("world!".encodeToByteArray())

            mock.close()
        }

    @Test
    fun mockPanicsReadDataLeft() {
        val mock = Builder.new().read("read".encodeToByteArray()).build()
        assertFailsWith<IllegalStateException> {
            mock.close()
        }
    }

    @Test
    fun mockPanicsWriteDataLeft() {
        val mock = Builder.new().write("write".encodeToByteArray()).build()
        assertFailsWith<IllegalStateException> {
            mock.close()
        }
    }

    @Test
    @kotlin.jvm.JvmName("testWait")
    fun wait() =
        runTest {
            val firstWait = FIRST_WAIT.milliseconds

            val mock =
                Builder
                    .new()
                    .wait(firstWait)
                    .read("hello ".encodeToByteArray())
                    .read("world!".encodeToByteArray())
                    .build()

            val buf = ByteArray(256)
            val start = testScheduler.currentTime

            val n1 = mock.read(buf)
            assertEquals("hello ", buf.decodeToString(0, n1))

            val n2 = mock.read(buf)
            assertEquals("world!", buf.decodeToString(0, n2))

            val elapsed = (testScheduler.currentTime - start).milliseconds
            assertTrue(elapsed >= firstWait, "consuming took $elapsed")

            mock.close()
        }

    @Test
    fun multipleWait() =
        runTest {
            val firstWait = FIRST_WAIT.milliseconds
            val secondWait = SECOND_WAIT.milliseconds

            val mock =
                Builder
                    .new()
                    .wait(firstWait)
                    .read("hello ".encodeToByteArray())
                    .wait(secondWait)
                    .read("world!".encodeToByteArray())
                    .build()

            val buf = ByteArray(256)
            val start = testScheduler.currentTime

            val n1 = mock.read(buf)
            assertEquals("hello ", buf.decodeToString(0, n1))

            val n2 = mock.read(buf)
            assertEquals("world!", buf.decodeToString(0, n2))

            val elapsed = (testScheduler.currentTime - start).milliseconds
            assertTrue(elapsed >= firstWait + secondWait, "consuming took $elapsed")

            mock.close()
        }
}

