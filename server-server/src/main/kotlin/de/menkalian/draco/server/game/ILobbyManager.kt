package de.menkalian.draco.server.game

import de.menkalian.draco.data.game.Player
import de.menkalian.draco.server.game.quizpoker.QuizpokerLobby

interface ILobbyManager {
    val serverUrl: String
    val restPort: Int
    val wsPort: Int
    val wsPath: String

    fun createPokerLobby(host: Player): QuizpokerLobby
    fun getLobby(uuid: String): QuizpokerLobby
    fun getPublicPokerLobbies(): List<QuizpokerLobby>

    fun createToken(uuid: String): String
    fun resolveToken(token: String): String
}