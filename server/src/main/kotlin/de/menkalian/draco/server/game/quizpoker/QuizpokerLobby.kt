package de.menkalian.draco.server.game.quizpoker

import de.menkalian.draco.data.Draco
import de.menkalian.draco.data.game.ConnectionSettings
import de.menkalian.draco.data.game.Player
import de.menkalian.draco.data.game.PokerGameValues
import de.menkalian.draco.data.game.PokerLobby
import de.menkalian.draco.data.game.PokerSettings
import de.menkalian.draco.data.game.enums.AnswerRevealStrategy
import de.menkalian.draco.data.game.enums.BlindRaiseStrategy
import de.menkalian.draco.data.game.enums.ConnectionState
import de.menkalian.draco.data.game.enums.GameState
import de.menkalian.draco.data.game.enums.LateJoinBehaviour
import de.menkalian.draco.data.game.enums.LobbyPublicity
import de.menkalian.draco.data.game.enums.TimeoutStrategy
import de.menkalian.draco.data.game.values.RegexValueMultiChangeListener
import de.menkalian.draco.data.game.values.TransferableValue
import de.menkalian.draco.data.game.values.ValueChangeListener
import de.menkalian.draco.data.game.values.Values
import de.menkalian.draco.data.quesstimate.Category
import de.menkalian.draco.data.quesstimate.Difficulty
import de.menkalian.draco.data.quesstimate.Language
import de.menkalian.draco.server.database.guesstimate.question.IQuestionDatabase
import de.menkalian.draco.server.game.ILobbyManager
import de.menkalian.draco.server.game.quizpoker.logic.IQuizpokerGameLogic
import de.menkalian.draco.server.game.quizpoker.logic.QuizpokerGameLogic
import de.menkalian.draco.server.game.quizpoker.messaging.QuizpokerMessageHandler
import de.menkalian.draco.server.game.quizpoker.messaging.QuizpokerUserHandler
import de.menkalian.draco.server.socket.IWebsocketHandler
import de.menkalian.draco.server.util.DuplicateUserNameException

class QuizpokerLobby(
    private val manager: ILobbyManager,
    websocketHandler: IWebsocketHandler,
    questionDatabase: IQuestionDatabase,
    uuid: String, host: Player
) {
    val sharedData = PokerLobby(
        uuid,
        manager.createToken(uuid),
        host,
        mutableListOf(),
        PokerSettings(),
        ConnectionSettings(),
        PokerGameValues()
    )

    internal val userHandler = QuizpokerUserHandler(this, websocketHandler)
    internal val logic: IQuizpokerGameLogic = QuizpokerGameLogic(this, questionDatabase)
    internal val messageHandler = QuizpokerMessageHandler(this)

    private val playerChangeListeners = mutableMapOf<Player, PlayerValueChangeListener>()
    private val valueChangeListener = RegexValueMultiChangeListener(".+".toRegex()) {
        filterSensitiveGameData(it)
        userHandler.broadcastMessage(it)
    }

    init {
        configureDefaults()
        sharedData.settings.addListener(valueChangeListener)
        sharedData.connectionValues.addListener(valueChangeListener)
        sharedData.gameStateValues.addListener(valueChangeListener)
        userHandler.addMessageListener(messageHandler)
        connect(host)
    }

    fun connect(player: Player) {
        synchronized(sharedData) {
            if (sharedData.players.any { it.name == player.name }) {
                throw DuplicateUserNameException()
            }

            val changeListener = PlayerValueChangeListener(player)
            playerChangeListeners[player] = changeListener
            player.addListener(changeListener)

            sharedData.players.add(player)
            player.connectionState = ConnectionState.CONNECTING
            player.lastKnownPing = -1
        }
    }

    fun disconnect(player: Player) {
        synchronized(sharedData) {
            val playerObj = sharedData.players.first { player.name == it.name }
            playerObj.connectionState = ConnectionState.DISCONNECTED
            playerObj.lastKnownPing = -1
            userHandler.removePlayer(playerObj)
        }
    }

    fun startGame() {
        logic.startGame()
    }

    fun stopGame() {
        logic.finishGame()
    }

    internal fun returnToLobby() {
        resetGameState()
    }

    private fun configureDefaults() {
        sharedData.connectionValues.apply {
            serverIp = manager.serverUrl
            restPort = manager.restPort
            wsPort = manager.wsPort
            wsPath = manager.wsPath

            heartbeatRate = 100
            heartbeatMaxMisses = 50
        }

        sharedData.settings.apply {
            lobbyName = "Quizpoker Lobby #${sharedData.uuid}"
            publicity = LobbyPublicity.CODE_ONLY

            defaultStartScore = 5000
            maxQuestionCount = Long.MAX_VALUE

            kickWhenBroke = false
            showHelpWarnings = false

            blindLevels = listOf(
                50L to 100L,
                100L to 200L,
                200L to 400L,
                500L to 1000L
            )
            blindRaiseStrategy = BlindRaiseStrategy.ON_ROUNDED
            answerRevealStrategy = AnswerRevealStrategy.NEVER

            timeoutMs = 60_000
            timeoutStrategy = TimeoutStrategy.AUTO_FOLD

            allowLateJoin = false
            lateJoinBehaviour = LateJoinBehaviour.MEDIAN_SCORE

            Category.values().forEach { addCategory(it) }
            Difficulty.values().forEach { addDifficulty(it) }
            addLanguage(Language.ENGLISH)
        }

        resetGameState()
    }

    private fun resetGameState() {
        sharedData.gameStateValues.apply {
            state = GameState.LOBBY
            round = 0
            smallBlind = sharedData.settings.blindLevels.first().first
            bigBlind = sharedData.settings.blindLevels.first().second
            currentQuestion = null
            currentPlayer = null
            currentBid = 0
            clearHints()
        }

        sharedData.players.forEach {
            it.apply {
                score = sharedData.settings.defaultStartScore
                currentAnswer = null
                answerRevealed = false
                currentPot = 0
                folded = false
            }
        }
    }

    private fun filterSensitiveGameData(values: Values) {
        if (!logic.hasAnswerRevealed()) {
            values.remove(Draco.Game.Poker.Question.Answer)
        }
    }

    fun getFilteredData(): PokerLobby {
        val data = sharedData.copy()

        if (!logic.hasAnswerRevealed()) {
            data.gameStateValues = sharedData.gameStateValues.copy()
            data.gameStateValues.currentQuestion = data.gameStateValues.currentQuestion?.copy(answer = Double.NaN)
        }

        val players = data.players
            .map {
                val toReturn = it.copy()
                if (!it.answerRevealed) {
                    toReturn.currentAnswer = null
                }
                toReturn
            }
            .toMutableList()
        data.players.clear()
        data.players.addAll(players)

        return data
    }

    private inner class PlayerValueChangeListener(val player: Player) : ValueChangeListener {
        override fun filterKey(key: String) = true

        override fun onValueChanged(key: String, value: TransferableValue) {}

        override fun onValuesChanged(values: Values) {
            val message = values.toMutableMap()
            message[Draco.Game.Player.Name] = TransferableValue.from(player.name)
            filterSensitivePlayerData(message)
            userHandler.broadcastMessage(message)
        }

        private fun filterSensitivePlayerData(values: Values) {
            if (!player.answerRevealed) {
                values.remove(Draco.Player.Poker.Answer)
            }
        }
    }
}