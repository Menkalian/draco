package de.menkalian.draco.server.rest.guesstimate

import de.menkalian.draco.data.quesstimate.Suggestion
import de.menkalian.draco.data.quesstimate.SuggestionState
import de.menkalian.draco.data.user.UserRight
import de.menkalian.draco.server.database.guesstimate.suggestion.ISuggestionDatabase
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
import org.springframework.web.bind.annotation.RestController

@RestController
class SuggestionCrudRestController(
    val database: ISuggestionDatabase,
    val userDatabase: IUserDatabase
) {
    @PutMapping("guesstimate/suggestion")
    fun createSuggestion(
        @RequestBody suggestion: Suggestion
    ): Suggestion {
        logger().debug("Creating suggestion $suggestion")
        val toRet = database.createSuggestion(suggestion)
        database.setState(toRet.uuid, SuggestionState.CREATED)
        return toRet
    }

    @GetMapping("guesstimate/suggestion/all")
    fun getAllSuggestions(
        @RequestHeader("auth", required = false, defaultValue = "") authToken: String,
    ): List<Suggestion> {
        return checkForRights(authToken, "get all suggestions", UserRight.SUGGESTION_READ) {
            database.getAllSuggestions()
        }
    }

    @GetMapping("guesstimate/suggestion/unread")
    fun getUnreadSuggestions(
        @RequestHeader("auth", required = false, defaultValue = "") authToken: String,
    ): Suggestion? {
        return checkForRights(authToken, "get unread suggestion", UserRight.SUGGESTION_READ) {
            database.getUnreadSuggestion()
        }
    }

    @GetMapping("guesstimate/suggestion/{uuid}")
    fun getSuggestion(
        @PathVariable("uuid") uuid: String
    ): Suggestion {
        return database.getSuggestion(uuid) ?: throw NotFoundException()
    }

    @PostMapping("guesstimate/suggestion/{uuid}")
    fun modifySuggestion(
        @RequestHeader("auth", required = false, defaultValue = "") authToken: String,
        @PathVariable("uuid") uuid: String,
        @RequestBody modifiedSuggestion: Suggestion
    ): Suggestion {
        // Needs work suggestions may be modified by everyone
        if (database.getSuggestion(uuid)?.state == SuggestionState.NEEDS_WORK) {
            val toRet = database.updateSuggestion(uuid, modifiedSuggestion) ?: throw NotFoundException()
            database.setState(uuid, SuggestionState.UPDATED)
            return toRet
        }

        return checkForRights(authToken, "update suggestion $uuid", UserRight.SUGGESTION_UPDATE) {
            logger().debug("Updating suggestion $uuid to $modifiedSuggestion")
            return@checkForRights database.updateSuggestion(uuid, modifiedSuggestion) ?: throw NotFoundException()
        }
    }

    @DeleteMapping("guesstimate/suggestion/{uuid}")
    fun deleteSuggestion(
        @RequestHeader("auth", required = false, defaultValue = "") authToken: String,
        @PathVariable("uuid") uuid: String
    ): Boolean {
        return checkForRights(authToken, "delete suggestion $uuid", UserRight.SUGGESTION_DELETE) {
            database.deleteSuggestion(uuid)
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
