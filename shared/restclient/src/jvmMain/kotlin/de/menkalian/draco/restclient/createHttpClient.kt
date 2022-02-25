package de.menkalian.draco.restclient

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

internal actual fun createHttpClient(): HttpClient {
    return HttpClient(CIO)
}