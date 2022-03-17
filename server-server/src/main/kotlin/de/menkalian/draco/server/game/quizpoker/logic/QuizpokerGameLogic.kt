package de.menkalian.draco.server.game.quizpoker.logic

import de.menkalian.draco.data.Draco
import de.menkalian.draco.data.game.Player
import de.menkalian.draco.data.game.PokerGameValues
import de.menkalian.draco.data.game.PokerSettings
import de.menkalian.draco.data.game.enums.AnswerRevealStrategy
import de.menkalian.draco.data.game.enums.ConnectionState
import de.menkalian.draco.data.game.enums.GameState
import de.menkalian.draco.data.game.enums.PlayerRole
import de.menkalian.draco.data.game.enums.TimeoutStrategy
import de.menkalian.draco.data.game.values.SingleKeyValueChangeListener
import de.menkalian.draco.data.game.values.TransferableValue
import de.menkalian.draco.data.game.values.ValueChangeListener
import de.menkalian.draco.data.game.values.Values
import de.menkalian.draco.data.game.values.addStringList
import de.menkalian.draco.data.quesstimate.GuesstimateQuestion
import de.menkalian.draco.data.quesstimate.QuestionQuery
import de.menkalian.draco.server.database.guesstimate.question.IQuestionDatabase
import de.menkalian.draco.server.game.quizpoker.QuizpokerLobby
import de.menkalian.draco.server.util.logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.pow

@Suppress("MemberVisibilityCanBePrivate")
class QuizpokerGameLogic(
    private val lobby: QuizpokerLobby,
    private val questionDatabase: IQuestionDatabase
) : IQuizpokerGameLogic {
    private val sharedState: PokerGameValues
        get() = lobby.sharedData.gameStateValues
    private val internalState = LogicGameState()

    private val settings: PokerSettings
        get() = lobby.sharedData.settings

    private val logicScope = CoroutineScope(Dispatchers.Default)
    private var countDownLatch = CountDownLatch(0)

    override fun startGame() {
        logger().info("Lobby ${lobby.sharedData.uuid}: Starte Spiel")
        sharedState.state = GameState.STARTING
        internalState.roundStage = RoundStage.START
        internalState.basicPlayerOrder = lobby.sharedData.players.toList()
        refreshPlayerOrder(false)

        logicScope.launch {
            runGameLoop()
        }
    }

    override fun finishGame() {
        logger().info("Lobby ${lobby.sharedData.uuid}: Finishing game")
        refreshPlayerOrder()
        internalState.roundStage = RoundStage.RESULTS

        val highScore: Long
        val winners = internalState.basicPlayerOrder
            .sortedByDescending { it.score }
            .apply { highScore = first().score }
            .takeWhile { it.score == highScore }

        logger().debug("Lobby ${lobby.sharedData.uuid}: Winners: $winners")

        val broadcastData = mutableMapOf(
            Draco.Game.Poker.Winners.Type to TransferableValue.from("game")
        )
        broadcastData.addStringList(
            winners.map { it.name },
            Draco.Game.Poker.Winners.n
        ) {
            Draco.Game.Poker.Winners.XXX(it).Name
        }

        lobby.userHandler.broadcastMessage(broadcastData)
        lobby.returnToLobby()
    }

    override fun increaseBlinds() {
        logger().debug("Lobby ${lobby.sharedData.uuid}: Increasing blinds")
        synchronized(internalState.blindLevel) {
            internalState.blindLevel = (internalState.blindLevel + 1).coerceIn(0..1000)

            val blindValues = getBlindsForLevel(internalState.blindLevel)
            logger().trace("Lobby ${lobby.sharedData.uuid}: New Blinds: $blindValues")
            sharedState.smallBlind = blindValues.first
            sharedState.bigBlind = blindValues.second
        }
    }

    override fun decreaseBlinds() {
        logger().debug("Lobby ${lobby.sharedData.uuid}: Decreasing blinds")
        synchronized(internalState.blindLevel) {
            internalState.blindLevel = (internalState.blindLevel - 1).coerceIn(0..1000)

            val blindValues = getBlindsForLevel(internalState.blindLevel)
            logger().trace("Lobby ${lobby.sharedData.uuid}: New Blinds: $blindValues")
            sharedState.smallBlind = blindValues.first
            sharedState.bigBlind = blindValues.second
        }
    }

    private fun getBlindsForLevel(level: Int): Pair<Long, Long> {
        val clampedLevel = level.coerceIn(0..1000)

        if (clampedLevel < settings.blindLevels.size) {
            return settings.blindLevels[clampedLevel]
        } else {
            val doubleAmount = clampedLevel - settings.blindLevels.size
            val lastBlinds = settings.blindLevels.last()
            return Pair(
                (lastBlinds.first * 2.0.pow(doubleAmount.toDouble())).toLong(),
                (lastBlinds.second * 2.0.pow(doubleAmount.toDouble())).toLong(),
            )
        }
    }

    override fun processPlayerBid(player: Player, amount: Long) {
        val potToAdd = (amount - player.currentPot).coerceIn(0..player.score)

        if (amount < sharedState.currentBid && potToAdd < player.score)
            return

        logger().trace("Lobby ${lobby.sharedData.uuid}: Player $player is betting $amount points")
        player.currentPot += potToAdd
        player.score -= potToAdd

        if (player.currentPot > sharedState.currentBid) {
            logger().trace("Lobby ${lobby.sharedData.uuid}: Raising current bid to ${player.currentPot} points")
            sharedState.currentBid = player.currentPot
        }
    }

    override fun acknowledgeRaise(player: Player) {
        if (player.currentPot < sharedState.currentBid && player.score > 0)
            return

        logger().debug("Lobby ${lobby.sharedData.uuid}: Player $player acknowledged raise")
        acknowledge(player)
    }

    override fun acknowledgeCheck(player: Player) {
        if (player.currentPot < sharedState.currentBid && player.score > 0)
            return

        logger().debug("Lobby ${lobby.sharedData.uuid}: Player $player acknowledged check")
        acknowledge(player)
    }

    override fun acknowledgeFold(player: Player) {
        logger().debug("Lobby ${lobby.sharedData.uuid}: Player $player acknowledged fold")
        player.folded = true
        acknowledge(player)
        if (settings.answerRevealStrategy == AnswerRevealStrategy.ALWAYS)
            revealPlayerAnswer(player)
    }

    override fun acknowledgeWaiting(player: Player) {
        if (player.role != PlayerRole.QUIZMASTER)
            return

        logger().debug("Lobby ${lobby.sharedData.uuid}: Player $player acknowledged")
        acknowledge(player)
    }

    override fun revealPlayerAnswer(player: Player) {
        if (internalState.roundStage == RoundStage.RESULTS
            || player.folded
            || player.connectionState == ConnectionState.DISCONNECTED
        ) {
            logger().trace("Revealing answer of $player")
            player.answerRevealed = true

            val broadcastMessage = mutableMapOf(
                Draco.Game.Player.Name to TransferableValue.from(player.name),
                Draco.Player.Poker.Answer to TransferableValue.from(player.currentAnswer ?: 0L)
            )
            lobby.userHandler.broadcastMessage(broadcastMessage)
        }
    }

    override fun hasAnswerRevealed(): Boolean {
        return internalState.roundStage == RoundStage.RIVER_CARD || internalState.roundStage == RoundStage.RESULTS
    }

    private fun acknowledge(player: Player) {
        if (player != sharedState.currentPlayer)
            return

        countDownLatch.countDown()
    }

    private fun waitForAcknowledge(player: Player = sharedState.currentPlayer!!, preWaitingAction: () -> Unit) {
        val disconnectListener = DisconnectCountDownListener()

        countDownLatch = CountDownLatch(1)
        player.addListener(disconnectListener)

        preWaitingAction()

        logger().debug("Lobby ${lobby.sharedData.uuid}: Waiting for acknowledge by $player")
        countDownLatch.await(settings.timeoutMs, TimeUnit.MILLISECONDS)

        if (countDownLatch.count > 0 && player.role != PlayerRole.QUIZMASTER) {
            when (settings.timeoutStrategy) {
                TimeoutStrategy.AUTO_FOLD -> acknowledgeFold(player)
                TimeoutStrategy.AUTO_CALL -> {
                    processPlayerBid(player, sharedState.currentBid)
                    acknowledgeCheck(player)
                }
                TimeoutStrategy.KICK      -> lobby.disconnect(player)
            }
        }

        player.removeListener(disconnectListener)
    }

    internal fun runGameLoop() {
        var resume: Boolean
        do {
            if (sharedState.state == GameState.LOBBY)
                break

            logger().debug("Lobby ${lobby.sharedData.uuid}: Running stage ${internalState.roundStage}")
            resume = when (internalState.roundStage) {
                RoundStage.START      -> runStartStage()
                RoundStage.GUESSING   -> runGuessingStage()
                RoundStage.PRE_FLOP   -> runBiddingStage(hint = false, answer = false)
                RoundStage.FLOP       -> runBiddingStage(hint = true, answer = false)
                RoundStage.TURN_CARD  -> runBiddingStage(hint = true, answer = false)
                RoundStage.RIVER_CARD -> runBiddingStage(hint = false, answer = true)
                RoundStage.RESULTS    -> publishRoundResults()
            }

            if (sharedState.state == GameState.LOBBY)
                break

            if (internalState.hasQuizmaster) {
                val quizmaster = lobby.sharedData.players.first { it.role == PlayerRole.QUIZMASTER }
                waitForAcknowledge(quizmaster) {
                    sharedState.currentPlayer = quizmaster

                    lobby.userHandler.playerMessage(
                        quizmaster, mutableMapOf(
                            Draco.Message.Quizmaster.Stage.Current to TransferableValue.from(internalState.roundStage.name),
                            Draco.Message.Quizmaster.Stage.Next to TransferableValue.from(internalState.roundStage.nextStage().name)
                        )
                    )
                }
            }

            if (resume) {
                internalState.roundStage = internalState.roundStage.nextStage()
            }
        } while (resume)

        if (sharedState.state != GameState.LOBBY)
            lobby.stopGame()
    }

    internal fun runStartStage(): Boolean {
        refreshPlayerOrder()

        // reset values
        internalState.roundStage = RoundStage.START
        sharedState.state = GameState.QUESTION
        sharedState.currentBid = 0
        sharedState.round++
        val startPlayerIdx = (internalState.basicPlayerOrder.indexOf(internalState.startPlayer) + 1)
            .coerceIn(internalState.basicPlayerOrder.indices)

        internalState.startPlayer = internalState.basicPlayerOrder[startPlayerIdx]
        updateBlindsRound()

        sharedState.currentBid = sharedState.bigBlind
        internalState.basicPlayerOrder.forEachIndexed { idx, it ->
            it.folded = false
            it.answerRevealed = false
            it.currentAnswer = null
            it.role = when (idx) {
                startPlayerIdx     -> PlayerRole.SMALL_BLIND
                startPlayerIdx + 1 -> PlayerRole.BIG_BLIND
                else               -> PlayerRole.DEFAULT
            }
        }
        processPlayerBid(
            internalState.basicPlayerOrder.first { it.role == PlayerRole.SMALL_BLIND },
            lobby.sharedData.gameStateValues.smallBlind
        )
        processPlayerBid(
            internalState.basicPlayerOrder.first { it.role == PlayerRole.BIG_BLIND },
            lobby.sharedData.gameStateValues.bigBlind
        )

        logger().info("Lobby ${lobby.sharedData.uuid}: Startet new round. State: shared: $sharedState internal: $internalState")

        return internalState.basicPlayerOrder.size > 1
    }

    internal fun runGuessingStage(): Boolean {
        countDownLatch = CountDownLatch(internalState.basicPlayerOrder.size)
        internalState.basicPlayerOrder.forEach {
            it.addListener(DestructingGuessCounterListener(it))
        }

        var nextQuestion: GuesstimateQuestion
        do {
            nextQuestion = questionDatabase.queryQuestions(buildQuestionQuery()).first()
        } while (internalState.playedQuestionsInGame.contains(nextQuestion))
        logger().info("Lobby ${lobby.sharedData.uuid}: Selected question: $nextQuestion")
        internalState.playedQuestionsInGame.add(nextQuestion)

        sharedState.currentQuestion = nextQuestion

        countDownLatch.await(settings.timeoutMs, TimeUnit.MILLISECONDS)

        if (countDownLatch.count > 0) {
            internalState.basicPlayerOrder
                .filter { it.currentAnswer == null }
                .forEach {
                    it.currentAnswer = 0L
                    when (settings.timeoutStrategy) {
                        TimeoutStrategy.AUTO_FOLD -> it.folded = true
                        TimeoutStrategy.AUTO_CALL -> {}
                        TimeoutStrategy.KICK      -> lobby.disconnect(it)
                    }
                }
        }

        logger().info("Lobby ${lobby.sharedData.uuid}: Guessing completed. State: shared: $sharedState internal: $internalState")
        return internalState.basicPlayerOrder
            .any { it.folded.not() && it.connectionState != ConnectionState.DISCONNECTED }
    }

    internal fun runBiddingStage(hint: Boolean, answer: Boolean): Boolean {
        val currentQuestion = sharedState.currentQuestion!!

        if (hint) {
            val hintToShow: String
            if (sharedState.showingHints.containsAll(currentQuestion.hints)) {
                hintToShow = "N/A"
            } else {
                hintToShow = currentQuestion.hints
                    .filter { sharedState.showingHints.contains(it).not() }
                    .random()
            }
            logger().debug("Lobby ${lobby.sharedData.uuid}: Showing hint: \"$hintToShow\"")
            sharedState.showHint(hintToShow)
        }

        if (answer) {
            logger().debug("Lobby ${lobby.sharedData.uuid}: Revealing answer")
            lobby.userHandler.broadcastMessage(
                mutableMapOf(
                    Draco.Game.Poker.Question.Answer to TransferableValue.from(currentQuestion.answer)
                )
            )
        }

        var currentPlayerIdx = internalState.basicPlayerOrder.indexOf(internalState.startPlayer) - 1
        internalState.currentBidParticipants.clear()

        while (isBiddingComplete().not()) {
            currentPlayerIdx = (currentPlayerIdx + 1).mod(internalState.basicPlayerOrder.size)
            val currentPlayer = internalState.basicPlayerOrder[currentPlayerIdx]

            waitForAcknowledge(currentPlayer) {
                sharedState.currentPlayer = currentPlayer
                internalState.currentBidParticipants.add(currentPlayer)

                if (currentPlayer.score == 0L || currentPlayer.folded
                    || currentPlayer.connectionState == ConnectionState.DISCONNECTED
                ) {
                    countDownLatch.countDown()
                }
            }
        }

        logger().info("Lobby ${lobby.sharedData.uuid}: Bidding stage completed. State: shared: $sharedState internal: $internalState")
        return true
    }

    private fun isBiddingComplete(): Boolean {
        return internalState.basicPlayerOrder.all { it.isBiddingComplete() }
    }

    private fun Player.isBiddingComplete(): Boolean {
        if (connectionState == ConnectionState.DISCONNECTED)
            return true

        return internalState.currentBidParticipants.contains(this)
                && (folded || score <= 0 || currentPot == sharedState.currentBid)
    }

    internal fun publishRoundResults(): Boolean {
        fun calculateScore(player: Player) = abs(player.currentAnswer!!.toDouble() - sharedState.currentQuestion!!.answer)

        val scores: List<Pair<Double, Player>> = internalState.basicPlayerOrder
            .filter { it.folded.not() }
            .onEach { revealPlayerAnswer(it) }
            .map { calculateScore(it) to it }
            .sortedByDescending { it.first }
        val payedPlayers = payPot(scores)
        logger().info("Lobby ${lobby.sharedData.uuid}: Round completed. Payed the pot to $payedPlayers. State: shared: $sharedState internal: $internalState")

        val broadcastData = mutableMapOf(
            Draco.Game.Poker.Winners.Type to TransferableValue.from("round")
        )
        broadcastData.addStringList(
            payedPlayers.map { it.name },
            Draco.Game.Poker.Winners.n
        ) { i -> Draco.Game.Poker.Winners.XXX(i).Name }

        sharedState.state = GameState.PAUSE
        return sharedState.round < settings.maxQuestionCount && internalState.basicPlayerOrder.count { it.score > 0 } > 1
    }

    private fun payPot(scores: List<Pair<Double, Player>>): List<Player> {
        val scoresWorkingList = scores.toMutableList()
        val payedPlayers = mutableListOf<Player>()
        var currentWinners: MutableList<Player>
        do {
            if (scoresWorkingList.isEmpty())
                break
            val firstWinner = scoresWorkingList.removeFirst()
            currentWinners = mutableListOf(firstWinner.second)
            while (scoresWorkingList.isNotEmpty()
                && scoresWorkingList.first().first == firstWinner.first
            ) {
                currentWinners.add(scoresWorkingList.removeFirst().second)
            }
            payedPlayers.addAll(currentWinners)
        } while (payPot(currentWinners))

        return payedPlayers
    }

    private fun payPot(winners: List<Player>): Boolean {
        if (winners.isEmpty())
            throw UnsupportedOperationException("Can not pay out to 0 people")

        val minEntry = winners.minByOrNull { it.currentPot }?.currentPot ?: 0
        var toPayOut = 0L

        lobby.sharedData.players.forEach {
            if (it.currentPot < minEntry) {
                toPayOut += it.currentPot
                it.currentPot = 0
            } else {
                toPayOut += minEntry
                it.currentPot -= minEntry
            }
        }

        toPayOut /= winners.size
        winners.forEach {
            logger().trace("Lobby ${lobby.sharedData.uuid}: Paying $toPayOut to ${it.name}")
            it.score += toPayOut
        }

        val remainingWinners = winners.filter { it.currentPot > 0 }
        if (remainingWinners.isNotEmpty()) {
            payPot(remainingWinners)
        }

        return lobby.sharedData.players.any {
            it.currentPot > 0
        }
    }

    private fun refreshPlayerOrder(raiseBlinds: Boolean = true) {
        val oldOrder = internalState.basicPlayerOrder
        internalState.basicPlayerOrder = oldOrder
            .filter { it.role != PlayerRole.QUIZMASTER && it.score > 0 && it.connectionState != ConnectionState.DISCONNECTED }

        if (raiseBlinds)
            updateBlindsDropout(oldOrder, internalState.basicPlayerOrder)

        oldOrder
            .filter { internalState.basicPlayerOrder.contains(it).not() }
            .forEach {
                logger().debug("Lobby ${settings.lobbyName}: Player ${it.name} was dropped from the game")
            }
    }

    private fun updateBlindsDropout(old: List<Player>, new: List<Player>) {
        if (settings.blindRaiseStrategy.dropout && old.size >= new.size) {
            val amount = old.size - new.size
            for (i in 1..amount) {
                increaseBlinds()
            }
        }
    }

    private fun updateBlindsRound() {
        if (settings.blindRaiseStrategy.rounded) {
            if (internalState.dealersSinceLastBlindRaise.containsAll(internalState.basicPlayerOrder)) {
                increaseBlinds()
                internalState.dealersSinceLastBlindRaise.clear()
            }

            internalState.dealersSinceLastBlindRaise.add(internalState.startPlayer)
        }
    }

    private fun buildQuestionQuery(): QuestionQuery {
        return QuestionQuery(
            1,
            lobby.sharedData.settings.allowedLanguages.toList(),
            lobby.sharedData.settings.allowedCategories.toList(),
            lobby.sharedData.settings.allowedDifficulties.toList()
        )
    }

    private inner class DestructingGuessCounterListener(val player: Player) : ValueChangeListener {
        override fun filterKey(key: String): Boolean {
            return key == Draco.Player.Poker.Answer
        }

        override fun onValueChanged(key: String, value: TransferableValue) {
            player.removeListener(this)
            countDownLatch.countDown()
        }

        override fun onValuesChanged(values: Values) {}
    }

    private inner class DisconnectCountDownListener
        : ValueChangeListener by SingleKeyValueChangeListener(
        Draco.Player.Connection.State,
        action = { _, value ->
            val state = ConnectionState.valueOf(value.toString())
            if (state == ConnectionState.DISCONNECTED) {
                countDownLatch.countDown()
            }
        })
}