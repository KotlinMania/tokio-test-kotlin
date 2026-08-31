// port-lint: source tokio-test/src/stream_mock.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.tokiotest

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlin.native.HiddenFromObjC
import kotlin.time.Duration

/**
 * Marker interface for stream item types.
 */
@HiddenFromObjC
public interface Item

/**
 * An action in a [StreamMock] script.
 */
@HiddenFromObjC
public sealed class StreamAction<out T> {
    public data class Next<out T>(
        val value: T,
    ) : StreamAction<T>()

    public data class Wait(
        val duration: Duration,
    ) : StreamAction<Nothing>()
}

/**
 * A builder for [StreamMock].
 *
 * Allows enqueueing actions such as returning items or waiting for a certain duration.
 */
@HiddenFromObjC
public class StreamMockBuilder<T> {
    private val actions: ArrayDeque<StreamAction<T>> = ArrayDeque()

    public companion object {
        /**
         * Create a new empty [StreamMockBuilder].
         */
        public fun <T> new(): StreamMockBuilder<T> = StreamMockBuilder()

        /**
         * Return a default empty [StreamMockBuilder].
         */
        public fun <T> default(): StreamMockBuilder<T> = new()
    }

    /**
     * Queue an item to be returned by the stream.
     */
    public fun next(value: T): StreamMockBuilder<T> {
        actions.addLast(StreamAction.Next(value))
        return this
    }

    /**
     * Queue the stream to wait for a duration.
     */
    public fun wait(duration: Duration): StreamMockBuilder<T> {
        actions.addLast(StreamAction.Wait(duration))
        return this
    }

    /**
     * Build the [StreamMock].
     */
    public fun build(): StreamMock<T> = StreamMock(ArrayDeque(actions))
}

/**
 * A mock stream implementing asynchronous sequential access to mocked items and delays.
 */
@HiddenFromObjC
public class StreamMock<T> internal constructor(
    private val actions: ArrayDeque<StreamAction<T>>,
) : Flow<T>,
    AutoCloseable {
    public sealed class Action<out T> {
        public data class Next<out T>(
            val value: T,
        ) : Action<T>()

        public data class Wait(
            val duration: Duration,
        ) : Action<Nothing>()
    }

    private var isClosed = false

    /**
     * Retrieves the next action in the script without consuming it.
     */
    public fun nextAction(): StreamAction<T>? = actions.firstOrNull()

    /**
     * Polls the next item from the stream.
     */
    public fun pollNext(): Poll<T?> {
        val next = actions.removeFirstOrNull() ?: return Poll.Ready(null)
        return when (next) {
            is StreamAction.Next -> Poll.Ready(next.value)
            is StreamAction.Wait -> Poll.Pending
        }
    }

    /**
     * Retrieves the next item in the stream, or `null` if exhausted.
     */
    public suspend fun next(): T? {
        while (actions.isNotEmpty()) {
            when (val action = actions.removeFirst()) {
                is StreamAction.Wait -> {
                    delay(action.duration)
                }
                is StreamAction.Next -> {
                    return action.value
                }
            }
        }
        return null
    }

    override suspend fun collect(collector: FlowCollector<T>) {
        while (true) {
            val item = next() ?: break
            collector.emit(item)
        }
    }

    /**
     * Drops or closes the stream, ensuring all scheduled actions were consumed.
     */
    public fun drop() {
        close()
    }

    override fun close() {
        if (!isClosed) {
            isClosed = true
            if (actions.isNotEmpty()) {
                throw IllegalStateException("StreamMock was dropped before all actions were consumed")
            }
        }
    }
}
