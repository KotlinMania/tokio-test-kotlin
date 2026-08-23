// port-lint: source task.rs
package io.github.kotlinmania.tokiotest

/**
 * Spawn a task into a [Spawn] which wraps it in a mocked executor harness.
 */
public fun <T> spawn(task: T): Spawn<T> = Spawn(MockTask(), task)

/**
 * Future or stream spawned on a mock task.
 */
public class Spawn<T> internal constructor(
    private val task: MockTask,
    private val inner: T,
) {
    /**
     * Consumes self returning the inner value.
     */
    public fun intoInner(): T = inner

    /**
     * Returns true if the inner future has received a wake notification since the last call to enter.
     */
    public fun isWoken(): Boolean = task.isWoken()

    /**
     * Returns the number of references to the task waker.
     */
    public fun wakerRefCount(): Int = task.wakerRefCount()

    /**
     * Enter the task context.
     */
    public fun <R> enter(block: (MockTask) -> R): R = task.enter(block)

    /**
     * Polls the future using the provided poll function under the task context.
     */
    public fun <R> poll(pollFn: (T) -> Poll<R>): Poll<R> =
        task.enter { pollFn(inner) }
}

/**
 * Mock task execution context tracking wake notifications.
 */
public class MockTask internal constructor() {
    private var woken: Boolean = false
    private var refCount: Int = 1

    /**
     * Clears any previously received wakes.
     */
    public fun clear() {
        woken = false
    }

    /**
     * Returns true if the task has received a wake notification since the last call to enter.
     */
    public fun isWoken(): Boolean = woken

    /**
     * Returns the waker reference count.
     */
    public fun wakerRefCount(): Int = refCount

    /**
     * Triggers a wake notification.
     */
    public fun wake() {
        woken = true
    }

    /**
     * Runs a block from the context of the task.
     */
    public fun <R> enter(block: (MockTask) -> R): R {
        clear()
        return block(this)
    }
}
