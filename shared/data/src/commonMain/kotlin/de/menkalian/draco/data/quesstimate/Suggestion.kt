package de.menkalian.draco.data.quesstimate

import kotlinx.serialization.Serializable

@Serializable
data class Suggestion(
    val uuid: String,
    val suggestedQuestion: GuesstimateQuestion,
    val state: SuggestionState,
    val notes: List<SuggestionComment>
) {
    @Serializable
    data class SuggestionComment(
        val author: String,
        val comment: String,
        val timestamp: Long
    )
}
