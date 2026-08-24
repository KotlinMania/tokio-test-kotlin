// port-lint: source macros.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.tokiotest

import kotlin.native.HiddenFromObjC
import kotlin.time.Duration
import kotlin.time.TimeMark

/**
 * Asserts a [Poll] is ready, returning the value.
 */
@HiddenFromObjC
public fun <T> assertReady(poll: Poll<T>, message: String? = null): T =
    when (poll) {
        is Poll.Ready -> poll.value
        is Poll.Pending -> {
            val prefix = if (message != null) "pending; $message" else "pending"
            throw AssertionError(prefix)
        }
    }

/**
 * Asserts a [Poll] is pending.
 */
@HiddenFromObjC
public fun <T> assertPending(poll: Poll<T>, message: String? = null) {
    when (poll) {
        is Poll.Pending -> {}
        is Poll.Ready -> {
            val detail = "ready; value = ${poll.value}"
            val msg = if (message != null) "$detail; $message" else detail
            throw AssertionError(msg)
        }
    }
}

/**
 * Asserts a [Poll] is ready and check for equality on the value.
 */
@HiddenFromObjC
public fun <T> assertReadyEq(poll: Poll<T>, expected: T, message: String? = null) {
    val actual = assertReady(poll, message)
    if (actual != expected) {
        val detail = "expected: <$expected> but was: <$actual>"
        val msg = if (message != null) "$detail; $message" else detail
        throw AssertionError(msg)
    }
}

/**
 * Asserts that the expression evaluates to successful result and returns the value.
 */
@HiddenFromObjC
public fun <T> assertOk(result: Result<T>, message: String? = null): T {
    if (result.isSuccess) {
        return result.getOrThrow()
    }
    val exception = result.exceptionOrNull()
    val detail = "assertion failed: Err($exception)"
    val msg = if (message != null) "$detail: $message" else detail
    throw AssertionError(msg)
}

/**
 * Asserts that the expression evaluates to failure and returns the error.
 */
@HiddenFromObjC
public fun <T> assertErr(result: Result<T>, message: String? = null): Throwable {
    if (result.isFailure) {
        return result.exceptionOrNull()!!
    }
    val value = result.getOrNull()
    val detail = "assertion failed: Ok($value)"
    val msg = if (message != null) "$detail: $message" else detail
    throw AssertionError(msg)
}

/**
 * Asserts a [Poll] of [Result] is ready and successful, returning the inner value.
 */
@HiddenFromObjC
public fun <T> assertReadyOk(poll: Poll<Result<T>>, message: String? = null): T {
    val res = assertReady(poll, message)
    return assertOk(res, message)
}

/**
 * Asserts a [Poll] of [Result] is ready and failure, returning the exception.
 */
@HiddenFromObjC
public fun <T> assertReadyErr(poll: Poll<Result<T>>, message: String? = null): Throwable {
    val res = assertReady(poll, message)
    return assertErr(res, message)
}

/**
 * Asserts that an elapsed duration since [start] is at least [expected].
 */
@HiddenFromObjC
public fun assertElapsed(start: TimeMark, expected: Duration, message: String? = null) {
    val elapsed = start.elapsedNow()
    if (elapsed < expected) {
        throw AssertionError(message ?: "actual = $elapsed, expected at least = $expected")
    }
}
