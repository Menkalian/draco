package de.menkalian.draco.server.util

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.UNAUTHORIZED)
class MissingAuthenticationException : RuntimeException()

@ResponseStatus(HttpStatus.UNAUTHORIZED)
class UserRightsNotSufficientException : RuntimeException()

@ResponseStatus(HttpStatus.NOT_FOUND)
class NotFoundException : RuntimeException()