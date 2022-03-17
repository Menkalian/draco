package de.menkalian.draco.data.game

import de.menkalian.draco.data.Draco
import de.menkalian.draco.data.game.enums.GameState
import de.menkalian.draco.data.game.values.TransferableValue
import de.menkalian.draco.data.game.values.ValueHolder
import de.menkalian.draco.data.quesstimate.GuesstimateQuestion

@kotlinx.serialization.Serializable
class PokerGameValues constructor() : ValueHolder() {
    var state: GameState = GameState.LOBBY
        set(value) {
            field = value
            notifyChange(Draco.Game.Poker.State, TransferableValue.from(value.name))
        }

    var currentPlayer: Player? = null
        set(value) {
            field = value
            notifyChange(Draco.Game.Poker.CurrentPlayer, TransferableValue.from(value?.name ?: ""))
        }

    var currentBid: Long = 0L
        set(value) {
            field = value
            notifyChange(Draco.Game.Poker.CurrentBid, TransferableValue.from(value))
        }

    var round: Int = 0
        set(value) {
            field = value
            notifyChange(Draco.Game.Poker.Round, TransferableValue.from(value))
        }

    var smallBlind: Long = 0
        set(value) {
            field = value
            notifyChange(Draco.Game.Poker.Blinds.Small, TransferableValue.from(value))
        }

    var bigBlind: Long = 0
        set(value) {
            field = value
            notifyChange(Draco.Game.Poker.Blinds.Big, TransferableValue.from(value))
        }

    var currentQuestion: GuesstimateQuestion? = null
        set(value) {
            field = value
            notifyChanges(
                mutableMapOf(
                    Draco.Game.Poker.Question.UUID to TransferableValue.from(value?.id ?: -1),
                    Draco.Game.Poker.Question.Text to TransferableValue.from(value?.question ?: ""),
                    Draco.Game.Poker.Question.Answer to TransferableValue.from(value?.answer ?: -1.0),
                )
            )
        }

    private val revealedHints = mutableListOf<String>()
    val showingHints = revealedHints.toList()

    fun showHint(hint: String) {
        revealedHints.add(hint)
        notifyHints()
    }

    fun clearHints() {
        revealedHints.clear()
        notifyHints()
    }

    private fun notifyHints() {
        val values = mutableMapOf<String, TransferableValue>()
        values[Draco.Game.Poker.Question.Hint.n] = TransferableValue.from(revealedHints.size)
        revealedHints.forEachIndexed { idx, hint ->
            values[Draco.Game.Poker.Question.Hint.XXX(idx + 1).Text] = TransferableValue.from(hint)
        }
        notifyChanges(values)
    }

    private constructor(
        state: GameState,
        currentPlayer: Player?,
        currentBid: Long,
        round: Int,
        smallBlind: Long,
        bigBlind: Long,
        currentQuestion: GuesstimateQuestion?
    ) : this() {
        this.state = state
        this.currentPlayer = currentPlayer
        this.currentBid = currentBid
        this.round = round
        this.smallBlind = smallBlind
        this.bigBlind = bigBlind
        this.currentQuestion = currentQuestion
    }

    fun copy(): PokerGameValues {
        return PokerGameValues(
            state, currentPlayer, currentBid, round, smallBlind, bigBlind, currentQuestion
        )
    }

    override fun toString(): String {
        return "PokerGameValues(state=$state, currentPlayer=$currentPlayer, currentBid=$currentBid, round=$round, smallBlind=$smallBlind, bigBlind=$bigBlind, currentQuestion=$currentQuestion, revealedHints=$revealedHints, showingHints=$showingHints)"
    }
}