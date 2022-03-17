package de.menkalian.draco

import io.ktor.client.HttpClient

internal expect fun createHttpClient(): HttpClient

internal expect fun currentSystemTimeMillis(): Long
