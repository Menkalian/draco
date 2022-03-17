package de.menkalian.draco.data.game

import de.menkalian.draco.data.Draco
import de.menkalian.draco.data.game.values.TransferableValue
import de.menkalian.draco.data.game.values.ValueHolder
import kotlin.properties.Delegates

@kotlinx.serialization.Serializable
class ConnectionSettings constructor() : ValueHolder() {
    constructor(builder: ConnectionSettingsBuilder) : this() {
        this.restUseTls = restUseTls
        this.restHost = restHost
        this.restPort = restPort
        this.wsUseTls = wsUseTls
        this.wsHost = wsHost
        this.wsPort = wsPort
        this.wsPath = wsPath
        this.heartbeatRate = heartbeatRate
        this.heartbeatMaxMisses = heartbeatMaxMisses
    }

    var restUseTls: Boolean by Delegates.observable(false) { _, _, new ->
        notifyChange(Draco.Connection.Server.REST.TLS, TransferableValue.from(new))
    }

    var restHost: String by Delegates.observable("") { _, _, new ->
        notifyChange(Draco.Connection.Server.REST.Host, TransferableValue.from(new))
    }

    var restPort: Int by Delegates.observable(0) { _, _, new ->
        notifyChange(Draco.Connection.Server.REST.Port, TransferableValue.from(new))
    }

    var wsUseTls: Boolean by Delegates.observable(false) { _, _, new ->
        notifyChange(Draco.Connection.Server.WS.TLS, TransferableValue.from(new))
    }

    var wsHost: String by Delegates.observable("") { _, _, new ->
        notifyChange(Draco.Connection.Server.WS.Host, TransferableValue.from(new))
    }

    var wsPort: Int by Delegates.observable(0) { _, _, new ->
        notifyChange(Draco.Connection.Server.WS.Port, TransferableValue.from(new))
    }

    var wsPath: String by Delegates.observable("") { _, _, new ->
        notifyChange(Draco.Connection.Server.WS.Path, TransferableValue.from(new))
    }

    var heartbeatRate: Int by Delegates.observable(0) { _, _, new ->
        notifyChange(Draco.Connection.Server.WS.Heartbeat.Rate, TransferableValue.from(new))
    }

    var heartbeatMaxMisses: Int by Delegates.observable(0) { _, _, new ->
        notifyChange(Draco.Connection.Server.WS.Heartbeat.MaxMisses, TransferableValue.from(new))
    }

    override fun toString(): String {
        return """
            Connection-Settings:
              - REST:
                - TLS         = $restUseTls
                - Host        = $restHost
                - Port        = $restPort
              - WS:
                - TLS         = $wsUseTls
                - Host        = $wsHost
                - Port        = $wsPort
                - Path        = $wsPath
                - Heartbeat:
                  - Rate      = $heartbeatRate
                  - MaxMisses = $heartbeatMaxMisses
        """.trimIndent()
    }

    class ConnectionSettingsBuilder {
        var restUseTls: Boolean = true
        var restHost: String = "draco.menkalian.de"
        var restPort: Int = 443

        var wsUseTls: Boolean = true
        var wsHost: String = "socket.draco.menkalian.de"
        var wsPort: Int = 443
        var wsPath: String = "/socket"

        var heartbeatRate: Int = 500
        var heartbeatMaxMisses: Int = 10

        fun restUseTls(restUseTls: Boolean) = apply {
            this.restUseTls = restUseTls
        }

        fun restHost(restHost: String) = apply {
            this.restHost = restHost
        }

        fun restPort(restPort: Int) = apply {
            this.restPort = restPort
        }

        fun wsUseTls(wsUseTls: Boolean) = apply {
            this.wsUseTls = wsUseTls
        }

        fun wsHost(wsHost: String) = apply {
            this.wsHost = wsHost
        }

        fun wsPort(wsPort: Int) = apply {
            this.wsPort = wsPort
        }

        fun wsPath(wsPath: String) = apply {
            this.wsPath = wsPath
        }

        fun heartbeatRate(heartbeatRate: Int) = apply {
            this.heartbeatRate = heartbeatRate
        }

        fun heartbeatMaxMisses(heartbeatMaxMisses: Int) = apply {
            this.heartbeatMaxMisses = heartbeatMaxMisses
        }

        fun build() = ConnectionSettings(this)
    }
}