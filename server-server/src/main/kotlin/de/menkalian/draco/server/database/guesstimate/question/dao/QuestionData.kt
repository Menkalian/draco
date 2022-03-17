package de.menkalian.draco.server.database.guesstimate.question.dao

import de.menkalian.draco.data.quesstimate.GuesstimateQuestion
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object QuestionData : IntIdTable() {
    val author = varchar("author", 255)
    val createdAt = long("createdAt")

    val language = reference("language", LanguageData.id)
    val difficulty = reference("difficulty", DifficultyData.id)
    val category = reference("category", CategoryData.id)

    val question = text("question")
    val answer = double("answer")
    val answerUnit = text("answerUnit")

    class QuestionDataEntry(id: EntityID<Int>) : IntEntity(id) {
        companion object : IntEntityClass<QuestionDataEntry>(QuestionData)

        var author by QuestionData.author
        var createdAt by QuestionData.createdAt

        var language by QuestionData.language
        var difficulty by QuestionData.difficulty
        var category by QuestionData.category

        var question by QuestionData.question
        var answer by QuestionData.answer
        var answerUnit by QuestionData.answerUnit

        val hints by HintData.HintDataEntry referrersOn HintData.question

        fun toQuestionObject() : GuesstimateQuestion {
            return GuesstimateQuestion(
                id.value,
                author,
                createdAt,
                LanguageData.LanguageDataEntry[language].toEnum(),
                DifficultyData.DifficultyDataEntry[difficulty].toEnum(),
                CategoryData.CategoryDataEntry[category].toEnum(),
                question,
                answer,
                answerUnit,
                hints.map { it.toHintString() }.toList()
            )
        }
    }
}