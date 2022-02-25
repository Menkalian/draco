package de.menkalian.draco.server.database.guesstimate.question

import de.menkalian.draco.data.quesstimate.Category
import de.menkalian.draco.data.quesstimate.Difficulty
import de.menkalian.draco.data.quesstimate.GuesstimateQuestion
import de.menkalian.draco.data.quesstimate.Language
import de.menkalian.draco.data.quesstimate.QuestionQuery
import de.menkalian.draco.server.database.IDatabase

interface IQuestionDatabase : IDatabase {
    // CREATE
    fun createQuestion(question: GuesstimateQuestion): GuesstimateQuestion

    // READ
    fun getAllQuestions(): List<GuesstimateQuestion>
    fun getQuestion(id: Int): GuesstimateQuestion?
    fun queryQuestions(filter: QuestionQuery): List<GuesstimateQuestion>

    // UPDATE
    fun updateQuestion(id: Int, updated: GuesstimateQuestion): GuesstimateQuestion?
    fun setAuthor(id: Int, author: String): GuesstimateQuestion?
    fun setCreatedAt(id: Int, timestamp: Long): GuesstimateQuestion?
    fun setLanguage(id: Int, language: Language): GuesstimateQuestion?
    fun setDifficulty(id: Int, difficulty: Difficulty): GuesstimateQuestion?
    fun setCategory(id: Int, category: Category): GuesstimateQuestion?
    fun setQuestion(id: Int, question: String): GuesstimateQuestion?
    fun setAnswer(id: Int, answer: Long): GuesstimateQuestion?
    fun clearHints(id: Int): GuesstimateQuestion?
    fun addHint(id: Int, hint: String): GuesstimateQuestion?

    // DELETE
    fun deleteQuestion(id: Int): Boolean
}