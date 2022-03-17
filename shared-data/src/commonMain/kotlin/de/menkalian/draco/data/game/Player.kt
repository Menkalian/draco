package de.menkalian.draco.data.game

import de.menkalian.draco.data.Draco
import de.menkalian.draco.data.game.enums.ConnectionState
import de.menkalian.draco.data.game.enums.PlayerRole
import de.menkalian.draco.data.game.values.TransferableValue
import de.menkalian.draco.data.game.values.ValueHolder

@kotlinx.serialization.Serializable
data class Player(var name: String) : ValueHolder() {
    var connectionState: ConnectionState = ConnectionState.UNKNOWN
        set(value) {
            field = value
            notifyChange(Draco.Player.Connection.State, TransferableValue.from(field.name))
        }

    var lastKnownPing: Long = -1
        set(value) {
            field = value
            notifyChange(Draco.Player.Connection.Ping, TransferableValue.from(field))
        }

    var role: PlayerRole = PlayerRole.DEFAULT
        set(value) {
            field = value
            notifyChange(Draco.Player.Poker.Role, TransferableValue.from(field.name))
        }

    var score: Long = 0
        set(value) {
            field = value
            notifyChange(Draco.Player.Poker.Score, TransferableValue.from(field))
        }

    var currentAnswer: Long? = null
    set(value) {
        field = value
        notifyChange(Draco.Player.Poker.Answer, TransferableValue.from(field ?: -1))
    }

    var answerRevealed: Boolean = false
        set(value) {
            field = value
            notifyChange(Draco.Player.Poker.Revealed, TransferableValue.from(field))
        }

    var currentPot: Long = 0
    set(value) {
        field = value
        notifyChange(Draco.Player.Poker.Pot, TransferableValue.from(field))
    }

    var folded: Boolean = false
        set(value) {
            field = value
            notifyChange(Draco.Player.Poker.Folded, TransferableValue.from(field))
        }

    override fun toString(): String {
        return "Player(name='$name', connectionState=$connectionState, lastKnownPing=$lastKnownPing, role=$role, score=$score, currentAnswer=$currentAnswer, answerRevealed=$answerRevealed, currentPot=$currentPot, folded=$folded)"
    }
}