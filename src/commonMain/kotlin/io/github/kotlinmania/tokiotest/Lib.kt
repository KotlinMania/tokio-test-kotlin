// port-lint: source lib.rs
package io.github.kotlinmania.tokiotest

/**
 * Runs the provided suspending block, blocking until it completes.
 */
public expect fun <T> blockOn(block: suspend () -> T): T

/**
 * Module descriptor for the tokiotest crate.
 */
public object TokioTestLib {
    public const val MODULE_NAME: String = "tokiotest"
    public const val CRATE_NAME: String = "tokiotest"
}
