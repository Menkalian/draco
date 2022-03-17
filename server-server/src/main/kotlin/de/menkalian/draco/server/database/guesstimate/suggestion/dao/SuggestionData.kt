package de.menkalian.draco.server.database.guesstimate.suggestion.dao

import de.menkalian.draco.data.quesstimate.Suggestion
import de.menkalian.draco.server.database.guesstimate.question.dao.QuestionData
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.UUID

object SuggestionData : UUIDTable() {
    val suggestedQuestion = reference("question", QuestionData.id)
    val state = reference("state", SuggestionStateData.id)

    class SuggestionDataEntry(id: EntityID<UUID>) : UUIDEntity(id) {
        companion object : UUIDEntityClass<SuggestionDataEntry>(SuggestionData)

        var suggestedQuestion by SuggestionData.suggestedQuestion
        var state by SuggestionData.state

        val notes by SuggestionCommentData.SuggestionCommentDataEntry referrersOn SuggestionCommentData.suggestion

        fun toSuggestionObject() : Suggestion {
            return Suggestion(
                id.value.toString(),
                QuestionData.QuestionDataEntry[suggestedQuestion].toQuestionObject(),
                SuggestionStateData.SuggestionStateDataEntry[state].toEnum(),
                notes.map { it.toSuggestionCommentObject() }
            )
        }
    }
}