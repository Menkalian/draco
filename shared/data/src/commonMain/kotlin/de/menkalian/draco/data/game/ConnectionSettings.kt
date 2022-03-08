package de.menkalian.draco.data.game

import de.menkalian.draco.data.Draco
import de.menkalian.draco.data.game.values.TransferableValue
import de.menkalian.draco.data.game.values.ValueHolder

@kotlinx.serialization.Serializable
class ConnectionSettings : ValueHolder() {
    var serverIp: String = "draco.menkalian.de"
        set(ip) {
            field = ip
            notifyChange(Draco.Connection.Server.IP, TransferableValue.from(ip))
        }

    var restPort: Int = 8080
        set(port) {
            field = port
            notifyChange(Draco.Connection.Server.REST.Port, TransferableValue.from(port))
        }

    var wsPort: Int = 8081
        set(port) {
            field = port
            notifyChange(Draco.Connection.Server.WS.Port, TransferableValue.from(port))
        }

    var wsPath: String = "/socket/poker"
        set(path) {
            field = path
            notifyChange(Draco.Connection.Server.WS.Path, TransferableValue.from(path))
        }

    var heartbeatRate: Int = 100
        set(rate) {
            field= rate
            notifyChange(Draco.Connection.Server.WS.Heartbeat.Rate, TransferableValue.from(rate))
        }

    var heartbeatMaxMisses: Int = 50
        set(value) {
            field = value
            notifyChange(Draco.Connection.Server.WS.Heartbeat.MaxMisses, TransferableValue.from(value))
        }

}