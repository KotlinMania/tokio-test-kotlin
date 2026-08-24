// port-lint: source task.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.tokiotest

import kotlin.native.HiddenFromObjC

/**
 * Spawns a task or computation into a [Spawn] harness.
 */
@HiddenFromObjC
public fun <T> spawn(task: T): Spawn<T> = Spawn(task)

/**
 * Mock task harness that wraps a task or computation.
 */
@HiddenFromObjC
public class Spawn<T>(
    private val inner: T,
) {
    private val mockTask: MockTask = MockTask()

    /**
     * Consumes or unwraps the inner value.
     */
    public fun intoInner(): T = inner

    /**
     * Returns true if the task has received a wake notification since the last call to [enter].
     */
    public fun isWoken(): Boolean = mockTask.isWoken()

    /**
     * Returns the reference count of the waker.
     */
    public fun wakerRefCount(): Int = mockTask.wakerRefCount()

    /**
     * Enters the task context.
     */
    public fun <R> enter(block: (MockTask) -> R): R = mockTask.enter { block(mockTask) }
}

/**
 * Tracks mock task state and wake notifications.
 */
@HiddenFromObjC
public class MockTask {
    private var woken: Boolean = false
    private var refCount: Int = 1

    public companion object {
        public fun new(): MockTask = MockTask()
    }

    public fun isWoken(): Boolean = woken

    public fun wake() {
        woken = true
    }

    public fun clear() {
        woken = false
    }

    public fun wakerRefCount(): Int = refCount

    public fun <R> enter(block: () -> R): R {
        clear()
        return block()
    }
}
