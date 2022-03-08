package de.menkalian.draco.server.game.quizpoker.messaging

import de.menkalian.draco.data.Draco
import de.menkalian.draco.data.game.Player
import de.menkalian.draco.data.game.enums.AnswerRevealStrategy
import de.menkalian.draco.data.game.enums.BlindRaiseStrategy
import de.menkalian.draco.data.game.enums.LateJoinBehaviour
import de.menkalian.draco.data.game.enums.LobbyPublicity
import de.menkalian.draco.data.game.enums.PlayerRole
import de.menkalian.draco.data.game.enums.TimeoutStrategy
import de.menkalian.draco.data.game.values.Values
import de.menkalian.draco.data.quesstimate.Category
import de.menkalian.draco.data.quesstimate.Difficulty
import de.menkalian.draco.data.quesstimate.Language
import de.menkalian.draco.server.game.quizpoker.QuizpokerLobby

class QuizpokerMessageHandler(private val lobby: QuizpokerLobby) : IPlayerMessageListener {
    override fun onPlayerMessage(player: Player, message: Values) {
        message.forEach {
            val key = it.key
            when {
                key.startsWith(Draco.Game.Poker.Settings.toString()) -> handleSettingChange(player, key, message)
                key.startsWith(Draco.Player.toString())              -> handlePlayerData(player, key, message)
                key.startsWith(Draco.Action.Host.toString())         -> handleHostAction(player, key, message)
                key.startsWith(Draco.Action.Player.toString())       -> handlePlayerAction(player, key, message)
                key.startsWith(Draco.Action.Quizmaster.toString())   -> handleQuizmasterAction(player, key, message)
            }
        }
    }

    private fun handleSettingChange(player: Player, key: String, values: Values) {
        if (player != lobby.sharedData.host)
            return

        val value = values[key]!!
        when (key) {
            Draco.Game.Poker.Settings.Lobby.Name       -> lobby.sharedData.settings.lobbyName = value.toString()
            Draco.Game.Poker.Settings.Lobby.Publicity  -> lobby.sharedData.settings.publicity = LobbyPublicity.valueOf(value.toString())
            Draco.Game.Poker.Settings.DefaultPoints    -> lobby.sharedData.settings.defaultStartScore = value.toLong()
            Draco.Game.Poker.Settings.Timeout          -> lobby.sharedData.settings.timeoutMs = value.toLong()
            Draco.Game.Poker.Settings.MaxQuestions     -> lobby.sharedData.settings.maxQuestionCount = value.toLong()
            Draco.Game.Poker.Settings.KickBroke        -> lobby.sharedData.settings.kickWhenBroke = value.toBoolean(true)
            Draco.Game.Poker.Settings.ShowHelpWarnings -> lobby.sharedData.settings.showHelpWarnings = value.toBoolean(true)
            Draco.Game.Poker.Settings.LateJoin         -> lobby.sharedData.settings.allowLateJoin = value.toBoolean(true)
            Draco.Game.Poker.Settings.BlindStrategy    -> lobby.sharedData.settings.blindRaiseStrategy = BlindRaiseStrategy.valueOf(value.toString())
            Draco.Game.Poker.Settings.RevealStrategy   -> lobby.sharedData.settings.answerRevealStrategy = AnswerRevealStrategy.valueOf(value.toString())
            Draco.Game.Poker.Settings.TimeoutStrategy  -> lobby.sharedData.settings.timeoutStrategy = TimeoutStrategy.valueOf(value.toString())
            Draco.Game.Poker.Settings.LateJoinStrategy -> lobby.sharedData.settings.lateJoinBehaviour = LateJoinBehaviour.valueOf(value.toString())
            Draco.Game.Poker.Settings.Categories.n     -> {
                lobby.sharedData.settings.clearCategories()
                for (i in 1..value.toInt()) {
                    lobby.sharedData.settings.addCategory(
                        Category.valueOf(
                            values[Draco.Game.Poker.Settings.Categories.XXX(i).Name]?.toString() ?: Category.ART.name
                        )
                    )
                }
            }
            Draco.Game.Poker.Settings.Difficulties.n   -> {
                lobby.sharedData.settings.clearDifficulties()
                for (i in 1..value.toInt()) {
                    lobby.sharedData.settings.addDifficulty(
                        Difficulty.valueOf(
                            values[Draco.Game.Poker.Settings.Difficulties.XXX(i).Name]?.toString() ?: Difficulty.EASY.name
                        )
                    )
                }
            }
            Draco.Game.Poker.Settings.Languages.n      -> {
                lobby.sharedData.settings.clearLanguages()
                for (i in 1..value.toInt()) {
                    lobby.sharedData.settings.addLanguage(
                        Language.valueOf(
                            values[Draco.Game.Poker.Settings.Languages.XXX(i).Name]?.toString() ?: Language.ENGLISH.name
                        )
                    )
                }
            }
            Draco.Game.Poker.Settings.Blinds.n         -> {
                val blinds = mutableListOf<Pair<Long, Long>>()
                for (i in 1..value.toInt()) {
                    blinds.add(
                        (values[Draco.Game.Poker.Settings.Blinds.XXX(i).Small]?.toLong() ?: 0L)
                                to (values[Draco.Game.Poker.Settings.Blinds.XXX(i).Big]?.toLong() ?: 0L)
                    )
                }
                lobby.sharedData.settings.blindLevels = blinds
            }
        }
    }

    private fun handlePlayerData(player: Player, key: String, values: Values) {
        val value = values[key]!!

        when (key) {
            Draco.Player.Connection.Ping -> player.lastKnownPing = value.toLong()
            Draco.Player.Poker.Answer    -> player.currentAnswer = value.toLong()
            Draco.Player.Poker.Pot       -> lobby.logic.processPlayerBid(player, value.toLong())
        }
    }

    private fun handleHostAction(player: Player, key: String, values: Values) {
        if (values[key]?.toBoolean() != true)
            return
        if (player != lobby.sharedData.host)
            return

        when (key) {
            Draco.Action.Host.StartGame  -> lobby.startGame()
            Draco.Action.Host.CancelGame -> lobby.stopGame()
        }
    }

    private fun handlePlayerAction(player: Player, key: String, values: Values) {
        if (values[key]?.toBoolean() != true)
            return

        when (key) {
            Draco.Action.Player.Reveal -> lobby.logic.revealPlayerAnswer(player)
            Draco.Action.Player.Check  -> lobby.logic.acknowledgeCheck(player)
            Draco.Action.Player.Fold   -> lobby.logic.acknowledgeFold(player)
            Draco.Action.Player.Raise  -> lobby.logic.acknowledgeRaise(player)
        }
    }

    private fun handleQuizmasterAction(player: Player, key: String, values: Values) {
        if (values[key]?.toBoolean() != true)
            return
        if (player.role != PlayerRole.QUIZMASTER)
            return

        when (key) {
            Draco.Action.Quizmaster.Acknowledge -> lobby.logic.acknowledgeWaiting(player)
            Draco.Action.Quizmaster.Reveal      -> lobby.logic.revealPlayerAnswer(
                lobby.sharedData.players.first { it.name == values[Draco.Action.Quizmaster.RevealName]!!.toString() }
            )
        }
    }
}