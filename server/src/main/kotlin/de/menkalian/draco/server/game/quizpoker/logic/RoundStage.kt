package de.menkalian.draco.server.game.quizpoker.logic

enum class RoundStage(private val nextStageName: String) {
    START("GUESSING"),
    GUESSING("PRE_FLOP"),
    PRE_FLOP("FLOP"),
    FLOP("TURN_CARD"),
    TURN_CARD("RIVER_CARD"),
    RIVER_CARD("RESULTS"),
    RESULTS("START");

    fun nextStage(): RoundStage = valueOf(nextStageName)
}