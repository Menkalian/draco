package de.menkalian.draco.restclient

import de.menkalian.draco.currentSystemTimeMillis
import de.menkalian.draco.data.Draco
import de.menkalian.draco.data.game.Player
import de.menkalian.draco.data.game.PokerLobby
import de.menkalian.draco.data.game.SocketPackage
import de.menkalian.draco.data.game.enums.AnswerRevealStrategy
import de.menkalian.draco.data.game.enums.BlindRaiseStrategy
import de.menkalian.draco.data.game.enums.ConnectionState
import de.menkalian.draco.data.game.enums.GameState
import de.menkalian.draco.data.game.enums.LateJoinBehaviour
import de.menkalian.draco.data.game.enums.LobbyPublicity
import de.menkalian.draco.data.game.enums.PackageType
import de.menkalian.draco.data.game.enums.PlayerRole
import de.menkalian.draco.data.game.enums.TimeoutStrategy
import de.menkalian.draco.data.game.values.RegexValueMultiChangeListener
import de.menkalian.draco.data.game.values.TransferableValue
import de.menkalian.draco.data.game.values.Values
import de.menkalian.draco.data.quesstimate.Category
import de.menkalian.draco.data.quesstimate.Difficulty
import de.menkalian.draco.data.quesstimate.GuesstimateQuestion
import de.menkalian.draco.data.quesstimate.Language
import de.menkalian.draco.restclient.config.DracoClientConfiguration
import de.menkalian.draco.restclient.error.DracoError
import io.ktor.client.HttpClient
import io.ktor.client.features.ClientRequestException
import io.ktor.client.features.HttpRedirect
import io.ktor.client.features.HttpRequestTimeoutException
import io.ktor.client.features.HttpTimeout
import io.ktor.client.features.RedirectResponseException
import io.ktor.client.features.ServerResponseException
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.features.logging.Logging
import io.ktor.client.features.timeout
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.webSocket
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.URLProtocol
import io.ktor.http.cio.websocket.FrameType
import io.ktor.http.cio.websocket.WebSocketSession
import io.ktor.http.cio.websocket.close
import io.ktor.http.cio.websocket.readBytes
import io.ktor.http.cio.websocket.send
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Suppress("unused", "MemberVisibilityCanBePrivate")
class PokerGameClient
    (
    val player: Player,
    val lobby: PokerLobby,
    httpClientTemplate: HttpClient,
    private val configuration: DracoClientConfiguration
) {
    companion object {
        var msgIdCounter = 0L
        val mutex = Mutex(false)
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val httpClient: HttpClient
    private val websocketHandler: WebsocketHandler
    private val heartbeatHandler: HeartbeatHandler

    private var disconnected: Boolean = false

    private val dataChangeWatcher: RegexValueMultiChangeListener

    var clientListener: ClientEventListener = object : ClientEventListener {}

    init {
        httpClient = httpClientTemplate.config {
            install(HttpTimeout)
            install(HttpRedirect)
            install(JsonFeature) {
                serializer = KotlinxSerializer(Json {
                    this.ignoreUnknownKeys = true
                })
            }
            install(Logging) {
                logger = configuration.ktorLogger
                level = configuration.logLevel
            }
            install(WebSockets)

            defaultRequest {
                setTimeouts()
                contentType(ContentType.Application.Json)
            }
        }
        websocketHandler = WebsocketHandler()
        heartbeatHandler = HeartbeatHandler()
        websocketHandler.addPackageHandler(PackageType.SERVER_BROADCAST, AsynchronousDataUpdateHandler())
        dataChangeWatcher = RegexValueMultiChangeListener(".+".toRegex()) {
            coroutineScope.launch {
                val pkg = SocketPackage(0, PackageType.CLIENT_MSG, 0, it)
                websocketHandler.sendPackage(pkg)
            }
        }
        player.addListener(dataChangeWatcher)
        if (player.name == lobby.host.name) {
            lobby.settings.addListener(dataChangeWatcher)
        }
    }

    private fun HttpRequestBuilder.setRestUrl() {
        url {
            protocol = if (configuration.useHttps) {
                URLProtocol.HTTPS
            } else {
                URLProtocol.HTTP
            }
            host = lobby.connectionValues.restHost
            port = lobby.connectionValues.restPort
        }
    }

    private fun HttpRequestBuilder.setTimeouts() {
        timeout {
            requestTimeoutMillis = configuration.requestTimeoutMs
            connectTimeoutMillis = configuration.connectionTimeoutMs
            socketTimeoutMillis = configuration.socketTimeoutMs
        }
    }

    fun disconnect(
        onDisconnected: () -> Unit,
        onError: (DracoError) -> Unit
    ) {
        disconnected = true
        coroutineScope.launch {
            catchErrors(onError) {
                websocketHandler.disconnect()
                httpClient.delete<Any> {
                    setRestUrl()
                    url.path("quizpoker/lobby", lobby.uuid)
                    body = player
                }
                onDisconnected()
            }
        }
    }

    fun refreshData(
        onError: (DracoError) -> Unit
    ) {
        coroutineScope.launch {
            catchErrors(onError) {
                val currentData = httpClient.get<PokerLobby> {
                    setRestUrl()
                    url.path("quizpoker/lobby", lobby.uuid)
                }

                mutex.withLock {
                    lobby.apply {
                        players.clear()
                        players.addAll(currentData.players)

                        gameStateValues = currentData.gameStateValues
                        settings = currentData.settings
                        if (player.name == host.name) {
                            settings.addListener(dataChangeWatcher)
                        }
                    }

                    val currentPlayerData = currentData.players.firstOrNull { it.name == player.name }
                    if (currentPlayerData != null) {
                        player.apply {
                            role = currentPlayerData.role
                            score = currentPlayerData.score
                            currentAnswer = currentPlayerData.currentAnswer
                            answerRevealed = currentPlayerData.answerRevealed
                            currentPot = currentPlayerData.currentPot
                            folded = currentPlayerData.folded
                        }
                    }
                }
            }
        }
    }

    enum class ClientAction(internal val actionKey: String) {
        RAISE(Draco.Action.Player.Raise),
        CHECK(Draco.Action.Player.Check),
        FOLD(Draco.Action.Player.Fold),
        REVEAL_SELF(Draco.Action.Player.Reveal),

        START(Draco.Action.Host.StartGame),
        CANCEL(Draco.Action.Host.CancelGame),

        ACK(Draco.Action.Quizmaster.Acknowledge),
        REVEAL_PLAYER(Draco.Action.Quizmaster.Reveal)
    }

    fun revealPlayer(playerName: String) {
        performAction(
            ClientAction.REVEAL_PLAYER,
            mutableMapOf(
                Draco.Action.Quizmaster.RevealName to TransferableValue.from(playerName)
            )
        )
    }

    fun performAction(action: ClientAction, data: Values = mutableMapOf()) {
        val msgData = mutableMapOf(
            action.actionKey to TransferableValue.from(true)
        )
        msgData.putAll(data)

        val pkg = SocketPackage(0, PackageType.CLIENT_MSG, 0, msgData)
        coroutineScope.launch {
            websocketHandler.sendPackage(pkg)
        }
    }

    fun interface SocketPackageHandler {
        fun onPackageReceived(pkg: SocketPackage)
    }

    interface ClientEventListener {
        fun onWebsocketError(ex: Exception) {}
        fun onQuestion(question: GuesstimateQuestion) {}

        fun onPlayerTurn(name: String) {}
        fun onOwnPlayerTurn() {}

        fun onPlayerBroke(name: String) {}
        fun onOwnPlayerBroke() {}

        fun onServerMessage(data: Values) {}

        fun onLobby() {}
        fun onGameStart() {}
        fun onRoundStart() {}
        fun onRoundPaused() {}
    }

    inner class WebsocketHandler {
        private var session: WebSocketSession? = null
        private val serializer = Json {
            ignoreUnknownKeys = true
        }

        private val packageHandlers: Map<PackageType, MutableList<SocketPackageHandler>> = PackageType.values().associateWith { mutableListOf() }
        private val acknowledgeHandlers: Map<PackageType, AcknowledgeHandler> = PackageType.values().associateWith { AcknowledgeHandler() }

        init {
            coroutineScope.launch {
                connectWebsocket()
            }
            acknowledgeHandlers.forEach {
                addPackageHandler(it.key, it.value)
            }
        }

        private suspend fun connectWebsocket() {
            session?.close()
            httpClient.webSocket(
                HttpMethod.Get,
                "127.0.0.1",
                8081,
                "/socket"
            ) {
                session = this
                sendInitialMessages()

                for (frame in incoming) {
                    try {
                        if (frame.frameType == FrameType.CLOSE) {
                            break
                        }

                        if (disconnected) {
                            close()
                        }

                        val pkg = serializer.decodeFromString<SocketPackage>(frame.readBytes().decodeToString())
                        acknowledge(pkg)
                        fireHandlers(pkg)
                    } catch (ex: Exception) {
                        clientListener.onWebsocketError(ex)
                    }
                }

                if (!disconnected) {
                    coroutineScope.launch {
                        delay(2000)
                        connectWebsocket()
                    }
                } else {
                    close()
                    session = null
                }
            }
        }

        fun disconnect() {
            coroutineScope.launch {
                session?.close()
            }
        }

        private suspend fun WebSocketSession.acknowledge(pkg: SocketPackage) {
            val acknowledgeType = pkg.type.getAcknowledge()
            if (acknowledgeType != null) {
                val acknowledge = SocketPackage(pkg.id, acknowledgeType, currentSystemTimeMillis(), mutableMapOf())
                send(serializer.encodeToString(acknowledge))
            }
        }

        private fun sendInitialMessages() {
            coroutineScope.launch {
                val helloPackage = SocketPackage(
                    0, PackageType.CLIENT_HELLO, 0, mutableMapOf(
                        Draco.Game.Player.Name to TransferableValue.from(player.name),
                        Draco.Game.Lobby.Id to TransferableValue.from(lobby.uuid)
                    )
                )
                sendPackage(helloPackage, onAcknowledge = { refreshData { } })
            }
        }

        private fun fireHandlers(pkg: SocketPackage) {
            coroutineScope.launch {
                mutex.withLock {
                    packageHandlers[pkg.type]?.forEach {
                        coroutineScope.launch {
                            it.onPackageReceived(pkg)
                        }
                    }
                }
            }
        }

        fun addPackageHandler(type: PackageType, handler: SocketPackageHandler) {
            coroutineScope.launch {
                mutex.withLock {
                    packageHandlers[type]!!.add(handler)
                }
            }
        }

        fun removePackageHandler(type: PackageType, handler: SocketPackageHandler) {
            coroutineScope.launch {
                mutex.withLock {
                    packageHandlers[type]!!.remove(handler)
                }
            }
        }

        fun sendPackage(
            pkg: SocketPackage,
            acknowledgeTimeoutMs: Long = 5000,
            onSent: (success: Boolean, pkg: SocketPackage?) -> Unit = { _, _ -> },
            onAcknowledge: () -> Unit = {},
            onAcknowledgeFailed: () -> Unit = {}
        ) {
            coroutineScope.launch {
                val modPackage: SocketPackage
                mutex.withLock {
                    modPackage = pkg.copy(id = msgIdCounter++, timestamp = currentSystemTimeMillis())
                }
                val acknowledgeType = modPackage.type.getAcknowledge()

                if (acknowledgeType != null) {
                    val timeoutJob = coroutineScope.launch {
                        delay(acknowledgeTimeoutMs)
                        if (!acknowledgeHandlers[acknowledgeType]!!.cancelAcknowledgement(modPackage.id)) {
                            onAcknowledgeFailed()
                        }
                    }

                    acknowledgeHandlers[acknowledgeType]!!.requestAcknowledgement(
                        AcknowledgeRequest(modPackage.id, acknowledgeType, timeoutJob, onAcknowledge)
                    )
                }

                try {
                    session?.send(serializer.encodeToString(modPackage).encodeToByteArray())
                    onSent(true, modPackage)

                    if (acknowledgeType == null) {
                        onAcknowledge()
                    }
                } catch (ex: Exception) {
                    onSent(false, null)
                }
            }
        }
    }

    inner class AsynchronousDataUpdateHandler : SocketPackageHandler {
        override fun onPackageReceived(pkg: SocketPackage) {
            coroutineScope.launch { clientListener.onServerMessage(pkg.data) }

            if (pkg.data.containsKey(Draco.Game.Player.Name)) {
                // Handle player update
                val playerName = pkg.data[Draco.Game.Player.Name]!!.toString()
                coroutineScope.launch {
                    mutex.withLock {
                        var player = lobby.players.firstOrNull { it.name == playerName }
                        if (player == null) {
                            player = Player(playerName)
                            lobby.players.add(player)
                        }

                        pkg.data.forEach {
                            applyToPlayer(player, it, pkg)

                            if (player.score == 0L && player.currentPot == 0L) {
                                clientListener.onPlayerBroke(playerName)
                            }

                            if (player.name == this@PokerGameClient.player.name) {
                                applyToPlayer(this@PokerGameClient.player, it, pkg)

                                if (player.score == 0L && player.currentPot == 0L) {
                                    clientListener.onOwnPlayerBroke()
                                }
                            }
                        }
                    }
                }
            } else {
                // Handle lobby data update
                coroutineScope.launch {
                    mutex.withLock {
                        pkg.data.forEach {
                            if (it.key.startsWith(Draco.Game.Poker.Settings.toString())) {
                                applyToSettings(it, pkg)
                            } else {
                                applyToState(it, pkg)
                            }
                        }
                    }
                }
            }
        }

        private fun applyToPlayer(
            player: Player,
            it: Map.Entry<String, TransferableValue>,
            pkg: SocketPackage
        ) {
            when (it.key) {
                Draco.Player.Connection.State -> player.connectionState = ConnectionState.valueOf(it.value.toString())
                Draco.Player.Connection.Ping  -> player.lastKnownPing = it.value.toLong()
                Draco.Player.Poker.Role       -> player.role = PlayerRole.valueOf(it.value.toString())
                Draco.Player.Poker.Score      -> player.score = it.value.toLong()
                Draco.Player.Poker.Answer     -> player.currentAnswer = it.value.toLong()
                Draco.Player.Poker.Revealed   -> player.answerRevealed = it.value.toBoolean()
                Draco.Player.Poker.Pot        -> player.currentPot = it.value.toLong()
                Draco.Player.Poker.Folded     -> player.folded = it.value.toBoolean()
            }
        }

        private fun applyToSettings(
            it: Map.Entry<String, TransferableValue>,
            pkg: SocketPackage
        ) {
            val value = it.value
            when (it.key) {
                Draco.Game.Poker.Settings.Lobby.Name       -> lobby.settings.lobbyName = value.toString()
                Draco.Game.Poker.Settings.Lobby.Publicity  -> lobby.settings.publicity = LobbyPublicity.valueOf(value.toString())
                Draco.Game.Poker.Settings.DefaultPoints    -> lobby.settings.defaultStartScore = value.toLong()
                Draco.Game.Poker.Settings.Timeout          -> lobby.settings.timeoutMs = value.toLong()
                Draco.Game.Poker.Settings.MaxQuestions     -> lobby.settings.maxQuestionCount = value.toLong()
                Draco.Game.Poker.Settings.KickBroke        -> lobby.settings.kickWhenBroke = value.toBoolean(true)
                Draco.Game.Poker.Settings.ShowHelpWarnings -> lobby.settings.showHelpWarnings = value.toBoolean(true)
                Draco.Game.Poker.Settings.LateJoin         -> lobby.settings.allowLateJoin = value.toBoolean(true)
                Draco.Game.Poker.Settings.BlindStrategy    -> lobby.settings.blindRaiseStrategy = BlindRaiseStrategy.valueOf(value.toString())
                Draco.Game.Poker.Settings.RevealStrategy   -> lobby.settings.answerRevealStrategy = AnswerRevealStrategy.valueOf(value.toString())
                Draco.Game.Poker.Settings.TimeoutStrategy  -> lobby.settings.timeoutStrategy = TimeoutStrategy.valueOf(value.toString())
                Draco.Game.Poker.Settings.LateJoinStrategy -> lobby.settings.lateJoinBehaviour = LateJoinBehaviour.valueOf(value.toString())
                Draco.Game.Poker.Settings.Categories.n     -> {
                    val categories = mutableSetOf<Category>()
                    for (i in 1..value.toInt()) {
                        categories.add(
                            Category.valueOf(
                                pkg.data[Draco.Game.Poker.Settings.Categories.XXX(i).Name]?.toString() ?: Category.ART.name
                            )
                        )
                    }
                    lobby.settings.allowedCategories = categories
                }
                Draco.Game.Poker.Settings.Difficulties.n   -> {
                    val difficulties = mutableSetOf<Difficulty>()
                    for (i in 1..value.toInt()) {
                        difficulties.add(
                            Difficulty.valueOf(
                                pkg.data[Draco.Game.Poker.Settings.Difficulties.XXX(i).Name]?.toString() ?: Difficulty.EASY.name
                            )
                        )
                    }
                    lobby.settings.allowedDifficulties = difficulties
                }
                Draco.Game.Poker.Settings.Languages.n      -> {
                    val languages = mutableSetOf<Language>()
                    for (i in 1..value.toInt()) {
                        languages.add(
                            Language.valueOf(
                                pkg.data[Draco.Game.Poker.Settings.Languages.XXX(i).Name]?.toString() ?: Language.ENGLISH.name
                            )
                        )
                    }
                    lobby.settings.allowedLanguages = languages
                }
                Draco.Game.Poker.Settings.Blinds.n         -> {
                    val blinds = mutableListOf<Pair<Long, Long>>()
                    for (i in 1..value.toInt()) {
                        blinds.add(
                            (pkg.data[Draco.Game.Poker.Settings.Blinds.XXX(i).Small]?.toLong() ?: 0L)
                                    to (pkg.data[Draco.Game.Poker.Settings.Blinds.XXX(i).Big]?.toLong() ?: 0L)
                        )
                    }
                    lobby.settings.blindLevels = blinds
                }
            }
        }

        private fun applyToState(
            it: Map.Entry<String, TransferableValue>,
            pkg: SocketPackage
        ) {
            val value = it.value
            when (it.key) {
                Draco.Game.Poker.State           -> {
                    lobby.gameStateValues.state = GameState.valueOf(value.toString())

                    when (lobby.gameStateValues.state) {
                        GameState.LOBBY    -> clientListener.onLobby()
                        GameState.STARTING -> clientListener.onGameStart()
                        GameState.QUESTION -> clientListener.onRoundStart()
                        GameState.PAUSE    -> clientListener.onRoundPaused()
                    }
                }
                Draco.Game.Poker.CurrentPlayer   -> {
                    lobby.gameStateValues.currentPlayer = lobby.players.firstOrNull { it.name == value.toString() }
                    lobby.gameStateValues.currentPlayer?.name?.let { name ->
                        clientListener.onPlayerTurn(name)
                        if (name == player.name) {
                            clientListener.onOwnPlayerTurn()
                        }
                    }

                }
                Draco.Game.Poker.CurrentBid      -> lobby.gameStateValues.currentBid = value.toLong()
                Draco.Game.Poker.Round           -> lobby.gameStateValues.round = value.toInt()
                Draco.Game.Poker.Blinds.Small    -> lobby.gameStateValues.smallBlind = value.toLong()
                Draco.Game.Poker.Blinds.Big      -> lobby.gameStateValues.bigBlind = value.toLong()
                Draco.Game.Poker.Question.UUID   -> {
                    try {
                        val id = value.toInt()
                        lobby.gameStateValues.currentQuestion = lobby.gameStateValues.currentQuestion?.copy(id = id)
                            ?: GuesstimateQuestion(id, "", 0L, Language.ENGLISH, Difficulty.EASY, Category.ART, "", 0.0, "", listOf())

                        coroutineScope.launch {
                            delay(500)
                            val currentQuestion = lobby.gameStateValues.currentQuestion
                            if (currentQuestion != null) {
                                clientListener.onQuestion(currentQuestion)
                            }
                        }
                    } catch (ex: Exception) {
                        lobby.gameStateValues.currentQuestion = null
                    }
                }
                Draco.Game.Poker.Question.Text   -> {
                    try {
                        lobby.gameStateValues.currentQuestion = lobby.gameStateValues.currentQuestion?.copy(question = value.toString())
                            ?: GuesstimateQuestion(0, "", 0L, Language.ENGLISH, Difficulty.EASY, Category.ART, value.toString(), 0.0, "", listOf())
                    } catch (ex: Exception) {
                        lobby.gameStateValues.currentQuestion = null
                    }
                }
                Draco.Game.Poker.Question.Answer -> {
                    try {
                        val answer = value.toDouble()
                        lobby.gameStateValues.currentQuestion = lobby.gameStateValues.currentQuestion?.copy(answer = answer)
                            ?: GuesstimateQuestion(0, "", 0L, Language.ENGLISH, Difficulty.EASY, Category.ART, "", answer, "", listOf())
                    } catch (ex: Exception) {
                        lobby.gameStateValues.currentQuestion = null
                    }
                }
                Draco.Game.Poker.Question.Hint.n -> {
                    lobby.gameStateValues.clearHints()
                    for (i in 1..value.toInt()) {
                        lobby.gameStateValues.showHint(
                            pkg.data[Draco.Game.Poker.Question.Hint.XXX(i).Text]?.toString() ?: "N/A"
                        )
                    }
                }
            }
        }
    }

    inner class HeartbeatHandler {
        private var failedHeartbeats = 0

        init {
            coroutineScope.launch {
                sendHeartbeats()
            }
        }

        private suspend fun sendHeartbeats() {
            val heartbeatPackage = SocketPackage(0, PackageType.HEARTBEAT, 0, mutableMapOf())
            while (true) {
                var sent = 0L
                websocketHandler.sendPackage(
                    heartbeatPackage,
                    onSent = { success, pkg ->
                        if (success && pkg != null) {
                            sent = pkg.timestamp
                        }
                    },
                    onAcknowledge = {
                        val time = currentSystemTimeMillis()
                        val ping = (time - sent) / 2
                        player.lastKnownPing = ping
                    },
                    onAcknowledgeFailed = {
                        failedHeartbeats++
                        println("HEARTBEAT_FAILED")
                        if (failedHeartbeats > lobby.connectionValues.heartbeatMaxMisses) {
                            refreshData { }
                        }
                    })
                delay(lobby.connectionValues.heartbeatRate.toLong())
            }
        }
    }

    private data class AcknowledgeRequest(
        val id: Long,
        val type: PackageType,
        val timeoutJob: Job,
        val onAcknowledge: () -> Unit
    )

    private class AcknowledgeHandler : SocketPackageHandler {
        private val runningScope = CoroutineScope(Dispatchers.Default)
        private val mutex = Mutex()

        private val openAcknowledgements = mutableListOf<AcknowledgeRequest>()

        fun requestAcknowledgement(request: AcknowledgeRequest) {
            runningScope.launch {
                mutex.withLock {
                    openAcknowledgements.add(request)
                }
            }
        }

        fun cancelAcknowledgement(id: Long): Boolean {
            runningScope.launch {
                mutex.withLock {
                    val canceled = openAcknowledgements.firstOrNull { it.id == id }
                    if (canceled != null) {
                        openAcknowledgements.remove(canceled)
                    }
                }
            }
            return true
        }

        override fun onPackageReceived(pkg: SocketPackage) {
            runningScope.launch {
                mutex.withLock {
                    val acknowledged = openAcknowledgements.firstOrNull { it.id == pkg.id }

                    if (acknowledged != null) {
                        openAcknowledgements.remove(acknowledged)
                        acknowledged.timeoutJob.cancel()
                        acknowledged.onAcknowledge()
                    }
                }
            }
        }
    }

    private suspend fun catchErrors(
        onError: (DracoError) -> Unit,
        executable: suspend () -> Unit
    ) {
        try {
            executable()
        } catch (ex: RedirectResponseException) {
            onError(
                DracoError(
                    ex.response.status.value,
                    "${ex.response.status.description}: ${ex.message}",
                    ex
                )
            )
        } catch (ex: ClientRequestException) {
            onError(
                DracoError(
                    ex.response.status.value,
                    "${ex.response.status.description}: ${ex.message}",
                    ex
                )
            )
        } catch (ex: ServerResponseException) {
            onError(
                DracoError(
                    ex.response.status.value,
                    "${ex.response.status.description}: ${ex.message}",
                    ex
                )
            )
        } catch (ex: HttpRequestTimeoutException) {
            onError(
                DracoError(
                    DracoError.ERR_TIMEOUT,
                    "Timeout for request: \"${ex.message}\"",
                    ex
                )
            )
        } catch (ex: Exception) {
            onError(
                DracoError(
                    DracoError.ERR_UNKNOWN,
                    "Unknown Error: \"${ex.message}\"",
                    ex
                )
            )
        }
    }

    override fun toString(): String {
        return "PokerGameClient(player=$player, lobby=$lobby, configuration=$configuration)"
    }
}