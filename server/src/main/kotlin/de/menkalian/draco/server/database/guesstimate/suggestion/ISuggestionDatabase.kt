package de.menkalian.draco.server.database.guesstimate.suggestion

import de.menkalian.draco.data.quesstimate.Category
import de.menkalian.draco.data.quesstimate.Difficulty
import de.menkalian.draco.data.quesstimate.Language
import de.menkalian.draco.data.quesstimate.Suggestion
import de.menkalian.draco.data.quesstimate.SuggestionState
import de.menkalian.draco.server.database.IDatabase

interface ISuggestionDatabase : IDatabase {
    // CREATE
    fun createSuggestion(suggestion: Suggestion): Suggestion

    // READ
    fun getAllSuggestions(): List<Suggestion>
    fun getSuggestion(uuid: String): Suggestion?
    fun getUnreadSuggestion(): Suggestion?

    // UPDATE
    fun updateSuggestion(uuid: String, updated: Suggestion): Suggestion?
    fun setState(uuid: String, state: SuggestionState): Suggestion?
    fun clearNotes(uuid: String): Suggestion?
    fun addNote(uuid: String, comment: Suggestion.SuggestionComment): Suggestion?

    fun setAuthor(uuid: String, author: String): Suggestion?
    fun setCreatedAt(uuid: String, timestamp: Long): Suggestion?
    fun setLanguage(uuid: String, language: Language): Suggestion?
    fun setDifficulty(uuid: String, difficulty: Difficulty): Suggestion?
    fun setCategory(uuid: String, category: Category): Suggestion?
    fun setQuestion(uuid: String, question: String): Suggestion?
    fun setAnswer(uuid: String, answer: Long): Suggestion?
    fun clearHints(uuid: String): Suggestion?
    fun addHint(uuid: String, hint: String): Suggestion?

    // DELETE
    fun deleteSuggestion(uuid: String): Boolean

}