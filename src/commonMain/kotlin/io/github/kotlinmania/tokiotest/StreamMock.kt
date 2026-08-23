// port-lint: source stream_mock.rs
package io.github.kotlinmania.tokiotest

import kotlin.time.Duration

internal sealed class StreamAction<out T> {
    data class Next<T>(
        val value: T,
    ) : StreamAction<T>()

    data class Wait(
        val duration: Duration,
    ) : StreamAction<Nothing>()
}

/**
 * A builder for [StreamMock].
 */
public class StreamMockBuilder<T> {
    private val actions: ArrayDeque<StreamAction<T>> = ArrayDeque()

    public fun next(value: T): StreamMockBuilder<T> {
        actions.addLast(StreamAction.Next(value))
        return this
    }

    public fun wait(duration: Duration): StreamMockBuilder<T> {
        actions.addLast(StreamAction.Wait(duration))
        return this
    }

    public fun build(): StreamMock<T> = StreamMock(ArrayDeque(actions))

    public companion object {
        public fun <T> new(): StreamMockBuilder<T> = StreamMockBuilder()
    }
}

/**
 * A mock stream implementing stream polling behavior.
 */
public class StreamMock<T> internal constructor(
    private val actions: ArrayDeque<StreamAction<T>>,
) : AutoCloseable {
    private var waiting: Boolean = false

    private fun nextAction(): StreamAction<T>? =
        if (actions.isEmpty()) null else actions.removeFirst()

    /**
     * Polls the next item from the stream.
     */
    public fun pollNext(): Poll<T?> {
        if (waiting) {
            waiting = false
        }
        val action = nextAction() ?: return Poll.Ready(null)
        return when (action) {
            is StreamAction.Next -> Poll.Ready(action.value)
            is StreamAction.Wait -> {
                waiting = true
                Poll.Pending
            }
        }
    }

    /**
     * Checks if all actions were consumed before dropping/closing.
     */
    public fun verifyAllConsumed() {
        val undroppedCount = actions.count { it is StreamAction.Next }
        check(undroppedCount == 0) {
            "StreamMock was dropped before all actions were consumed, $undroppedCount actions were not consumed"
        }
    }

    override fun close() {
        verifyAllConsumed()
    }
}
