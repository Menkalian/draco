package de.menkalian.draco.data.quesstimate

import kotlinx.serialization.Serializable

@Serializable
data class GuesstimateQuestion(
    val id: Int,

    val author: String,
    val createdAt: Long,

    val language: Language,
    val difficulty: Difficulty,
    val category: Category,

    val question: String,
    val answer: Double,
    val answerUnit: String,
    val hints: List<String>
)
