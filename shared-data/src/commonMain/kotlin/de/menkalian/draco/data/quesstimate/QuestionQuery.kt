package de.menkalian.draco.data.quesstimate

import kotlinx.serialization.Serializable

@Serializable
data class QuestionQuery(
    var amount: Int = 10,
    val languages: List<Language> = listOf(),
    val categories: List<Category> = listOf(),
    val difficulties: List<Difficulty> = listOf()
) {
    init {
        amount = amount.coerceIn(0..50)
    }
}