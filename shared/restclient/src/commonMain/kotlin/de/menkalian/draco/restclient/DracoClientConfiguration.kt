package de.menkalian.draco.restclient

import io.ktor.client.features.logging.DEFAULT
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logger

data class DracoClientConfiguration(
    val useHttps: Boolean,
    val serverUrl: String,
    val serverPort: Short,
    val authToken: String?,
    val connectionTimeoutMs: Long?,
    val requestTimeoutMs: Long?,
    val socketTimeoutMs: Long?,
    val logger: Logger,
    val logLevel: LogLevel,
) {
    class DracoClientConfigurationBuilder {
        private var useHttps: Boolean = true
        private var serverUrl: String = "draco.menkalian.de"
        private var serverPort: Short = 443
        private var authToken: String? = null

        private var connectionTimeoutMs: Long? = 5000L
        private var requestTimeoutMs: Long? = 5000L
        private var socketTimeoutMs: Long? = 5000L

        private var logger: Logger = Logger.DEFAULT
        private var logLevel: LogLevel = LogLevel.BODY

        fun useHttps(useHttps: Boolean) = apply {
            this.useHttps = useHttps
        }

        fun serverUrl(serverUrl: String) = apply {
            this.serverUrl = serverUrl
        }

        fun serverPort(serverPort: Short) = apply {
            this.serverPort = serverPort
        }

        fun authToken(authToken: String?) = apply {
            this.authToken = authToken
        }

        fun connectionTimeoutMs(connectionTimeoutMs: Long?) = apply {
            this.connectionTimeoutMs = connectionTimeoutMs
        }

        fun requestTimeoutMs(requestTimeoutMs: Long?) = apply {
            this.requestTimeoutMs = requestTimeoutMs
        }

        fun socketTimeoutMs(socketTimeoutMs: Long?) = apply {
            this.socketTimeoutMs = socketTimeoutMs
        }

        fun logger(logger: Logger) = apply {
            this.logger = logger
        }

        fun logLevel(logLevel: LogLevel) = apply {
            this.logLevel = logLevel
        }

        fun build() = DracoClientConfiguration(
            useHttps,
            serverUrl,
            serverPort,
            authToken,
            connectionTimeoutMs,
            requestTimeoutMs,
            socketTimeoutMs,
            logger,
            logLevel
        )
    }
}
