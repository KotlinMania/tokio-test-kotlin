// port-lint: source tokio-test/src/lib.rs
package io.github.kotlinmania.tokiotest

/**
 * Runs the provided asynchronous computation, blocking the current thread until the
 * computation completes.
 */
public expect fun <T> blockOn(block: suspend () -> T): T

/**
 * Runs the provided future or coroutine block, blocking the current thread until completion.
 */
public fun <T> blockOnTask(block: suspend () -> T): T = blockOn(block)

/**
 * Module descriptor for the tokiotest crate.
 */
public object TokioTestLib {
    public const val MODULE_NAME: String = "tokiotest"
    public const val CRATE_NAME: String = "tokiotest"
}
