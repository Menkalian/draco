package de.menkalian.draco.server.socket

import de.menkalian.draco.data.game.SocketPackage
import de.menkalian.draco.data.game.enums.PackageType
import de.menkalian.draco.server.util.logger
import io.ktor.application.install
import io.ktor.http.cio.websocket.FrameType
import io.ktor.http.cio.websocket.readBytes
import io.ktor.http.cio.websocket.send
import io.ktor.routing.Routing
import io.ktor.routing.routing
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

@Service
class KtorWebsocketHandler(@Value("\${draco.internal.socket.port}") port: Int, @Value("\${draco.internal.socket.path}") path: String) : IWebsocketHandler {
    companion object {
        val msgIdCounter = AtomicLong(0L)

        val sendScope = CoroutineScope(Dispatchers.IO)
        val eventScope = CoroutineScope(Dispatchers.Default)
        val timeoutScope = CoroutineScope(Dispatchers.Default)
    }

    private final val websocketServer: ApplicationEngine
    private final val serializer: Json

    private final val activeSessions: MutableMap<String, DefaultWebSocketServerSession> = mutableMapOf()

    private final val packageHandlers: Map<PackageType, MutableList<ISocketPackageHandler>> = PackageType.values().associateWith { mutableListOf() }
    private final val acknowledgeHandlers: Map<PackageType, AcknowledgeHandler> = PackageType.values().associateWith { AcknowledgeHandler() }

    init {
        logger().info("Starting WebSocket at port $port with path \"$path\"")
        serializer = Json {
            ignoreUnknownKeys = true
        }

        acknowledgeHandlers.forEach { addPackageHandler(it.key, it.value) }

        websocketServer = embeddedServer(CIO, port = port) {
            install(WebSockets)
            routing {
                addWebsocketRoute(path)
            }
        }.start()
    }

    private fun Routing.addWebsocketRoute(path: String) {
        webSocket(path) {
            val uuid: String

            synchronized(activeSessions) {
                uuid = getNewUniqueSessionUuid()
                activeSessions[uuid] = this
            }

            logger().debug("New Websocket connection $uuid established")

            for (frame in incoming) {
                try {
                    if (frame.frameType == FrameType.CLOSE) {
                        logger().debug("Received closing frame for connection $uuid")
                        break
                    }

                    val pkg = serializer.decodeFromString<SocketPackage>(frame.readBytes().decodeToString())
                    acknowledge(pkg)
                    fireHandlers(uuid, pkg)
                } catch (ex: Exception) {
                    logger().error("Error occured while processing session $uuid. Frame: ${frame.data.toList()}", ex)
                }
            }

            logger().debug("Websocket connection $uuid terminated")
            synchronized(activeSessions) {
                activeSessions.remove(uuid)
            }
        }
    }

    private fun getNewUniqueSessionUuid(): String {
        synchronized(activeSessions) {
            var uuid: String
            do {
                uuid = UUID.randomUUID().toString()
            } while (activeSessions.containsKey(uuid))

            return uuid
        }
    }

    private suspend fun DefaultWebSocketServerSession.acknowledge(pkg: SocketPackage) {
        val acknowledgeType = pkg.type.getAcknowledge()
        logger().debug("Sending acknowledgement with type $acknowledgeType for package ${pkg.id}")
        if (acknowledgeType != null) {
            val acknowledge = SocketPackage(pkg.id, acknowledgeType, System.currentTimeMillis(), mutableMapOf())
            send(serializer.encodeToString(acknowledge))
        }
    }

    private fun fireHandlers(uuid: String, pkg: SocketPackage) {
        logger().trace("Firing handlers for UUID $uuid and package $pkg")
        packageHandlers[pkg.type]?.forEach {
            eventScope.launch {
                it.onPackageReceived(uuid, pkg)
            }
        }
    }

    override fun addPackageHandler(type: PackageType, handler: ISocketPackageHandler) {
        logger().debug("Registering package handler $handler")
        synchronized(packageHandlers) {
            packageHandlers[type]!!.add(handler)
        }
    }

    override fun removePackageHandler(type: PackageType, handler: ISocketPackageHandler) {
        logger().debug("Unregistering package handler $handler")
        synchronized(packageHandlers) {
            packageHandlers[type]!!.remove(handler)
        }
    }

    override fun sendPackage(
        targetUuid: String,
        pkg: SocketPackage,
        acknowledgeTimeoutMs: Long,
        onSent: (success: Boolean) -> Unit,
        onAcknowledge: () -> Unit,
        onAcknowledgeFailed: () -> Unit
    ) {
        sendScope.launch {
            val modPackage = pkg.copy(id = msgIdCounter.incrementAndGet(), timestamp = System.currentTimeMillis())
            val acknowledgeType = modPackage.type.getAcknowledge()

            logger().debug("Sending package $modPackage with{} acknowledgement", if (acknowledgeType == null) "out" else "")

            if (acknowledgeType != null) {
                val timeoutJob = timeoutScope.launch {
                    delay(acknowledgeTimeoutMs)
                    if (!acknowledgeHandlers[acknowledgeType]!!.cancelAcknowledgement(modPackage.id)) {
                        onAcknowledgeFailed()
                    }
                }

                acknowledgeHandlers[acknowledgeType]!!.requestAcknowledgement(
                    AcknowledgeRequest(modPackage.id, acknowledgeType, timeoutJob, onAcknowledge)
                )
            }

            try {
                activeSessions[targetUuid]?.send(serializer.encodeToString(modPackage).encodeToByteArray())
                onSent(true)

                if (acknowledgeType == null) {
                    onAcknowledge()
                }
            } catch (ex: Exception) {
                onSent(false)
            }
        }
    }
}