package de.menkalian.draco.restclient

import de.menkalian.draco.data.Draco
import de.menkalian.draco.data.game.values.Values
import de.menkalian.draco.data.quesstimate.GuesstimateQuestion
import de.menkalian.draco.data.quesstimate.Language
import de.menkalian.draco.restclient.config.DracoClientConfiguration
import de.menkalian.draco.restclient.logger.Slf4jLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {
    val client = DracoClientFactory.createClient(
        DracoClientConfiguration.DracoClientConfigurationBuilder()
            .useHttps(false)
            .serverUrl("localhost")
            .serverPort(8080)
            .authToken(args.getOrElse(0) { "YOUR_TOKEN_HERE" })
            .logger(Slf4jLogger())
            .build()
    )

    ErrorHandler.latch = CountDownLatch(1)
    client.lobby.create("Player1", ErrorHandler::onSuccess, ErrorHandler::onDracoError)
    ErrorHandler.latch?.await(10, TimeUnit.SECONDS)

    val gameClientP1 = ErrorHandler.latest as PokerGameClient

    ErrorHandler.latch = CountDownLatch(1)
    client.lobby.connect("Player2", gameClientP1.lobby.uuid, ErrorHandler::onSuccess, ErrorHandler::onDracoError)
    ErrorHandler.latch?.await(10, TimeUnit.SECONDS)
    val gameClientP2 = ErrorHandler.latest as PokerGameClient

    ErrorHandler.latch = CountDownLatch(1)
    client.lobby.connectWithToken("Player3", gameClientP1.lobby.accessToken, ErrorHandler::onSuccess, ErrorHandler::onDracoError)
    ErrorHandler.latch?.await(10, TimeUnit.SECONDS)
    val gameClientP3 = ErrorHandler.latest as PokerGameClient

    ErrorHandler.latch = CountDownLatch(1)
    client.lobby.connect("Player4", gameClientP1.lobby.uuid, ErrorHandler::onSuccess, ErrorHandler::onDracoError)
    ErrorHandler.latch?.await(10, TimeUnit.SECONDS)
    val gameClientP4 = ErrorHandler.latest as PokerGameClient

    runBlocking {
        delay(500)
    }

    ErrorHandler.latch = CountDownLatch(1)
    gameClientP1.clientListener = object : PokerGameClient.ClientEventListener {
        override fun onServerMessage(data: Values) {
            if (data.containsKey(Draco.Game.Poker.Settings.Languages.n))
                ErrorHandler.latch?.countDown()
        }
    }
    gameClientP1.lobby.settings.allowedLanguages += Language.GERMAN
    ErrorHandler.latch?.await(10, TimeUnit.SECONDS)

    runBlocking {
        delay(500)
    }

    // Start game
    ErrorHandler.latch = CountDownLatch(1)
    gameClientP1.clientListener = object : PokerGameClient.ClientEventListener {
        override fun onQuestion(question: GuesstimateQuestion) {
            ErrorHandler.latch?.countDown()
        }
    }
    gameClientP1.performAction(PokerGameClient.ClientAction.START)
    ErrorHandler.latch?.await(10, TimeUnit.SECONDS)

    ErrorHandler.latch = CountDownLatch(1)
    gameClientP1.clientListener = object : PokerGameClient.ClientEventListener {
        override fun onOwnPlayerTurn() {
            ErrorHandler.latch?.countDown()
        }
    }

    // Give guesses
    gameClientP1.player.currentAnswer = 30L
    gameClientP2.player.currentAnswer = 35L
    gameClientP3.player.currentAnswer = 25L
    gameClientP4.player.currentAnswer = 30L

    ErrorHandler.latch?.await(10, TimeUnit.SECONDS)

    ErrorHandler.latch = CountDownLatch(1)
    gameClientP1.clientListener = object : PokerGameClient.ClientEventListener {
        override fun onPlayerTurn(name: String) {
            ErrorHandler.latch?.countDown()
        }

        override fun onServerMessage(data: Values) {
            if (data.containsKey(Draco.Player.Poker.Pot))
                ErrorHandler.latch?.countDown()
        }
    }

    gameClientP1.player.currentPot = 200
    ErrorHandler.latch?.await(10, TimeUnit.SECONDS)
    ErrorHandler.latch = CountDownLatch(1)
    gameClientP1.performAction(PokerGameClient.ClientAction.RAISE)
    ErrorHandler.latch?.await(10, TimeUnit.SECONDS)

    ErrorHandler.latch = CountDownLatch(1)
    gameClientP2.player.currentPot = 3100
    ErrorHandler.latch?.await(10, TimeUnit.SECONDS)

    ErrorHandler.latch = CountDownLatch(1)
    gameClientP2.performAction(PokerGameClient.ClientAction.RAISE)
    ErrorHandler.latch?.await(10, TimeUnit.SECONDS)

    ErrorHandler.latch = CountDownLatch(1)
    gameClientP3.player.currentPot = 3100
    ErrorHandler.latch?.await(10, TimeUnit.SECONDS)
    ErrorHandler.latch = CountDownLatch(1)
    gameClientP3.performAction(PokerGameClient.ClientAction.RAISE)
    ErrorHandler.latch?.await(10, TimeUnit.SECONDS)

    ErrorHandler.latch = CountDownLatch(1)
    gameClientP4.performAction(PokerGameClient.ClientAction.FOLD)
    ErrorHandler.latch?.await(10, TimeUnit.SECONDS)

    ErrorHandler.latch = CountDownLatch(1)
    gameClientP1.player.currentPot = 3100
    ErrorHandler.latch?.await(10, TimeUnit.SECONDS)
    ErrorHandler.latch = CountDownLatch(1)
    gameClientP1.performAction(PokerGameClient.ClientAction.RAISE)
    ErrorHandler.latch?.await(10, TimeUnit.SECONDS)

    runBlocking {
        delay(1000)
    }

    // Advance to next bidding
    ErrorHandler.latch = CountDownLatch(1)
    gameClientP1.performAction(PokerGameClient.ClientAction.CHECK)
    ErrorHandler.latch?.await(10, TimeUnit.SECONDS)

    ErrorHandler.latch = CountDownLatch(1)
    gameClientP2.player.currentPot = 5000
    ErrorHandler.latch?.await(10, TimeUnit.SECONDS)
    ErrorHandler.latch = CountDownLatch(1)
    gameClientP2.performAction(PokerGameClient.ClientAction.RAISE)
    ErrorHandler.latch?.await(10, TimeUnit.SECONDS)

    ErrorHandler.latch = CountDownLatch(1)
    gameClientP3.player.currentPot = 5000
    ErrorHandler.latch?.await(10, TimeUnit.SECONDS)
    ErrorHandler.latch = CountDownLatch(1)
    gameClientP3.performAction(PokerGameClient.ClientAction.RAISE)
    ErrorHandler.latch?.await(10, TimeUnit.SECONDS)

    ErrorHandler.latch = CountDownLatch(1)
    gameClientP1.player.currentPot = 4000
    ErrorHandler.latch?.await(10, TimeUnit.SECONDS)
    ErrorHandler.latch = CountDownLatch(1)
    gameClientP1.performAction(PokerGameClient.ClientAction.FOLD)
    ErrorHandler.latch?.await(10, TimeUnit.SECONDS)

    // Finished (everybody fold or all in) -> Advance to results
    runBlocking {
        delay(2_000)
    }

    gameClientP1.performAction(PokerGameClient.ClientAction.CANCEL)

    runBlocking {
        delay(2_000)
    }

    var latch = CountDownLatch(1)
    println(gameClientP1)
    gameClientP1.disconnect({ latch.countDown() }) { latch.countDown() }
    latch.await()

    latch = CountDownLatch(1)
    println(gameClientP2)
    gameClientP2.disconnect({ latch.countDown() }) { latch.countDown() }
    latch.await()

    latch = CountDownLatch(1)
    println(gameClientP3)
    gameClientP3.disconnect({ latch.countDown() }) { latch.countDown() }
    latch.await()

    latch = CountDownLatch(1)
    println(gameClientP4)
    gameClientP4.disconnect({ latch.countDown() }) { latch.countDown() }
    latch.await()
}