package de.menkalian.draco.restclient

import io.ktor.client.HttpClient

internal expect fun createHttpClient(): HttpClient
