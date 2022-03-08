package de.menkalian.draco.server.game.quizpoker.logic

import de.menkalian.draco.data.game.Player

interface IQuizpokerGameLogic {
    fun startGame()
    fun finishGame()

    fun increaseBlinds()
    fun decreaseBlinds()

    fun processPlayerBid(player: Player, amount: Long)

    fun acknowledgeRaise(player: Player)
    fun acknowledgeCheck(player: Player)
    fun acknowledgeFold(player: Player)
    fun acknowledgeWaiting(player: Player)

    fun revealPlayerAnswer(player: Player)

    fun hasAnswerRevealed(): Boolean
}