package de.menkalian.draco

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import kotlin.js.Date

internal actual fun createHttpClient(): HttpClient {
    return HttpClient(Js)
}

internal actual fun currentSystemTimeMillis(): Long {
    return Date.now().toLong()
}