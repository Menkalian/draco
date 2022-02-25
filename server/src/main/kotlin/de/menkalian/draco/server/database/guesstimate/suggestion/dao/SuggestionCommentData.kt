package de.menkalian.draco.server.database.guesstimate.suggestion.dao

import de.menkalian.draco.data.quesstimate.Suggestion
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object SuggestionCommentData : IntIdTable() {
    val author = varchar("author", 255)
    val comment = text("comment")
    val timestamp = long("timestamp")

    val suggestion = reference("suggestion", SuggestionData.id)

    class SuggestionCommentDataEntry(id: EntityID<Int>) : IntEntity(id) {
        companion object : IntEntityClass<SuggestionCommentDataEntry>(SuggestionCommentData)

        var author by SuggestionCommentData.author
        var comment by SuggestionCommentData.comment
        var timestamp by SuggestionCommentData.timestamp

        var suggestion by SuggestionCommentData.suggestion

        fun toSuggestionCommentObject() : Suggestion.SuggestionComment {
            return Suggestion.SuggestionComment(
                author, comment, timestamp
            )
        }
    }
}