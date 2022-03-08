package de.menkalian.draco.restclient

import de.menkalian.draco.data.quesstimate.Category
import de.menkalian.draco.data.quesstimate.Difficulty
import de.menkalian.draco.data.quesstimate.GuesstimateQuestion
import de.menkalian.draco.data.quesstimate.Language
import de.menkalian.draco.data.quesstimate.QuestionQuery
import de.menkalian.draco.data.quesstimate.Suggestion
import de.menkalian.draco.data.quesstimate.SuggestionState
import de.menkalian.draco.data.telemetrie.LogReport
import de.menkalian.draco.data.telemetrie.TelemetrieReport
import de.menkalian.draco.data.user.EntitledUser
import de.menkalian.draco.data.user.UserRight
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {
    val client = DracoClientFactory.createClient(
        DracoClientConfiguration.DracoClientConfigurationBuilder()
            .useHttps(false)
            .serverUrl("localhost")
            .serverPort(8080)
            .authToken(args.getOrElse(0) { "YOUR_TOKEN_HERE" })
            .build()
    )

    // Test user API
    client.user.apply {
        ErrorHandler.latch = CountDownLatch(1)
        create(
            "Philipp",
            listOf(UserRight.USER_CREATE, UserRight.USER_READ, UserRight.USER_UPDATE, UserRight.SUGGESTION_UPDATE),
            ErrorHandler::onSuccess,
            ErrorHandler::onDracoError
        )
        ErrorHandler.latch?.await(10, TimeUnit.SECONDS)

        ErrorHandler.latch = CountDownLatch(1)
        val created = ErrorHandler.latest as EntitledUser
        val n = created.copy(
            name = "Lisa",
            rights = created.rights + UserRight.QUESTION_READ
        )
        update(n, ErrorHandler::onSuccess, ErrorHandler::onDracoError)
        ErrorHandler.latch?.await(10, TimeUnit.SECONDS)


        ErrorHandler.latch = CountDownLatch(5)
        getAll(ErrorHandler::onSuccess, ErrorHandler::onDracoError)
        getById(1, ErrorHandler::onSuccess, ErrorHandler::onDracoError)
        getById(2, ErrorHandler::onSuccess, ErrorHandler::onDracoError)
        getById(created.id, ErrorHandler::onSuccess, ErrorHandler::onDracoError)
        getMe(ErrorHandler::onSuccess, ErrorHandler::onDracoError)
        ErrorHandler.latch?.await(10, TimeUnit.SECONDS)

        ErrorHandler.latch = CountDownLatch(1)
        delete(created.id, ErrorHandler::onSuccess, ErrorHandler::onDracoError)
        ErrorHandler.latch?.await(10, TimeUnit.SECONDS)
    }

    client.telemetrie.apply {
        startUpload(
            TelemetrieReport(
                "Kilian",
                "kilian.krampf@draco.menkalian.de",
                "YEP",
                listOf(
                    LogReport(
                        "1234",
                        "AUTH",
                        "1010202",
                        "aodifsof"
                    )
                )
            ),
            { println("Successfully sent log report") },
            ErrorHandler::onDracoError
        )
    }

    client.suggestion.apply {
        ErrorHandler.latch = CountDownLatch(1)
        create(
            Suggestion(
                "",
                GuesstimateQuestion(
                    -1, "Kilian", 2040, Language.GERMAN, Difficulty.EASY,
                    Category.SCIENCE_COMPUTERS, "HOW", 32.toDouble(), "", listOf()
                ),
                SuggestionState.CREATED,
                listOf()
            ),
            ErrorHandler::onSuccess,
            ErrorHandler::onDracoError
        )
        ErrorHandler.latch?.await(10, TimeUnit.SECONDS)

        val created = ErrorHandler.latest as Suggestion

        ErrorHandler.latch = CountDownLatch(1)
        decline(created.uuid, ErrorHandler::onSuccess, ErrorHandler::onDracoError)
        ErrorHandler.latch?.await(10, TimeUnit.SECONDS)

        ErrorHandler.latch = CountDownLatch(1)
        val n = created.copy(
            suggestedQuestion = created.suggestedQuestion.copy(author = "Nailik"),
            state = SuggestionState.UPDATED
        )
        update(n, ErrorHandler::onSuccess, ErrorHandler::onDracoError)
        ErrorHandler.latch?.await(10, TimeUnit.SECONDS)

        ErrorHandler.latch = CountDownLatch(3)
        getAll(ErrorHandler::onSuccess, ErrorHandler::onDracoError)
        getUnread(ErrorHandler::onSuccess, ErrorHandler::onDracoError)
        getByUuid(created.uuid, ErrorHandler::onSuccess, ErrorHandler::onDracoError)
        ErrorHandler.latch?.await(10, TimeUnit.SECONDS)

        ErrorHandler.latch = CountDownLatch(2)
        comment(created.uuid, Suggestion.SuggestionComment("admin", "yepp", 42), ErrorHandler::onSuccess, ErrorHandler::onDracoError)
        accept(created.uuid, ErrorHandler::onSuccess, ErrorHandler::onDracoError)
        ErrorHandler.latch?.await(10, TimeUnit.SECONDS)

        ErrorHandler.latch = CountDownLatch(1)
        delete(created.uuid, ErrorHandler::onSuccess, ErrorHandler::onDracoError)
        ErrorHandler.latch?.await(10, TimeUnit.SECONDS)
    }

    client.question.apply {
        ErrorHandler.latch = CountDownLatch(1)
        create(
            GuesstimateQuestion(-1, "asda", 1020, Language.GERMAN, Difficulty.HARD, Category.CELEBRITIES, "Whichen't", 40.0, "", listOf()),
            ErrorHandler::onSuccess,
            ErrorHandler::onDracoError
        )
        ErrorHandler.latch?.await(10, TimeUnit.SECONDS)

        ErrorHandler.latch = CountDownLatch(1)
        val created = ErrorHandler.latest as GuesstimateQuestion
        val n = created.copy(
            author = "ASDFASFSF",
        )
        update(n, ErrorHandler::onSuccess, ErrorHandler::onDracoError)
        ErrorHandler.latch?.await(10, TimeUnit.SECONDS)


        ErrorHandler.latch = CountDownLatch(3)
        query(QuestionQuery(), ErrorHandler::onSuccess, ErrorHandler::onDracoError)
        getAll(ErrorHandler::onSuccess, ErrorHandler::onDracoError)
        getById(created.id, ErrorHandler::onSuccess, ErrorHandler::onDracoError)
        ErrorHandler.latch?.await(10, TimeUnit.SECONDS)

        ErrorHandler.latch = CountDownLatch(1)
        delete(created.id, ErrorHandler::onSuccess, ErrorHandler::onDracoError)
        ErrorHandler.latch?.await(10, TimeUnit.SECONDS)
    }
}

object ErrorHandler {
    var latch: CountDownLatch? = null
    var latest: Any? = null

    fun onSuccess(any: Any?) {
        println("Success: $any")
        latest = any
        latch?.countDown()
    }

    fun onDracoError(error: DracoError) {
        println("Error: $error")
        latest = null
        latch?.countDown()
    }
}
