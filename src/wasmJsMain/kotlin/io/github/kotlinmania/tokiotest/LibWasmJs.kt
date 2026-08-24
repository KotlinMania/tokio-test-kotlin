package io.github.kotlinmania.tokiotest

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

public actual fun <T> blockOn(block: suspend () -> T): T {
    var finalResult: Result<T>? = null
    block.startCoroutine(
        object : Continuation<T> {
            override val context: CoroutineContext = EmptyCoroutineContext

            override fun resumeWith(result: Result<T>) {
                finalResult = result
            }
        },
    )
    val res =
        finalResult
            ?: error("blockOn on Wasm-JS requires coroutines to complete synchronously; use suspend functions or runTest directly")
    return res.getOrThrow()
}
