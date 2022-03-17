package de.menkalian.draco

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

internal actual fun createHttpClient(): HttpClient {
    return HttpClient(CIO)
}

internal actual fun currentSystemTimeMillis(): Long {
    return System.currentTimeMillis()
}