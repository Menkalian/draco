package de.menkalian.draco.server.rest.guesstimate

import de.menkalian.draco.data.quesstimate.Category
import de.menkalian.draco.data.quesstimate.Difficulty
import de.menkalian.draco.data.quesstimate.GuesstimateQuestion
import de.menkalian.draco.data.quesstimate.Language
import de.menkalian.draco.data.quesstimate.QuestionQuery
import de.menkalian.draco.data.user.UserRight
import de.menkalian.draco.server.database.guesstimate.question.IQuestionDatabase
import de.menkalian.draco.server.database.user.IUserDatabase
import de.menkalian.draco.server.util.MissingAuthenticationException
import de.menkalian.draco.server.util.NotFoundException
import de.menkalian.draco.server.util.UserRightsNotSufficientException
import de.menkalian.draco.server.util.authTokenHash
import de.menkalian.draco.server.util.logger
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class QuestionCrudRestController(val database: IQuestionDatabase, val userDatabase: IUserDatabase) {
    @PutMapping("guesstimate/question")
    fun createQuestion(
        @RequestHeader("auth", required = false, defaultValue = "") authToken: String,
        @RequestBody question: GuesstimateQuestion
    ): GuesstimateQuestion {
        return checkForRights(authToken, "create question", UserRight.QUESTION_CREATE) {
            logger().debug("Creating question $question")
            return@checkForRights database.createQuestion(question)
        }
    }

    @GetMapping("guesstimate/question/all")
    fun getAllQuestions(
        @RequestHeader("auth", required = false, defaultValue = "") authToken: String,
    ): List<GuesstimateQuestion> {
        return checkForRights(authToken, "get all questions", UserRight.QUESTION_READ) {
            database.getAllQuestions()
        }
    }

    @GetMapping("guesstimate/questions")
    fun queryQuestions(
        @RequestBody(required = false) questionQuery: QuestionQuery?,
        @RequestParam("amount") amount: Int?,
        @RequestParam("difficulty") difficulties: String?,
        @RequestParam("category") categories: String?,
        @RequestParam("language") languages: String?
    ): List<GuesstimateQuestion> {
        val defaultQuery = questionQuery ?: QuestionQuery()
        val updatedQuery = QuestionQuery(
            amount ?: defaultQuery.amount,
            languages?.split(",")?.map { Language.valueOf(it) } ?: defaultQuery.languages,
            categories?.split(",")?.map { Category.valueOf(it) } ?: defaultQuery.categories,
            difficulties?.split(",")?.map { Difficulty.valueOf(it) } ?: defaultQuery.difficulties
        )

        return database.queryQuestions(updatedQuery)
    }

    @GetMapping("guesstimate/question/{id}")
    fun getQuestion(
        @PathVariable("id") id: Int
    ): GuesstimateQuestion {
        return database.getQuestion(id) ?: throw NotFoundException()
    }

    @PostMapping("guesstimate/question/{id}")
    fun modifyQuestion(
        @RequestHeader("auth", required = false, defaultValue = "") authToken: String,
        @PathVariable("id") id: Int,
        @RequestBody modifiedQuestion: GuesstimateQuestion
    ): GuesstimateQuestion {
        return checkForRights(authToken, "update question $id", UserRight.QUESTION_UPDATE) {
            logger().debug("Updating question $id to $modifiedQuestion")
            return@checkForRights database.updateQuestion(id, modifiedQuestion) ?: throw NotFoundException()
        }
    }

    @DeleteMapping("guesstimate/question/{id}")
    fun deleteQuestion(
        @RequestHeader("auth", required = false, defaultValue = "") authToken: String,
        @PathVariable("id") id: Int
    ): Boolean {
        return checkForRights(authToken, "delete question $id", UserRight.QUESTION_DELETE) {
            database.deleteQuestion(id)
        }
    }

    private fun <T> checkForRights(authToken: String, requestType: String, vararg requiredRights: UserRight, onAuthorized: () -> T): T {
        val authenticatedUser = userDatabase.getUser(authToken.authTokenHash())
            ?: throw MissingAuthenticationException()
        logger().info("Received \"$requestType\"-request by ${authenticatedUser.name}")
        logger().debug("Requires rights: ${requiredRights.toList()}")

        if (requiredRights.all { right -> authenticatedUser hasRight right }) {
            logger().debug("User ${authenticatedUser.name} authorized")
            return onAuthorized()
        } else {
            logger().warn("User ${authenticatedUser.name} does not have sufficient rights")
            logger().debug("Missing rights: ${requiredRights.filterNot { right -> authenticatedUser hasRight right }}")
            throw UserRightsNotSufficientException()
        }
    }
}