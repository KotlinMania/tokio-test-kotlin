package io.github.kotlinmania.tokiotest

import kotlinx.coroutines.runBlocking

public actual fun <T> blockOn(block: suspend () -> T): T = runBlocking { block() }
