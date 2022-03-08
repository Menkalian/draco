package de.menkalian.draco.server.rest.game

import de.menkalian.draco.data.game.Player
import de.menkalian.draco.data.game.PokerLobby
import de.menkalian.draco.server.game.ILobbyManager
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
        val lobby = lobbyManager.createPokerLobby(player)
        return lobby.sharedData
    }

    @PostMapping("/quizpoker/lobby/{uuid}")
    fun joinLobby(@RequestBody player: Player, @PathVariable("uuid") lobbyUuid: String): PokerLobby {
        val lobby = lobbyManager.getLobby(lobbyUuid)
        lobby.connect(player)
        return lobby.sharedData
    }

    @DeleteMapping("/quizpoker/lobby/{uuid}")
    fun disconnectLobby(@RequestBody player: Player, @PathVariable("uuid") lobbyUuid: String): PokerLobby {
        val lobby = lobbyManager.getLobby(lobbyUuid)
        lobby.disconnect(player)
        return lobby.sharedData
    }

    @GetMapping("/quizpoker/lobby/{uuid}")
    fun getLobbyStatus(@PathVariable("uuid") lobbyUuid: String): PokerLobby {
        return lobbyManager.getLobby(lobbyUuid).getFilteredData()
    }

    @GetMapping("/quizpoker/lobby/token/{token}")
    fun resolveToken(@PathVariable("token") token: String): String {
        return lobbyManager.resolveToken(token)
    }
}