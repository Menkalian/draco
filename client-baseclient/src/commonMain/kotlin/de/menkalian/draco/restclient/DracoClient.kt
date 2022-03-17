package de.menkalian.draco.restclient

import de.menkalian.draco.data.game.Player
import de.menkalian.draco.data.game.PokerLobby
import de.menkalian.draco.data.quesstimate.GuesstimateQuestion
import de.menkalian.draco.data.quesstimate.QuestionQuery
import de.menkalian.draco.data.quesstimate.Suggestion
import de.menkalian.draco.data.telemetrie.TelemetrieReport
import de.menkalian.draco.data.user.EntitledUser
import de.menkalian.draco.data.user.UserRight
import de.menkalian.draco.restclient.config.DracoClientConfiguration
import de.menkalian.draco.restclient.error.DracoError
import de.menkalian.draco.restclient.error.catchErrors
import io.ktor.client.HttpClient
import io.ktor.client.features.HttpRedirect
import io.ktor.client.features.HttpTimeout
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.features.logging.Logging
import io.ktor.client.features.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.http.ContentType
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@Suppress("unused")
class DracoClient(private val httpClientTemplate: HttpClient, private val configuration: DracoClientConfiguration) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val httpClient: HttpClient

    private val log = configuration.logger

    init {
        log.info("Initializing DracoClient")
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

            defaultRequest {
                log.debug("Configuring default request")
                setDracoUrl()
                setTimeouts()
                contentType(ContentType.Application.Json)
            }
        }
    }

    val user = UserClient()
    val telemetrie = TelemetrieClient()
    val question = QuestionClient()
    val suggestion = SuggestionClient()
    val lobby = LobbyClient()

    private fun HttpRequestBuilder.setDracoUrl() {
        url {
            protocol = if (configuration.useHttps) {
                log.trace("Using HTTPS")
                URLProtocol.HTTPS
            } else {
                log.trace("Using HTTP")
                URLProtocol.HTTP
            }
            host = configuration.serverUrl
            port = configuration.serverPort.toInt()
            log.trace("Using host \"$host\" and port \"$port\"")
        }
    }

    private fun HttpRequestBuilder.setAuth() {
        header("auth", configuration.authToken)
    }

    private fun HttpRequestBuilder.setTimeouts() {
        timeout {
            requestTimeoutMillis = configuration.requestTimeoutMs
            connectTimeoutMillis = configuration.connectionTimeoutMs
            socketTimeoutMillis = configuration.socketTimeoutMs
        }
    }

    inner class UserClient {
        private val base = "user"

        fun create(
            name: String,
            rights: List<UserRight>,
            onCreated: (EntitledUser?) -> Unit,
            onError: (DracoError) -> Unit
        ) {
            coroutineScope.launch {
                catchErrors(onError) {
                    val created: EntitledUser = httpClient.put {
                        setAuth()
                        url.path(base)
                        body = EntitledUser(
                            name = name,
                            rights = rights
                        )
                    }
                    onCreated(created)
                }
            }
        }

        fun getAll(
            onRead: (List<EntitledUser>) -> Unit,
            onError: (DracoError) -> Unit
        ) {
            coroutineScope.launch {
                catchErrors(onError) {
                    val read: List<EntitledUser> = httpClient.get {
                        setAuth()
                        url.path(base, "all")
                    }
                    onRead(read)
                }
            }
        }

        fun getById(
            id: Int,
            onRead: (EntitledUser?) -> Unit,
            onError: (DracoError) -> Unit
        ) {
            coroutineScope.launch {
                catchErrors(onError) {
                    val read: EntitledUser? = httpClient.get {
                        setAuth()
                        url.path(base, id.toString())
                    }
                    onRead(read)
                }
            }
        }

        fun getMe(
            onRead: (EntitledUser?) -> Unit,
            onError: (DracoError) -> Unit
        ) {
            coroutineScope.launch {
                catchErrors(onError) {
                    val read: EntitledUser? = httpClient.get {
                        setAuth()
                        url.path(base, "me")
                    }
                    onRead(read)
                }
            }
        }

        fun update(
            user: EntitledUser,
            onUpdated: (EntitledUser?) -> Unit,
            onError: (DracoError) -> Unit
        ) {
            coroutineScope.launch {
                catchErrors(onError) {
                    val created: EntitledUser = httpClient.post {
                        setAuth()
                        url.path(base, user.id.toString())
                        body = user
                    }
                    onUpdated(created)
                }
            }
        }

        fun delete(
            id: Int,
            onFinished: (Boolean) -> Unit,
            onError: (DracoError) -> Unit
        ) {
            coroutineScope.launch {
                catchErrors(onError) {
                    val success: Boolean = httpClient.delete {
                        setAuth()
                        url.path(base, id.toString())
                    }
                    onFinished(success)
                }
            }
        }
    }

    inner class TelemetrieClient {
        fun startUpload(
            report: TelemetrieReport,
            onFinished: () -> Unit,
            onError: (DracoError) -> Unit
        ) {
            coroutineScope.launch {
                catchErrors(onError) {
                    httpClient.post<Any?> {
                        url.path("telemetrie/upload")
                        body = report
                    }
                    onFinished()
                }
            }
        }
    }

    inner class QuestionClient {
        private val base = "guesstimate/question"

        fun create(
            question: GuesstimateQuestion,
            onCreated: (GuesstimateQuestion) -> Unit,
            onError: (DracoError) -> Unit
        ) {
            coroutineScope.launch {
                catchErrors(onError) {
                    val created: GuesstimateQuestion = httpClient.put {
                        setAuth()
                        url.path(base)
                        body = question
                    }
                    onCreated(created)
                }
            }
        }

        fun query(
            questionQuery: QuestionQuery,
            onRead: (List<GuesstimateQuestion>) -> Unit,
            onError: (DracoError) -> Unit
        ) {
            coroutineScope.launch {
                catchErrors(onError) {
                    val read: List<GuesstimateQuestion> = httpClient.get {
                        url.path("guesstimate/questions")
                        body = questionQuery
                    }
                    onRead(read)
                }
            }
        }

        fun getAll(
            onRead: (List<GuesstimateQuestion>) -> Unit,
            onError: (DracoError) -> Unit
        ) {
            coroutineScope.launch {
                catchErrors(onError) {
                    val read: List<GuesstimateQuestion> = httpClient.get {
                        setAuth()
                        url.path(base, "all")
                    }
                    onRead(read)
                }
            }
        }

        fun getById(
            id: Int,
            onRead: (GuesstimateQuestion?) -> Unit,
            onError: (DracoError) -> Unit
        ) {
            coroutineScope.launch {
                catchErrors(onError) {
                    val read: GuesstimateQuestion? = httpClient.get {
                        setAuth()
                        url.path(base, id.toString())
                    }
                    onRead(read)
                }
            }
        }

        fun update(
            question: GuesstimateQuestion,
            onUpdated: (GuesstimateQuestion?) -> Unit,
            onError: (DracoError) -> Unit
        ) {
            coroutineScope.launch {
                catchErrors(onError) {
                    val created: GuesstimateQuestion = httpClient.post {
                        setAuth()
                        url.path(base, question.id.toString())
                        body = question
                    }
                    onUpdated(created)
                }
            }
        }

        fun delete(
            id: Int,
            onFinished: (Boolean) -> Unit,
            onError: (DracoError) -> Unit
        ) {
            coroutineScope.launch {
                catchErrors(onError) {
                    val success: Boolean = httpClient.delete {
                        setAuth()
                        url.path(base, id.toString())
                    }
                    onFinished(success)
                }
            }
        }
    }

    inner class SuggestionClient {
        private val base = "guesstimate/suggestion"

        fun create(
            question: Suggestion,
            onCreated: (Suggestion) -> Unit,
            onError: (DracoError) -> Unit
        ) {
            coroutineScope.launch {
                catchErrors(onError) {
                    val created: Suggestion = httpClient.put {
                        url.path(base)
                        body = question
                    }
                    onCreated(created)
                }
            }
        }

        fun getAll(
            onRead: (List<Suggestion>) -> Unit,
            onError: (DracoError) -> Unit
        ) {
            coroutineScope.launch {
                catchErrors(onError) {
                    val read: List<Suggestion> = httpClient.get {
                        setAuth()
                        url.path(base, "all")
                    }
                    onRead(read)
                }
            }
        }

        fun getUnread(
            onRead: (Suggestion?) -> Unit,
            onError: (DracoError) -> Unit
        ) {
            coroutineScope.launch {
                catchErrors(onError) {
                    val read: Suggestion? = httpClient.get {
                        setAuth()
                        url.path(base, "unread")
                    }
                    onRead(read)
                }
            }
        }

        fun getByUuid(
            uuid: String,
            onRead: (Suggestion?) -> Unit,
            onError: (DracoError) -> Unit
        ) {
            coroutineScope.launch {
                catchErrors(onError) {
                    val read: Suggestion? = httpClient.get {
                        setAuth()
                        url.path(base, uuid)
                    }
                    onRead(read)
                }
            }
        }

        fun update(
            suggestion: Suggestion,
            onUpdated: (Suggestion?) -> Unit,
            onError: (DracoError) -> Unit
        ) {
            coroutineScope.launch {
                catchErrors(onError) {
                    val updated: Suggestion? = httpClient.post {
                        setAuth()
                        url.path(base, suggestion.uuid)
                        body = suggestion
                    }
                    onUpdated(updated)
                }
            }
        }

        fun comment(
            uuid: String,
            comment: Suggestion.SuggestionComment,
            onSuccess: (Suggestion?) -> Unit,
            onError: (DracoError) -> Unit
        ) {
            coroutineScope.launch {
                catchErrors(onError) {
                    val updated: Suggestion? = httpClient.put {
                        setAuth()
                        url.path(base, uuid, "comment")
                        body = comment
                    }
                    onSuccess(updated)
                }
            }
        }

        fun close(
            uuid: String,
            onSuccess: (Suggestion?) -> Unit,
            onError: (DracoError) -> Unit
        ) {
            coroutineScope.launch {
                catchErrors(onError) {
                    val updated: Suggestion? = httpClient.post {
                        setAuth()
                        url.path(base, uuid, "close")
                    }
                    onSuccess(updated)
                }
            }
        }

        fun accept(
            uuid: String,
            onSuccess: (Suggestion?) -> Unit,
            onError: (DracoError) -> Unit
        ) {
            coroutineScope.launch {
                catchErrors(onError) {
                    val updated: Suggestion? = httpClient.post {
                        setAuth()
                        url.path(base, uuid, "accept")
                    }
                    onSuccess(updated)
                }
            }
        }

        fun decline(
            uuid: String,
            onSuccess: (Suggestion?) -> Unit,
            onError: (DracoError) -> Unit
        ) {
            coroutineScope.launch {
                catchErrors(onError) {
                    val updated: Suggestion? = httpClient.post {
                        setAuth()
                        url.path(base, uuid, "decline")
                    }
                    onSuccess(updated)
                }
            }
        }

        fun delete(
            uuid: String,
            onFinished: (Boolean) -> Unit,
            onError: (DracoError) -> Unit
        ) {
            coroutineScope.launch {
                catchErrors(onError) {
                    val success: Boolean = httpClient.delete {
                        setAuth()
                        url.path(base, uuid)
                    }
                    onFinished(success)
                }
            }
        }
    }

    inner class LobbyClient {
        private val base = "quizpoker/lobby"

        fun create(
            playerName: String,
            onCreated: (PokerGameClient) -> Unit,
            onError: (DracoError) -> Unit
        ) {
            coroutineScope.launch {
                catchErrors(onError) {
                    val player = Player(playerName)
                    val lobby: PokerLobby = httpClient.put {
                        url.path(base)
                        body = player
                    }

                    onCreated(PokerGameClient(player, lobby, httpClientTemplate, configuration))
                }
            }
        }

        fun connect(
            playerName: String,
            lobbyUuid: String,
            onConnected: (PokerGameClient) -> Unit,
            onError: (DracoError) -> Unit
        ) {
            coroutineScope.launch {
                catchErrors(onError) {
                    val player = Player(playerName)
                    val lobby: PokerLobby = httpClient.post {
                        url.path(base, lobbyUuid)
                        body = player
                    }

                    onConnected(PokerGameClient(player, lobby, httpClientTemplate, configuration))
                }
            }
        }

        fun connectWithToken(
            playerName: String,
            token: String,
            onConnected: (PokerGameClient) -> Unit,
            onError: (DracoError) -> Unit
        ) {
            coroutineScope.launch {
                catchErrors(onError) {
                    val uuid: String = httpClient.get {
                        url.path(base, "token", token)
                    }

                    connect(playerName, uuid, onConnected, onError)
                }
            }
        }
    }
}
