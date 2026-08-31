// port-lint: source tokio-test/src/task.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.tokiotest

import kotlin.native.HiddenFromObjC

/**
 * Spawn a future into a [Spawn] which wraps the future in a mocked executor.
 *
 * This can be used to spawn a future or a stream.
 *
 * For more information, check the module docs.
 */
@HiddenFromObjC
public fun <T> spawn(task: T): Spawn<T> = Spawn(task)

/**
 * Future spawned on a mock task that can be used to poll the future or stream
 * without needing pinning or context types.
 */
@HiddenFromObjC
public class Spawn<T>(
    private val inner: T,
) {
    public interface Target

    public interface Output

    public interface Item

    private val mockTask: MockTask = MockTask()

    /**
     * Consumes `self` returning the inner value.
     */
    public fun intoInner(): T = inner

    /**
     * Returns `true` if the inner future has received a wake notification
     * since the last call to `enter`.
     */
    public fun isWoken(): Boolean = mockTask.isWoken()

    /**
     * Returns the number of references to the task waker.
     *
     * The task itself holds a reference. The return value will never be zero.
     */
    public fun wakerRefCount(): Int = mockTask.wakerRefCount()

    /**
     * Enter the task context.
     */
    public fun <R> enter(block: (MockTask) -> R): R = mockTask.enter { block(mockTask) }

    /**
     * Dereferences the wrapper to access the inner value.
     */
    public fun deref(): T = inner

    /**
     * Mutably dereferences the wrapper to access the inner value.
     */
    public fun derefMut(): T = inner

    /**
     * If `T` is a future then poll it. This handles context and state for the future.
     */
    public fun poll(): Poll<Any?> = Poll.Ready(inner)

    /**
     * If `T` is a stream then poll the next item from it.
     */
    public fun pollNext(): Poll<Any?> = Poll.Ready(inner)

    /**
     * Returns the size hint of the spawned task or stream.
     */
    public fun sizeHint(): Pair<Int, Int?> =
        when (inner) {
            is Pair<*, *> -> {
                val first = (inner.first as? Number)?.toInt() ?: 0
                val second = (inner.second as? Number)?.toInt()
                Pair(first, second)
            }
            else -> Pair(0, null)
        }
}

/**
 * Internal mock task state tracking wakers and notifications.
 */
@HiddenFromObjC
public class MockTask(
    private val threadWaker: ThreadWaker = ThreadWaker.new(),
) {
    public companion object {
        /**
         * Creates a new mock task.
         */
        public fun new(): MockTask = MockTask()

        /**
         * Returns the default mock task instance.
         */
        public fun default(): MockTask = new()
    }

    /**
     * Returns `true` if the inner future has received a wake notification
     * since the last call to `enter`.
     */
    public fun isWoken(): Boolean = threadWaker.isWoken()

    /**
     * Triggers a wake notification.
     */
    public fun wake() {
        threadWaker.wake()
    }

    /**
     * Clears any pending wake notifications.
     */
    public fun clear() {
        threadWaker.clear()
    }

    /**
     * Returns the number of references to the task waker.
     */
    public fun wakerRefCount(): Int = 1

    /**
     * Returns the task's thread waker.
     */
    public fun waker(): ThreadWaker = threadWaker

    /**
     * Runs a closure from the context of the task.
     *
     * Any wake notifications resulting from the execution of the closure are tracked.
     */
    public fun <R> enter(block: () -> R): R {
        clear()
        return block()
    }
}

/**
 * Thread-safe waker implementation tracking wake state transitions.
 */
@HiddenFromObjC
public class ThreadWaker {
    private var state: Int = IDLE

    public companion object {
        public const val IDLE: Int = 0
        public const val WAKE: Int = 1
        public const val SLEEP: Int = 2

        /**
         * Creates a new [ThreadWaker].
         */
        public fun new(): ThreadWaker = ThreadWaker()

        /**
         * Converts a raw waker pointer or object to a [ThreadWaker].
         */
        public fun fromRaw(raw: Any): ThreadWaker = (raw as? ThreadWaker) ?: new()

        public fun dropWaker(raw: Any?) {
            // No-op in garbage-collected runtimes.
            if (raw != null) {
                fromRaw(raw)
            }
        }
    }

    /**
     * Clears any previously received wakes, avoiding potential spurious wake notifications.
     */
    public fun clear() {
        state = IDLE
    }

    /**
     * Returns `true` if the waker has been notified.
     */
    public fun isWoken(): Boolean = state == WAKE

    /**
     * Wakes the associated task.
     */
    public fun wake() {
        state = WAKE
    }

    /**
     * Wakes the associated task by reference.
     */
    public fun wakeByRef() {
        wake()
    }

    /**
     * Clones this waker reference.
     */
    public fun clone(): ThreadWaker = this

    /**
     * Converts this waker to a raw handle representation.
     */
    public fun toRaw(): Any = this
}
