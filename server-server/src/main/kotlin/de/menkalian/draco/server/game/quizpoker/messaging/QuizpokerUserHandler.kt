package de.menkalian.draco.server.game.quizpoker.messaging

import de.menkalian.draco.data.Draco
import de.menkalian.draco.data.game.Player
import de.menkalian.draco.data.game.SocketPackage
import de.menkalian.draco.data.game.enums.ConnectionState
import de.menkalian.draco.data.game.enums.PackageType
import de.menkalian.draco.data.game.values.TransferableValue
import de.menkalian.draco.data.game.values.Values
import de.menkalian.draco.server.game.quizpoker.QuizpokerLobby
import de.menkalian.draco.server.socket.ISocketPackageHandler
import de.menkalian.draco.server.socket.IWebsocketHandler
import de.menkalian.draco.server.util.logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QuizpokerUserHandler(private val lobby: QuizpokerLobby, private val websocketHandler: IWebsocketHandler) {
    private val playersByUuid = mutableMapOf<String, Player>()
    private val uuidsByPlayer = mutableMapOf<Player, String>()
    private val sessionMutex = Any()

    private val messageListeners = mutableListOf<IPlayerMessageListener>()

    private val eventScope = CoroutineScope(Dispatchers.Default)

    init {
        websocketHandler.addPackageHandler(PackageType.HEARTBEAT, HeartbeatHandler())
        websocketHandler.addPackageHandler(PackageType.CLIENT_HELLO, ClientHelloHandler())
        websocketHandler.addPackageHandler(PackageType.CLIENT_MSG, ClientMessageHandler())
    }

    private inner class HeartbeatHandler : ISocketPackageHandler {
        override fun onPackageReceived(sessionUuid: String, pkg: SocketPackage) {
            // TODO
        }
    }

    private inner class ClientHelloHandler : ISocketPackageHandler {
        override fun onPackageReceived(sessionUuid: String, pkg: SocketPackage) {
            val lobbyId = pkg.data[Draco.Game.Lobby.Id]?.toString()
            if (lobbyId != lobby.sharedData.uuid)
                return

            val playerName = pkg.data[Draco.Game.Player.Name]?.toString()
            logger().debug("Lobby ${lobby.sharedData.uuid}: Received ClientHello-Message by Session $sessionUuid for $playerName")
            val player = lobby.sharedData.players.firstOrNull { it.name == playerName }
            if (player != null) {
                synchronized(sessionMutex) {
                    playersByUuid[sessionUuid] = player
                    uuidsByPlayer[player] = sessionUuid
                }
                player.connectionState = ConnectionState.CONNECTED
            }
        }
    }

    private inner class ClientMessageHandler : ISocketPackageHandler {
        override fun onPackageReceived(sessionUuid: String, pkg: SocketPackage) {
            val player = playersByUuid[sessionUuid]
            if (player != null) {
                logger().trace("Received package from $player: $pkg")
                fireMessageListeners(player, pkg)
            }
        }
    }

    fun addMessageListener(listener: IPlayerMessageListener) {
        synchronized(messageListeners) {
            logger().debug("Registered $listener")
            messageListeners.add(listener)
        }
    }

    @Suppress("unused")
    fun removeMessageListener(listener: IPlayerMessageListener) {
        synchronized(messageListeners) {
            logger().debug("Unregistered $listener")
            messageListeners.remove(listener)
        }
    }

    fun broadcastMessage(values: Values) {
        val pkg = SocketPackage(0, PackageType.SERVER_BROADCAST, 0, values)
        logger().debug("Broadcasting $pkg to all players")
        synchronized(sessionMutex) {
            uuidsByPlayer.values.forEach {
                websocketHandler.sendPackage(it, pkg)
            }
        }
    }

    fun playerMessage(player: Player, values: Values) {
        val pkg = SocketPackage(0, PackageType.SERVER_MSG, 0, values)
        logger().debug("Sending $pkg to $player")
        websocketHandler.sendPackage(uuidsByPlayer[player]!!, pkg)
    }

    private fun fireMessageListeners(player: Player, pkg: SocketPackage) {
        // Copy data
        val message = pkg.data.toMutableMap()

        // Put additional data
        message[Draco.Message.Id] = TransferableValue.from(pkg.id)
        message[Draco.Message.Type] = TransferableValue.from(pkg.type.name)
        message[Draco.Message.Timestamp] = TransferableValue.from(pkg.timestamp)

        synchronized(messageListeners) {
            messageListeners.forEach {
                eventScope.launch {
                    it.onPlayerMessage(player, message)
                }
            }
        }
    }

    fun removePlayer(playerObj: Player) {
        synchronized(sessionMutex) {
            val sessionUuid = uuidsByPlayer[playerObj]
            uuidsByPlayer.remove(playerObj)
            playersByUuid.remove(sessionUuid)
        }
    }
}