package de.menkalian.draco.server.rest.guesstimate

import de.menkalian.draco.data.quesstimate.Suggestion
import de.menkalian.draco.data.quesstimate.SuggestionState
import de.menkalian.draco.data.user.UserRight
import de.menkalian.draco.server.database.guesstimate.question.IQuestionDatabase
import de.menkalian.draco.server.database.guesstimate.suggestion.ISuggestionDatabase
import de.menkalian.draco.server.database.user.IUserDatabase
import de.menkalian.draco.server.util.MissingAuthenticationException
import de.menkalian.draco.server.util.NotFoundException
import de.menkalian.draco.server.util.UserRightsNotSufficientException
import de.menkalian.draco.server.util.authTokenHash
import de.menkalian.draco.server.util.logger
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class SuggestionActionsRestController(
    val suggestionDatabase: ISuggestionDatabase,
    val questionDatabase: IQuestionDatabase,
    val userDatabase: IUserDatabase
) {
    @PutMapping("guesstimate/suggestion/{uuid}/comment")
    fun addComment(
        @RequestHeader("auth", required = false, defaultValue = "") authToken: String,
        @PathVariable("uuid") uuid: String,
        @RequestBody comment: Suggestion.SuggestionComment
    ): Suggestion {
        return checkForRights(authToken, "add suggestion comment", UserRight.SUGGESTION_COMMENT_CREATE) {
            suggestionDatabase.addNote(uuid, comment) ?: throw NotFoundException()
        }
    }

    @PostMapping("guesstimate/suggestion/{uuid}/close")
    fun closeSuggestion(
        @RequestHeader("auth", required = false, defaultValue = "") authToken: String,
        @PathVariable("uuid") uuid: String
    ): Suggestion {
        return checkForRights(authToken, "close suggestion", UserRight.SUGGESTION_UPDATE) {
            return@checkForRights suggestionDatabase.setState(uuid, SuggestionState.CLOSED) ?: throw NotFoundException()
        }
    }

    @PostMapping("guesstimate/suggestion/{uuid}/accept")
    fun acceptSuggestion(
        @RequestHeader("auth", required = false, defaultValue = "") authToken: String,
        @PathVariable("uuid") uuid: String
    ): Suggestion {
        return checkForRights(authToken, "accept suggestion", UserRight.SUGGESTION_UPDATE, UserRight.QUESTION_CREATE) {
            val suggestion = suggestionDatabase.getSuggestion(uuid)
            if (suggestion != null) {
                questionDatabase.createQuestion(suggestion.suggestedQuestion)
            }
            return@checkForRights suggestionDatabase.setState(uuid, SuggestionState.ACCEPTED) ?: throw NotFoundException()
        }
    }

    @PostMapping("guesstimate/suggestion/{uuid}/decline")
    fun declineSuggestion(
        @RequestHeader("auth", required = false, defaultValue = "") authToken: String,
        @PathVariable("uuid") uuid: String
    ): Suggestion {
        return checkForRights(authToken, "decline suggestion", UserRight.SUGGESTION_UPDATE) {
            return@checkForRights suggestionDatabase.setState(uuid, SuggestionState.NEEDS_WORK) ?: throw NotFoundException()
        }
    }

    private fun <T> checkForRights(authToken: String, requestType: String, vararg requiredRights: UserRight, onAuthorized: () -> T): T {
        val authenticatedUser = userDatabase.getUser(authToken.authTokenHash())
            ?: throw MissingAuthenticationException().apply { logger().warn("No Authentication found.") }
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
