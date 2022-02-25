package de.menkalian.draco.restclient

import io.ktor.client.HttpClient
import io.ktor.client.engine.curl.Curl

actual fun createHttpClient(): HttpClient {
    return HttpClient(Curl)
}