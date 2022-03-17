package de.menkalian.draco.server.game.quizpoker.messaging

import de.menkalian.draco.data.game.Player
import de.menkalian.draco.data.game.values.Values

fun interface IPlayerMessageListener {
    fun onPlayerMessage(player: Player, message: Values)
}