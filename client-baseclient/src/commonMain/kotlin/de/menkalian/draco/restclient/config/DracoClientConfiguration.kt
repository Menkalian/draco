package de.menkalian.draco.restclient.config

import de.menkalian.draco.restclient.logger.Logger
import de.menkalian.draco.restclient.logger.StdoutLogger
import io.ktor.client.features.logging.DEFAULT
import io.ktor.client.features.logging.LogLevel

data class DracoClientConfiguration(
    val useHttps: Boolean,
    val serverUrl: String,
    val serverPort: Short,
    val authToken: String?,
    val connectionTimeoutMs: Long?,
    val requestTimeoutMs: Long?,
    val socketTimeoutMs: Long?,
    val ktorLogger: io.ktor.client.features.logging.Logger,
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

        private var ktorLogger: io.ktor.client.features.logging.Logger = io.ktor.client.features.logging.Logger.DEFAULT
        private var logger: Logger = StdoutLogger()
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

        fun ktorLogger(logger: io.ktor.client.features.logging.Logger) = apply {
            this.ktorLogger = logger
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
            ktorLogger,
            logger,
            logLevel
        )
    }
}
