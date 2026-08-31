// port-lint: source io.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.tokiotest

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.native.HiddenFromObjC
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * An action scheduled on a [Mock] or submitted via [Handle].
 */
public typealias Action = IoAction

/**
 * An action scheduled on a [Mock] or submitted via [Handle].
 */
@HiddenFromObjC
public sealed class IoAction {
    public data class Read(
        val data: ByteArray,
    ) : IoAction() {
        override fun equals(other: Any?): Boolean =
            this === other || (other is Read && data.contentEquals(other.data))

        override fun hashCode(): Int = data.contentHashCode()
    }

    public data class Write(
        val data: ByteArray,
    ) : IoAction() {
        override fun equals(other: Any?): Boolean =
            this === other || (other is Write && data.contentEquals(other.data))

        override fun hashCode(): Int = data.contentHashCode()
    }

    public data class Wait(
        val duration: Duration,
    ) : IoAction()

    public data class ReadError(
        val error: Throwable?,
    ) : IoAction()

    public data class WriteError(
        val error: Throwable?,
    ) : IoAction()
}

/**
 * Internal state for [Mock].
 */
@HiddenFromObjC
public class Inner(
    public val actions: ArrayDeque<IoAction> = ArrayDeque(),
    public val name: String = "",
) {
    public fun action(): IoAction? = actions.firstOrNull()

    public fun pollAction(cx: Any? = null): Poll<IoAction?> {
        cx?.hashCode()
        val next = actions.removeFirstOrNull()
        return Poll.Ready(next)
    }

    public fun fmt(): String =
        if (name.isEmpty()) "Inner {...}" else "Inner {name=$name, ...}"

    override fun toString(): String = fmt()
}

/**
 * Panic message formatting snippet for [Mock].
 */
@HiddenFromObjC
public class PanicMsgSnippet(
    public val inner: Inner,
) {
    public fun fmt(): String =
        if (inner.name.isEmpty()) {
            "(${inner.actions.size} actions remain)"
        } else {
            "(name ${inner.name}, ${inner.actions.size} actions remain)"
        }

    override fun toString(): String = fmt()
}

/**
 * Result of building a [Mock] paired with a [Handle].
 */
@HiddenFromObjC
public data class MockAndHandle(
    val mock: Mock,
    val handle: Handle,
)

/**
 * Builds [Mock] instances.
 */
@HiddenFromObjC
public class Builder {
    private val actions: ArrayDeque<IoAction> = ArrayDeque()
    private var name: String = ""

    /**
     * Return a new, empty [Builder].
     */
    public companion object {
        public fun new(): Builder = Builder()

        public fun default(): Builder = Builder()
    }

    /**
     * Sequence a `read` operation.
     *
     * The next operation in the mock's script will be to expect a `read` call
     * and return `buf`.
     */
    public fun read(buf: ByteArray): Builder {
        actions.addLast(IoAction.Read(buf.copyOf()))
        return this
    }

    /**
     * Sequence a `read` operation that produces an error.
     *
     * The next operation in the mock's script will be to expect a `read` call
     * and return `error`.
     */
    public fun readError(error: Throwable): Builder {
        actions.addLast(IoAction.ReadError(error))
        return this
    }

    /**
     * Sequence a `write` operation.
     *
     * The next operation in the mock's script will be to expect a `write`
     * call.
     */
    public fun write(buf: ByteArray): Builder {
        actions.addLast(IoAction.Write(buf.copyOf()))
        return this
    }

    /**
     * Sequence a `write` operation that produces an error.
     *
     * The next operation in the mock's script will be to expect a `write`
     * call that provides `error`.
     */
    public fun writeError(error: Throwable): Builder {
        actions.addLast(IoAction.WriteError(error))
        return this
    }

    /**
     * Sequence a wait.
     *
     * The next operation in the mock's script will be to wait without doing so
     * for `duration` amount of time.
     */
    public fun wait(duration: Duration): Builder {
        val clamped = if (duration < 1.milliseconds) 1.milliseconds else duration
        actions.addLast(IoAction.Wait(clamped))
        return this
    }

    /**
     * Set name of the mock IO object to include in panic messages and debug output.
     */
    public fun name(name: String): Builder {
        this.name = name
        return this
    }

    /**
     * Build a [Mock] value according to the defined script.
     */
    public fun build(): Mock {
        val (mock, _) = buildWithHandle()
        return mock
    }

    /**
     * Build a [Mock] value paired with a handle.
     */
    public fun buildWithHandle(): MockAndHandle {
        val channel = Channel<IoAction>(Channel.UNLIMITED)
        val copy =
            ArrayDeque(
                actions.map { action ->
                    when (action) {
                        is IoAction.Read -> IoAction.Read(action.data.copyOf())
                        is IoAction.Write -> IoAction.Write(action.data.copyOf())
                        is IoAction.Wait -> IoAction.Wait(action.duration)
                        is IoAction.ReadError -> IoAction.ReadError(action.error)
                        is IoAction.WriteError -> IoAction.WriteError(action.error)
                    }
                },
            )
        val mock = Mock(copy, channel, name)
        val handle = Handle(channel)
        return MockAndHandle(mock, handle)
    }
}

/**
 * A handle to send additional actions to the related [Mock].
 */
@HiddenFromObjC
public class Handle internal constructor(
    private val channel: Channel<IoAction>,
) {
    /**
     * Sequence a `read` operation.
     */
    public fun read(buf: ByteArray): Handle {
        channel.trySend(IoAction.Read(buf.copyOf()))
        return this
    }

    /**
     * Sequence a `read` operation error.
     */
    public fun readError(error: Throwable): Handle {
        channel.trySend(IoAction.ReadError(error))
        return this
    }

    /**
     * Sequence a `write` operation.
     */
    public fun write(buf: ByteArray): Handle {
        channel.trySend(IoAction.Write(buf.copyOf()))
        return this
    }

    /**
     * Sequence a `write` operation error.
     */
    public fun writeError(error: Throwable): Handle {
        channel.trySend(IoAction.WriteError(error))
        return this
    }

    /**
     * Sequence a wait duration.
     */
    public fun wait(duration: Duration): Handle {
        channel.trySend(IoAction.Wait(duration))
        return this
    }
}

/**
 * An I/O object that follows a predefined script.
 *
 * This value is created by [Builder] and implements mock read and write operations.
 * It follows the scenario described by the builder and panics otherwise.
 */
@HiddenFromObjC
public class Mock internal constructor(
    private val actions: ArrayDeque<IoAction>,
    private val channel: Channel<IoAction>,
    private val name: String,
) : AutoCloseable {
    private fun drainChannel() {
        while (true) {
            val next = channel.tryReceive().getOrNull() ?: break
            actions.addLast(next)
        }
    }

    public val inner: Inner get() = Inner(actions, name)

    public fun pmsg(): PanicMsgSnippet = PanicMsgSnippet(inner)

    public fun action(): IoAction? = actions.firstOrNull()

    public fun pollAction(cx: Any? = null): Poll<IoAction?> {
        cx?.hashCode()
        drainChannel()
        val next = actions.removeFirstOrNull()
        return Poll.Ready(next)
    }

    public fun drop() {
        close()
    }

    /**
     * Returns the remaining wait duration if the current scheduled action is a wait.
     */
    public fun remainingWait(): Duration? {
        drainChannel()
        val first = actions.firstOrNull()
        return if (first is IoAction.Wait) first.duration else null
    }

    /**
     * Wakes up waiting readers if a read action is ready.
     */
    public fun maybeWakeupReader() {
        // In coroutine context, channel state updates wake suspended readers automatically
    }

    /**
     * Polls a read operation against the mock.
     */
    public fun pollRead(
        dst: ByteArray,
        offset: Int = 0,
        length: Int = dst.size - offset,
    ): Poll<Result<Int>> {
        drainChannel()
        if (actions.isEmpty()) {
            return Poll.Ready(Result.success(0))
        }
        val current = actions.first()
        return when (current) {
            is IoAction.Read -> {
                actions.removeFirst()
                val data = current.data
                val n = min(length, data.size)
                data.copyInto(dst, destinationOffset = offset, startIndex = 0, endIndex = n)
                if (data.size > n) {
                    val remaining = data.copyOfRange(n, data.size)
                    actions.addFirst(IoAction.Read(remaining))
                }
                Poll.Ready(Result.success(n))
            }
            is IoAction.ReadError -> {
                actions.removeFirst()
                val err = current.error ?: IllegalStateException("Mock read error")
                Poll.Ready(Result.failure(err))
            }
            is IoAction.Wait -> {
                actions.removeFirst()
                Poll.Pending
            }
            else -> Poll.Pending
        }
    }

    /**
     * Suspending read operation.
     */
    public suspend fun read(
        dst: ByteArray,
        offset: Int = 0,
        length: Int = dst.size - offset,
    ): Int {
        while (true) {
            drainChannel()
            if (actions.isEmpty()) {
                return 0
            }
            val current = actions.first()
            when (current) {
                is IoAction.Wait -> {
                    actions.removeFirst()
                    delay(current.duration)
                }
                is IoAction.Read -> {
                    actions.removeFirst()
                    val data = current.data
                    val n = min(length, data.size)
                    data.copyInto(dst, destinationOffset = offset, startIndex = 0, endIndex = n)
                    if (data.size > n) {
                        val remaining = data.copyOfRange(n, data.size)
                        actions.addFirst(IoAction.Read(remaining))
                    }
                    return n
                }
                is IoAction.ReadError -> {
                    actions.removeFirst()
                    val err = current.error ?: IllegalStateException("Mock read error")
                    throw err
                }
                else -> {
                    val next = channel.receive()
                    actions.addLast(next)
                }
            }
        }
    }

    /**
     * Suspending exact read operation filling the full requested range.
     */
    public suspend fun readExact(
        dst: ByteArray,
        offset: Int = 0,
        length: Int = dst.size - offset,
    ) {
        var readBytes = 0
        while (readBytes < length) {
            val n = read(dst, offset + readBytes, length - readBytes)
            if (n == 0) {
                throw IllegalStateException("Unexpected EOF before reading exact length $length: ${pmsg()}")
            }
            readBytes += n
        }
    }

    /**
     * Polls a write operation against the mock.
     */
    public fun pollWrite(
        src: ByteArray,
        offset: Int = 0,
        length: Int = src.size - offset,
    ): Poll<Result<Int>> {
        drainChannel()
        if (actions.isEmpty()) {
            return Poll.Ready(Result.failure(IllegalStateException("broken pipe / unexpected write ${pmsg()}")))
        }
        val current = actions.first()
        return when (current) {
            is IoAction.Write -> {
                actions.removeFirst()
                val expected = current.data
                val n = min(length, expected.size)
                for (i in 0 until n) {
                    val actualByte = src[offset + i]
                    val expectedByte = expected[i]
                    if (actualByte != expectedByte) {
                        return Poll.Ready(
                            Result.failure(
                                AssertionError(
                                    "Write mismatch name=$name byte=$i expected=$expectedByte actual=$actualByte",
                                ),
                            ),
                        )
                    }
                }
                if (expected.size > n) {
                    val remaining = expected.copyOfRange(n, expected.size)
                    actions.addFirst(IoAction.Write(remaining))
                }
                maybeWakeupReader()
                Poll.Ready(Result.success(n))
            }
            is IoAction.WriteError -> {
                actions.removeFirst()
                val err = current.error ?: IllegalStateException("Mock write error")
                Poll.Ready(Result.failure(err))
            }
            is IoAction.Wait -> {
                actions.removeFirst()
                Poll.Pending
            }
            else -> Poll.Pending
        }
    }

    /**
     * Suspending write operation.
     */
    public suspend fun write(
        src: ByteArray,
        offset: Int = 0,
        length: Int = src.size - offset,
    ): Int {
        while (true) {
            drainChannel()
            if (actions.isEmpty()) {
                throw IllegalStateException("broken pipe / unexpected write ${pmsg()}")
            }
            val current = actions.first()
            when (current) {
                is IoAction.Wait -> {
                    actions.removeFirst()
                    delay(current.duration)
                }
                is IoAction.Write -> {
                    actions.removeFirst()
                    val expected = current.data
                    val n = min(length, expected.size)
                    for (i in 0 until n) {
                        val actualByte = src[offset + i]
                        val expectedByte = expected[i]
                        if (actualByte != expectedByte) {
                            throw AssertionError(
                                "Write mismatch name=$name byte=$i expected=$expectedByte actual=$actualByte",
                            )
                        }
                    }
                    if (expected.size > n) {
                        val remaining = expected.copyOfRange(n, expected.size)
                        actions.addFirst(IoAction.Write(remaining))
                    }
                    maybeWakeupReader()
                    return n
                }
                is IoAction.WriteError -> {
                    actions.removeFirst()
                    val err = current.error ?: IllegalStateException("Mock write error")
                    throw err
                }
                else -> {
                    val next = channel.receive()
                    actions.addLast(next)
                }
            }
        }
    }

    /**
     * Suspending write operation that writes all data.
     */
    public suspend fun writeAll(
        src: ByteArray,
        offset: Int = 0,
        length: Int = src.size - offset,
    ) {
        var written = 0
        while (written < length) {
            val n = write(src, offset + written, length - written)
            written += n
        }
    }

    /**
     * Polls flush against the mock.
     */
    public fun pollFlush(): Poll<Result<Unit>> = Poll.Ready(Result.success(Unit))

    /**
     * Polls shutdown against the mock.
     */
    public fun pollShutdown(): Poll<Result<Unit>> = Poll.Ready(Result.success(Unit))

    /**
     * Verifies that all expected actions were consumed.
     */
    public fun verifyAllConsumed() {
        drainChannel()
        for (action in actions) {
            when (action) {
                is IoAction.Read ->
                    check(action.data.isEmpty()) {
                        "There is still data left to read. ${pmsg()}"
                    }
                is IoAction.Write ->
                    check(action.data.isEmpty()) {
                        "There is still data left to write. ${pmsg()}"
                    }
                else -> Unit
            }
        }
    }

    override fun close() {
        verifyAllConsumed()
    }
}
