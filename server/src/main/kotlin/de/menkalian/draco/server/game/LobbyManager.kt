package de.menkalian.draco.server.game

import de.menkalian.draco.data.game.Player
import de.menkalian.draco.data.game.enums.ConnectionState
import de.menkalian.draco.data.game.enums.LobbyPublicity
import de.menkalian.draco.server.database.guesstimate.question.IQuestionDatabase
import de.menkalian.draco.server.game.quizpoker.QuizpokerLobby
import de.menkalian.draco.server.socket.IWebsocketHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class LobbyManager(
    @Value("\${draco.host.address}") override val serverUrl: String,
    @Value("\${server.port}") override val restPort: Int,
    @Value("\${draco.socket.port}") override val wsPort: Int,
    @Value("\${draco.socket.path}") override val wsPath: String,
    private val websocketHandler: IWebsocketHandler,
    private val questionDatabase: IQuestionDatabase,
) : ILobbyManager {
    private val mutex = Any()

    private val lobbies: MutableMap<String, QuizpokerLobby> = mutableMapOf()
    private val tokens: MutableMap<String, String> = mutableMapOf()

    private val cleanupScope = CoroutineScope(Dispatchers.Default)

    init {
        cleanupScope.launch {
            while (true) {
                synchronized(mutex) {
                    cleanupLobbies()
                    cleanupTokens()
                }
                delay(1 * 60 * 60 * 1000) // Wait 1h
            }
        }
    }

    override fun createPokerLobby(host: Player): QuizpokerLobby {
        synchronized(mutex) {
            val uuid = generateUuid()
            val lobby = QuizpokerLobby(this, websocketHandler, questionDatabase, uuid, host)
            lobbies[uuid] = lobby
            return lobby
        }
    }

    override fun getLobby(uuid: String): QuizpokerLobby {
        synchronized(mutex) {
            return lobbies[uuid]!!
        }
    }

    override fun getPublicPokerLobbies(): List<QuizpokerLobby> {
        synchronized(mutex) {
            return lobbies.values
                .filter { it.sharedData.settings.publicity == LobbyPublicity.PUBLIC }
        }
    }

    override fun createToken(uuid: String): String {
        synchronized(mutex) {
            val token = generateToken()
            tokens[token] = uuid
            return token
        }
    }

    override fun resolveToken(token: String): String {
        synchronized(mutex) {
            return tokens[token]!!
        }
    }

    private fun generateUuid(): String {
        var uuid: String
        do {
            uuid = UUID.randomUUID().toString()
        } while (lobbies.containsKey(uuid))
        return uuid
    }

    private fun generateToken(): String {
        val availableChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray()
        var token: String
        do {
            token = (1..6)
                .map { availableChars.random() }
                .joinToString("")
        } while (lobbies.containsKey(token))
        return token
    }

    private fun cleanupLobbies() {
        val oldLobbies = lobbies.toMutableMap()
        lobbies.clear()
        lobbies.putAll(
            oldLobbies.filterValues {
                it.sharedData.players.any { p ->
                    p.connectionState != ConnectionState.UNKNOWN
                            && p.connectionState != ConnectionState.CONNECTION_LOST
                            && p.connectionState != ConnectionState.DISCONNECTED
                }
            }
        )
    }

    private fun cleanupTokens() {
        val oldTokens = tokens.toMutableMap()
        tokens.clear()
        tokens.putAll(
            oldTokens.filterValues { lobbies.containsKey(it) }
        )
    }
}