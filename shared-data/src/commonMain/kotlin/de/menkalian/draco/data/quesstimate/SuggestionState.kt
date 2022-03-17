package de.menkalian.draco.data.quesstimate

import kotlinx.serialization.Serializable

@Serializable
enum class SuggestionState {
    CREATED,
    NEEDS_WORK,
    UPDATED,
    CLOSED,
    ACCEPTED
}