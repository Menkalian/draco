package de.menkalian.draco.data.user

import kotlinx.serialization.Serializable

@Serializable
enum class UserRight {
    ADMIN,

    USER_READ,
    USER_CREATE,
    USER_UPDATE,
    USER_DELETE,

    SUGGESTION_READ,

    // SUGGESTION_CREATE is given to all anonymous users
    SUGGESTION_UPDATE,
    SUGGESTION_DELETE,
    SUGGESTION_COMMENT_CREATE,

    QUESTION_READ,
    QUESTION_CREATE,
    QUESTION_UPDATE,
    QUESTION_DELETE;
}