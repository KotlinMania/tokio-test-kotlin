// port-lint: source macros.rs
package io.github.kotlinmania.tokiotest

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark

/**
 * A collection of useful assertions for testing futures and Tokio-based code.
 */
public sealed class Poll<out T> {
    public data class Ready<T>(
        val value: T,
    ) : Poll<T>()

    public data object Pending : Poll<Nothing>()
}

/**
 * Asserts a [Poll] is ready, returning the value.
 *
 * This invokes an [AssertionError] if the provided [Poll] is pending at runtime.
 */
public fun <T> assertReady(poll: Poll<T>, message: String? = null): T =
    when (poll) {
        is Poll.Ready -> poll.value
        Poll.Pending -> failWith(message, "pending")
    }

/**
 * Asserts a [Poll] carrying a [Result] is ready and successful, returning the value.
 */
public fun <T> assertReadyOk(poll: Poll<Result<T>>, message: String? = null): T =
    assertOk(assertReady(poll, message), message)

/**
 * Asserts a [Poll] carrying a [Result] is ready and failed, returning the exception.
 */
public fun assertReadyErr(poll: Poll<Result<*>>, message: String? = null): Throwable =
    assertErr(assertReady(poll, message), message)

/**
 * Asserts a [Poll] is pending.
 *
 * This invokes an [AssertionError] if the provided [Poll] is ready at runtime.
 */
public fun assertPending(poll: Poll<*>, message: String? = null) {
    when (poll) {
        is Poll.Ready -> failWith(message, "ready; value = ${poll.value}")
        Poll.Pending -> Unit
    }
}

/**
 * Asserts if a poll is ready and checks for equality on the value.
 */
public fun <T> assertReadyEq(poll: Poll<T>, expected: T, message: String? = null): T {
    val actual = assertReady(poll, message)
    if (actual != expected) {
        failWith(message, "assertion failed: expected <$expected>, actual <$actual>")
    }
    return actual
}

/**
 * Asserts that the result evaluates successfully and returns the value.
 */
public fun <T> assertOk(result: Result<T>, message: String? = null): T =
    result.getOrElse { error ->
        failWith(message, "assertion failed: Err($error)")
    }

/**
 * Asserts that the result evaluates to a failure and returns the exception.
 */
public fun assertErr(result: Result<*>, message: String? = null): Throwable =
    result.exceptionOrNull()
        ?: failWith(message, "assertion failed: Ok(${result.getOrNull()})")

/**
 * Asserts that an exact duration has elapsed since the start mark, with a 1 ms buffer.
 *
 * This 1 ms buffer is required because timer implementations have finite time resolution and
 * will not always sleep for the exact interval.
 */
public fun assertElapsed(start: TimeMark, duration: Duration, message: String? = null) {
    val elapsed = start.elapsedNow()
    if (elapsed < duration || elapsed > duration + 1.milliseconds) {
        failWith(message, "actual = $elapsed, expected = $duration")
    }
}

private fun failWith(message: String?, fallback: String): Nothing {
    if (message.isNullOrBlank()) {
        throw AssertionError(fallback)
    }
    throw AssertionError("$fallback; $message")
}
