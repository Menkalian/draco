package de.menkalian.draco.server.database.guesstimate.question.dao

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object HintData : IntIdTable() {
    val text = text("text")
    val question = reference("question", QuestionData.id)

    class HintDataEntry(id: EntityID<Int>) : IntEntity(id) {
        companion object : IntEntityClass<HintDataEntry>(HintData)

        var text by HintData.text
        var question by QuestionData.QuestionDataEntry referencedOn HintData.question

        fun toHintString() : String {
            return text
        }
    }
}