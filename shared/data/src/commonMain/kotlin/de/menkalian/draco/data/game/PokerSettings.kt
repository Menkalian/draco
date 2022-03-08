package de.menkalian.draco.data.game

import de.menkalian.draco.data.Draco
import de.menkalian.draco.data.game.enums.AnswerRevealStrategy
import de.menkalian.draco.data.game.enums.BlindRaiseStrategy
import de.menkalian.draco.data.game.enums.LateJoinBehaviour
import de.menkalian.draco.data.game.enums.LobbyPublicity
import de.menkalian.draco.data.game.enums.TimeoutStrategy
import de.menkalian.draco.data.game.values.TransferableValue
import de.menkalian.draco.data.game.values.ValueHolder
import de.menkalian.draco.data.quesstimate.Category
import de.menkalian.draco.data.quesstimate.Difficulty
import de.menkalian.draco.data.quesstimate.Language

@kotlinx.serialization.Serializable
class PokerSettings : ValueHolder() {
    var lobbyName: String = "_"
        set(value) {
            field = value
            notifyChange(Draco.Game.Poker.Settings.Lobby.Name, TransferableValue.from(field))
        }

    var publicity: LobbyPublicity = LobbyPublicity.CODE_ONLY
        set(value) {
            field = value
            notifyChange(Draco.Game.Poker.Settings.Lobby.Publicity, TransferableValue.from(field.name))
        }

    var defaultStartScore: Long = 5000
        set(value) {
            field = value
            notifyChange(Draco.Game.Poker.Settings.DefaultPoints, TransferableValue.from(field))
        }

    var maxQuestionCount: Long = -1
        set(value) {
            field = value
            notifyChange(Draco.Game.Poker.Settings.MaxQuestions, TransferableValue.from(field))
        }

    var timeoutMs: Long = 60_000
        set(value) {
            field = value
            notifyChange(Draco.Game.Poker.Settings.Timeout, TransferableValue.from(field))
        }

    var kickWhenBroke: Boolean = false
        set(value) {
            field = value
            notifyChange(Draco.Game.Poker.Settings.KickBroke, TransferableValue.from(field))
        }

    var showHelpWarnings: Boolean = false
        set(value) {
            field = value
            notifyChange(Draco.Game.Poker.Settings.ShowHelpWarnings, TransferableValue.from(field))
        }

    var allowLateJoin: Boolean = false
        set(value) {
            field = value
            notifyChange(Draco.Game.Poker.Settings.LateJoin, TransferableValue.from(field))
        }

    var blindLevels: List<Pair<Long, Long>> = listOf(
        50L to 100L,
        100L to 200L,
        200L to 400L,
        500L to 1000L
    )
        set(value) {
            field = value

            val values = mutableMapOf<String, TransferableValue>()
            values[Draco.Game.Poker.Settings.Blinds.n] = TransferableValue.from(value.size)
            value.forEachIndexed { idx, bds ->
                values[Draco.Game.Poker.Settings.Blinds.XXX(idx + 1).Small] = TransferableValue.from(bds.first)
                values[Draco.Game.Poker.Settings.Blinds.XXX(idx + 1).Big] = TransferableValue.from(bds.second)
            }

            notifyChanges(values)
        }

    var blindRaiseStrategy: BlindRaiseStrategy = BlindRaiseStrategy.ON_ROUNDED
        set(value) {
            field = value
            notifyChange(Draco.Game.Poker.Settings.BlindStrategy, TransferableValue.from(field.name))
        }

    var answerRevealStrategy: AnswerRevealStrategy = AnswerRevealStrategy.NEVER
        set(value) {
            field = value
            notifyChange(Draco.Game.Poker.Settings.RevealStrategy, TransferableValue.from(field.name))
        }

    var timeoutStrategy: TimeoutStrategy = TimeoutStrategy.AUTO_FOLD
        set(value) {
            field = value
            notifyChange(Draco.Game.Poker.Settings.TimeoutStrategy, TransferableValue.from(field.name))
        }

    var lateJoinBehaviour: LateJoinBehaviour = LateJoinBehaviour.MEDIAN_SCORE
        set(value) {
            field = value
            notifyChange(Draco.Game.Poker.Settings.LateJoinStrategy, TransferableValue.from(field.name))
        }

    private val allowedCategoriesIntern = mutableListOf<Category>()
    val allowedCategories = allowedCategoriesIntern.toList()

    fun addCategory(category: Category) {
        allowedCategoriesIntern.add(category)
        notifyCategories()
    }

    fun removeCategory(category: Category) {
        allowedCategoriesIntern.remove(category)
        notifyCategories()
    }

    fun clearCategories() {
        allowedCategoriesIntern.clear()
        notifyCategories()
    }

    private fun notifyCategories() {
        val values = mutableMapOf<String, TransferableValue>()
        values[Draco.Game.Poker.Settings.Categories.n] = TransferableValue.from(allowedCategoriesIntern.size)
        allowedCategoriesIntern.forEachIndexed { idx, category ->
            values[Draco.Game.Poker.Settings.Categories.XXX(idx + 1).Name] = TransferableValue.from(category.name)
        }
        notifyChanges(values)
    }

    private val allowedDifficultiesIntern = mutableListOf<Difficulty>()
    val allowedDifficulties = allowedDifficultiesIntern.toList()

    fun addDifficulty(difficulty: Difficulty) {
        allowedDifficultiesIntern.add(difficulty)
        notifyDifficulties()
    }

    fun removeDifficulty(difficulty: Difficulty) {
        allowedDifficultiesIntern.remove(difficulty)
        notifyDifficulties()
    }

    fun clearDifficulties() {
        allowedDifficultiesIntern.clear()
        notifyDifficulties()
    }

    private fun notifyDifficulties() {
        val values = mutableMapOf<String, TransferableValue>()
        values[Draco.Game.Poker.Settings.Difficulties.n] = TransferableValue.from(allowedDifficultiesIntern.size)
        allowedDifficultiesIntern.forEachIndexed { idx, difficulty ->
            values[Draco.Game.Poker.Settings.Difficulties.XXX(idx + 1).Name] = TransferableValue.from(difficulty.name)
        }
        notifyChanges(values)
    }

    private val allowedLanguagesIntern = mutableListOf<Language>()
    val allowedLanguages = allowedLanguagesIntern.toList()

    fun addLanguage(category: Language) {
        allowedLanguagesIntern.add(category)
        notifyLanguages()
    }

    fun removeLanguage(category: Language) {
        allowedLanguagesIntern.remove(category)
        notifyLanguages()
    }

    fun clearLanguages() {
        allowedLanguagesIntern.clear()
        notifyLanguages()
    }

    private fun notifyLanguages() {
        val values = mutableMapOf<String, TransferableValue>()
        values[Draco.Game.Poker.Settings.Languages.n] = TransferableValue.from(allowedLanguagesIntern.size)
        allowedLanguagesIntern.forEachIndexed { idx, lang ->
            values[Draco.Game.Poker.Settings.Languages.XXX(idx + 1).Name] = TransferableValue.from(lang.name)
        }
        notifyChanges(values)
    }
}