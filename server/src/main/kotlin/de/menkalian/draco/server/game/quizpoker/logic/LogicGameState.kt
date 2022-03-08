package de.menkalian.draco.server.game.quizpoker.logic

import de.menkalian.draco.data.game.Player
import de.menkalian.draco.data.quesstimate.GuesstimateQuestion

internal data class LogicGameState(
    // Game counters / trackers
    val playedQuestionsInGame: MutableList<GuesstimateQuestion> = mutableListOf(),
    val dealersSinceLastBlindRaise: MutableList<Player> = mutableListOf(),
    var blindLevel: Int = 0,
    var basicPlayerOrder: List<Player> = listOf(),

    // Cached settings/behaviour switches
    var hasQuizmaster: Boolean = false,

    // Round counters/trackers
    var roundStage: RoundStage = RoundStage.START,
    var startPlayer: Player = Player("N/A"),

    // Bidding counters/trackers
    val currentBidParticipants: MutableSet<Player> = mutableSetOf()
)
