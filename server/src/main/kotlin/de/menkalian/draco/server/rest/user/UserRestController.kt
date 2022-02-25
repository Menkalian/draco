package de.menkalian.draco.server.rest.user

import de.menkalian.draco.data.user.EntitledUser
import de.menkalian.draco.data.user.UserRight
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
class UserRestController(val userDatabase: IUserDatabase) {
    @PutMapping("user")
    fun createUser(
        @RequestHeader("auth", required = false, defaultValue = "") authToken: String,
        @RequestBody user: EntitledUser
    ): EntitledUser {
        return checkForRights(authToken, "create user", UserRight.USER_CREATE) {
            logger().debug("Creating user ${user.name} with rights ${user.rights}")

            val newUser = userDatabase.createUser(user.name)

            user.rights.forEach { right ->
                checkForRights(authToken, "add right $right", right) {
                    userDatabase.addRight(newUser.id, right)
                }
            }

            return@checkForRights userDatabase.getUser(newUser.id)!!
        }
    }

    @GetMapping("user/all")
    fun getUsers(
        @RequestHeader("auth", required = false, defaultValue = "") authToken: String,
    ): List<EntitledUser> {
        return checkForRights(authToken, "get all users", UserRight.USER_READ) {
            userDatabase.getUsers()
        }
    }

    @GetMapping("user/{id}")
    fun getUser(
        @RequestHeader("auth", required = false, defaultValue = "") authToken: String,
        @PathVariable("id") id: Int
    ): EntitledUser {
        return checkForRights(authToken, "get user $id", UserRight.USER_READ) {
            userDatabase.getUser(id) ?: throw NotFoundException()
        }
    }

    @GetMapping("user/me")
    fun getMyUser(
        @RequestHeader("auth") authToken: String
    ): EntitledUser {
        return userDatabase.getUser(authToken.authTokenHash()) ?: throw MissingAuthenticationException()
    }

    @PostMapping("user/{id}")
    fun modifyUser(
        @RequestHeader("auth", required = false, defaultValue = "") authToken: String,
        @PathVariable("id") id: Int,
        @RequestBody modifiedUser: EntitledUser
    ): EntitledUser {
        return checkForRights(authToken, "update user $id", *(modifiedUser.rights + UserRight.USER_CREATE).toTypedArray()) {
            logger().debug("Updating user $id with new name ${modifiedUser.name} and rights ${modifiedUser.rights}")
            return@checkForRights userDatabase.updateUser(id, modifiedUser) ?: throw NotFoundException()
        }
    }

    @DeleteMapping("user/{id}")
    fun deleteUser(
        @RequestHeader("auth", required = false, defaultValue = "") authToken: String,
        @PathVariable("id") id: Int
    ): Boolean {
        return checkForRights(authToken, "delete user $id", UserRight.USER_DELETE) {
            userDatabase.deleteUser(id)
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