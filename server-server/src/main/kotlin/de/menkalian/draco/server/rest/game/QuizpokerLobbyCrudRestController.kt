package de.menkalian.draco.server.rest.game

import de.menkalian.draco.data.game.Player
import de.menkalian.draco.data.game.PokerLobby
import de.menkalian.draco.server.game.ILobbyManager
import de.menkalian.draco.server.util.logger
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class QuizpokerLobbyCrudRestController(val lobbyManager: ILobbyManager) {
    @PutMapping("/quizpoker/lobby")
    fun createLobby(@RequestBody player: Player): PokerLobby {
        logger().info("Player $player creates a new lobby")
        val lobby = lobbyManager.createPokerLobby(player)
        return lobby.sharedData
    }

    @PostMapping("/quizpoker/lobby/{uuid}")
    fun joinLobby(@RequestBody player: Player, @PathVariable("uuid") lobbyUuid: String): PokerLobby {
        logger().info("Player $player joins lobby $lobbyUuid")
        val lobby = lobbyManager.getLobby(lobbyUuid)
        lobby.connect(player)
        return lobby.sharedData
    }

    @DeleteMapping("/quizpoker/lobby/{uuid}")
    fun disconnectLobby(@RequestBody player: Player, @PathVariable("uuid") lobbyUuid: String): PokerLobby {
        logger().info("Player $player disconnects from lobby $lobbyUuid")
        val lobby = lobbyManager.getLobby(lobbyUuid)
        lobby.disconnect(player)
        return lobby.sharedData
    }

    @GetMapping("/quizpoker/lobby/{uuid}")
    fun getLobbyStatus(@PathVariable("uuid") lobbyUuid: String): PokerLobby {
        logger().debug("Status of lobby $lobbyUuid read")
        return lobbyManager.getLobby(lobbyUuid).getFilteredData()
    }

    @GetMapping("/quizpoker/lobby/public/all")
    fun getPublicLobbies() : List<PokerLobby> {
        logger().trace("Public lobbies are queried")
        return lobbyManager.getPublicPokerLobbies().map { it.getFilteredData() }
    }

    @GetMapping("/quizpoker/lobby/token/{token}")
    fun resolveToken(@PathVariable("token") token: String): String {
        logger().debug("Resolving token $token")
        return lobbyManager.resolveToken(token)
    }
}