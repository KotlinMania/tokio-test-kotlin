// port-lint: source macros.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.tokiotest

import kotlin.native.HiddenFromObjC

/**
 * Indicates whether a value is available for an asynchronous computation.
 */
@HiddenFromObjC
public sealed class Poll<out T> {
    /**
     * Represents that a value is immediately ready.
     */
    @HiddenFromObjC
    public data class Ready<out T>(
        public val value: T,
    ) : Poll<T>()

    /**
     * Represents that a value is not ready yet.
     */
    @HiddenFromObjC
    public data object Pending : Poll<Nothing>() {
        override fun toString(): String = "Pending"
    }

    public val isReady: Boolean get() = this is Ready
    public val isPending: Boolean get() = this is Pending
}
