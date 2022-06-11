package de.menkalian.draco.server.socket

import de.menkalian.draco.data.Draco
import de.menkalian.draco.data.game.SocketPackage
import de.menkalian.draco.data.game.enums.PackageType
import de.menkalian.draco.data.game.event.EventBus
import de.menkalian.draco.data.game.event.events.Event
import de.menkalian.draco.data.game.values.TransferableValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class SocketSynchronizedEventBus(private val websocketHandler: IWebsocketHandler, private val sessionUuid: String) : EventBus(), ISocketPackageHandler {
    private val serializer = Json {
        ignoreUnknownKeys = true
    }

    init {
        websocketHandler.addPackageHandler(PackageType.EVENT, this)
    }

    fun deinitialize() {
        websocketHandler.removePackageHandler(PackageType.EVENT, this)
    }

    override fun onPackageReceived(sessionUuid: String, pkg: SocketPackage) {
        if (this.sessionUuid == sessionUuid) {
            val eventData = pkg.data[Draco.Event.Data]!!.toString()
            val event = serializer.decodeFromString<Event>(eventData)
            fireEventIntern(event)
        }
    }

    override fun <T : Event> fireEvent(event: T): Job {
        return eventScope.launch {
            websocketHandler.sendPackage(
                sessionUuid, SocketPackage(
                    -1, PackageType.EVENT, -1,
                    mutableMapOf(
                        Draco.Event.Data to TransferableValue.from(event.serialized(serializer))
                    )
                )
            )
        }
    }

    private fun <T : Event> fireEventIntern(event: T) {
        super.fireEvent(event)
    }
}