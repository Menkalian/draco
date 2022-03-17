package de.menkalian.draco

import io.ktor.client.HttpClient
import io.ktor.client.engine.curl.Curl
import kotlin.system.getTimeMillis

actual fun createHttpClient(): HttpClient {
    return HttpClient(Curl)
}

internal actual fun currentSystemTimeMillis(): Long {
    return getTimeMillis()
}