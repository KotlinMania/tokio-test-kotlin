// port-lint: source stream_mock.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.tokiotest

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlin.native.HiddenFromObjC
import kotlin.time.Duration

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
 */
@HiddenFromObjC
public class StreamMockBuilder<T> {
    private val actions: ArrayDeque<StreamAction<T>> = ArrayDeque()

    public companion object {
        public fun <T> new(): StreamMockBuilder<T> = StreamMockBuilder()
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
    private var isClosed = false

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

    override fun close() {
        if (!isClosed) {
            isClosed = true
            if (actions.isNotEmpty()) {
                throw IllegalStateException("StreamMock was dropped before all actions were consumed")
            }
        }
    }
}
