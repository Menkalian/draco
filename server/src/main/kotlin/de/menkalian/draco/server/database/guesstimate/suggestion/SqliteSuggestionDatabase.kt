package de.menkalian.draco.server.database.guesstimate.suggestion

import de.menkalian.draco.data.quesstimate.Category
import de.menkalian.draco.data.quesstimate.Difficulty
import de.menkalian.draco.data.quesstimate.Language
import de.menkalian.draco.data.quesstimate.Suggestion
import de.menkalian.draco.data.quesstimate.SuggestionState
import de.menkalian.draco.server.database.guesstimate.question.dao.CategoryData
import de.menkalian.draco.server.database.guesstimate.question.dao.CategoryData.CategoryDataEntry.Companion.findDao
import de.menkalian.draco.server.database.guesstimate.question.dao.DifficultyData
import de.menkalian.draco.server.database.guesstimate.question.dao.DifficultyData.DifficultyDataEntry.Companion.findDao
import de.menkalian.draco.server.database.guesstimate.question.dao.HintData
import de.menkalian.draco.server.database.guesstimate.question.dao.LanguageData
import de.menkalian.draco.server.database.guesstimate.question.dao.LanguageData.LanguageDataEntry.Companion.findDao
import de.menkalian.draco.server.database.guesstimate.question.dao.QuestionData
import de.menkalian.draco.server.database.guesstimate.suggestion.dao.SuggestionCommentData
import de.menkalian.draco.server.database.guesstimate.suggestion.dao.SuggestionData
import de.menkalian.draco.server.database.guesstimate.suggestion.dao.SuggestionStateData
import de.menkalian.draco.server.database.guesstimate.suggestion.dao.SuggestionStateData.SuggestionStateDataEntry.Companion.findDao
import de.menkalian.draco.server.database.shared.MetaDataAwareDatabaseExtension
import de.menkalian.draco.server.util.initEnumDatabase
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.info.BuildProperties
import org.springframework.stereotype.Component
import java.io.File
import java.util.UUID

@Component
@Suppress("LeakingThis")
class SqliteSuggestionDatabase(
    @Value("\${draco.sqlite.database.path}") private val databaseFolderPath: String,
    build: BuildProperties
) : ISuggestionDatabase {
    companion object {
        private const val DATABASE_SCHEMA_VERSION = 1
    }

    override var isOpen: Boolean = false
    override val dbConnection: Database

    private val metadataExtension = MetaDataAwareDatabaseExtension()

    init {
        File(databaseFolderPath).mkdirs()

        dbConnection = Database.connect("jdbc:sqlite:$databaseFolderPath/suggestions.db3", driver = "org.sqlite.JDBC")
        isOpen = true

        metadataExtension.initMetadata(this, build, DATABASE_SCHEMA_VERSION, "Suggestion")
        initEnums()
    }

    private fun initEnums() {
        ensureOpen()
        initEnumDatabase(dbConnection, CategoryData, Category.values().map { it.name })
        initEnumDatabase(dbConnection, DifficultyData, Difficulty.values().map { it.name })
        initEnumDatabase(dbConnection, LanguageData, Language.values().map { it.name })
        initEnumDatabase(dbConnection, SuggestionStateData, SuggestionState.values().map { it.name })
    }

    override fun createSuggestion(suggestion: Suggestion): Suggestion {
        ensureOpen()
        return transaction(dbConnection) {
            createAllTables()

            val daoQuestion = QuestionData.QuestionDataEntry
                .new {
                    this.author = suggestion.suggestedQuestion.author
                    this.createdAt = suggestion.suggestedQuestion.createdAt
                    this.language = suggestion.suggestedQuestion.language.findDao().id
                    this.difficulty = suggestion.suggestedQuestion.difficulty.findDao().id
                    this.category = suggestion.suggestedQuestion.category.findDao().id
                    this.question = suggestion.suggestedQuestion.question
                    this.answer = suggestion.suggestedQuestion.answer
                }

            val daoSuggestion = SuggestionData.SuggestionDataEntry
                .new {
                    this.state = suggestion.state.findDao().id
                    this.suggestedQuestion = daoQuestion.id
                }

            suggestion.notes.forEach {
                addNote(daoSuggestion.id.value.toString(), it)
            }

            daoSuggestion.toSuggestionObject()
        }
    }

    override fun getAllSuggestions(): List<Suggestion> {
        ensureOpen()
        return transaction(dbConnection) {
            createAllTables()

            SuggestionData.SuggestionDataEntry
                .all()
                .map { it.toSuggestionObject() }
        }
    }

    override fun getSuggestion(uuid: String): Suggestion? {
        ensureOpen()
        return transaction(dbConnection) {
            createAllTables()

            SuggestionData.SuggestionDataEntry
                .findById(UUID.fromString(uuid))
                ?.toSuggestionObject()
        }
    }

    override fun getUnreadSuggestion(): Suggestion? {
        ensureOpen()
        return transaction(dbConnection) {
            createAllTables()

            val unreadStates = listOf(
                SuggestionState.CREATED,
                SuggestionState.UPDATED
            ).map { it.findDao().id }

            SuggestionData.SuggestionDataEntry
                .find(SuggestionData.state.inList(unreadStates))
                .firstOrNull()
                ?.toSuggestionObject()
        }
    }

    override fun updateSuggestion(uuid: String, updated: Suggestion): Suggestion? {
        ensureOpen()

        var success = true
        success = success && setState(uuid, updated.state) != null
        success = success && setAuthor(uuid, updated.suggestedQuestion.author) != null
        success = success && setCreatedAt(uuid, updated.suggestedQuestion.createdAt) != null
        success = success && setLanguage(uuid, updated.suggestedQuestion.language) != null
        success = success && setDifficulty(uuid, updated.suggestedQuestion.difficulty) != null
        success = success && setCategory(uuid, updated.suggestedQuestion.category) != null
        success = success && setQuestion(uuid, updated.suggestedQuestion.question) != null
        success = success && setAnswer(uuid, updated.suggestedQuestion.answer) != null

        success = success && clearNotes(uuid) != null
        updated.notes.forEach {
            success = success && addNote(uuid, it) != null
        }

        success = success && clearHints(uuid) != null
        updated.suggestedQuestion.hints.forEach {
            success = success && addHint(uuid, it) != null
        }

        return getSuggestion(uuid)
    }

    override fun setState(uuid: String, state: SuggestionState): Suggestion? {
        ensureOpen()
        return transaction(dbConnection) {
            createAllTables()

            SuggestionData.SuggestionDataEntry
                .findById(UUID.fromString(uuid))
                ?.apply { this.state = state.findDao().id }
                ?.toSuggestionObject()
        }
    }

    override fun clearNotes(uuid: String): Suggestion? {
        ensureOpen()
        transaction(dbConnection) {
            createAllTables()

            val suggestion = SuggestionData.SuggestionDataEntry
                .findById(UUID.fromString(uuid))

            suggestion?.notes?.forEach {
                it.delete()
            }
        }
        return getSuggestion(uuid)
    }

    override fun addNote(uuid: String, comment: Suggestion.SuggestionComment): Suggestion? {
        ensureOpen()
        transaction(dbConnection) {
            createAllTables()

            val suggestion = SuggestionData.SuggestionDataEntry
                .findById(UUID.fromString(uuid))

            if (suggestion != null) {
                SuggestionCommentData.SuggestionCommentDataEntry.new {
                    this.suggestion = suggestion.id
                    this.author = comment.author
                    this.timestamp = comment.timestamp
                    this.comment = comment.comment
                }
            }
        }
        return getSuggestion(uuid)
    }

    override fun setAuthor(uuid: String, author: String): Suggestion? {
        ensureOpen()
        transaction(dbConnection) {
            createAllTables()
            val questionId = SuggestionData.SuggestionDataEntry
                .findById(UUID.fromString(uuid))
                ?.suggestedQuestion

            if (questionId != null) {
                QuestionData.QuestionDataEntry
                    .findById(questionId)
                    ?.apply { this.author = author }
                    ?.toQuestionObject()
            }
        }
        return getSuggestion(uuid)
    }

    override fun setCreatedAt(uuid: String, timestamp: Long): Suggestion? {
        ensureOpen()
        transaction(dbConnection) {
            createAllTables()
            val questionId = SuggestionData.SuggestionDataEntry
                .findById(UUID.fromString(uuid))
                ?.suggestedQuestion

            if (questionId != null) {
                QuestionData.QuestionDataEntry
                    .findById(questionId)
                    ?.apply { this.createdAt = createdAt }
                    ?.toQuestionObject()
            }
        }
        return getSuggestion(uuid)
    }

    override fun setLanguage(uuid: String, language: Language): Suggestion? {
        ensureOpen()
        transaction(dbConnection) {
            createAllTables()
            val questionId = SuggestionData.SuggestionDataEntry
                .findById(UUID.fromString(uuid))
                ?.suggestedQuestion

            if (questionId != null) {
                QuestionData.QuestionDataEntry
                    .findById(questionId)
                    ?.apply { this.language = language.findDao().id }
                    ?.toQuestionObject()
            }
        }
        return getSuggestion(uuid)
    }

    override fun setDifficulty(uuid: String, difficulty: Difficulty): Suggestion? {
        ensureOpen()
        transaction(dbConnection) {
            createAllTables()
            val questionId = SuggestionData.SuggestionDataEntry
                .findById(UUID.fromString(uuid))
                ?.suggestedQuestion

            if (questionId != null) {
                QuestionData.QuestionDataEntry
                    .findById(questionId)
                    ?.apply { this.difficulty = difficulty.findDao().id }
                    ?.toQuestionObject()
            }
        }
        return getSuggestion(uuid)
    }

    override fun setCategory(uuid: String, category: Category): Suggestion? {
        ensureOpen()
        transaction(dbConnection) {
            createAllTables()
            val questionId = SuggestionData.SuggestionDataEntry
                .findById(UUID.fromString(uuid))
                ?.suggestedQuestion

            if (questionId != null) {
                QuestionData.QuestionDataEntry
                    .findById(questionId)
                    ?.apply { this.category = category.findDao().id }
                    ?.toQuestionObject()
            }
        }
        return getSuggestion(uuid)
    }

    override fun setQuestion(uuid: String, question: String): Suggestion? {
        ensureOpen()
        transaction(dbConnection) {
            createAllTables()
            val questionId = SuggestionData.SuggestionDataEntry
                .findById(UUID.fromString(uuid))
                ?.suggestedQuestion

            if (questionId != null) {
                QuestionData.QuestionDataEntry
                    .findById(questionId)
                    ?.apply { this.question = question }
                    ?.toQuestionObject()
            }
        }
        return getSuggestion(uuid)
    }

    override fun setAnswer(uuid: String, answer: Long): Suggestion? {
        ensureOpen()
        transaction(dbConnection) {
            createAllTables()
            val questionId = SuggestionData.SuggestionDataEntry
                .findById(UUID.fromString(uuid))
                ?.suggestedQuestion

            if (questionId != null) {
                QuestionData.QuestionDataEntry
                    .findById(questionId)
                    ?.apply { this.answer = answer }
                    ?.toQuestionObject()
            }
        }
        return getSuggestion(uuid)
    }

    override fun clearHints(uuid: String): Suggestion? {
        ensureOpen()
        transaction(dbConnection) {
            createAllTables()

            val questionId = SuggestionData.SuggestionDataEntry
                .findById(UUID.fromString(uuid))
                ?.suggestedQuestion

            if (questionId != null) {
                val question = QuestionData.QuestionDataEntry
                    .findById(questionId)

                question?.hints?.forEach {
                    it.delete()
                }
            }
        }
        return getSuggestion(uuid)
    }

    override fun addHint(uuid: String, hint: String): Suggestion? {
        ensureOpen()
        transaction(dbConnection) {
            createAllTables()
            val questionId = SuggestionData.SuggestionDataEntry
                .findById(UUID.fromString(uuid))
                ?.suggestedQuestion

            if (questionId != null) {
                HintData.HintDataEntry
                    .new {
                        this.question = QuestionData.QuestionDataEntry[questionId]
                        this.text = hint
                    }
            }
        }

        return getSuggestion(uuid)
    }

    override fun deleteSuggestion(uuid: String): Boolean {
        ensureOpen()
        return transaction(dbConnection) {
            createAllTables()

            val suggestion = SuggestionData.SuggestionDataEntry
                .findById(UUID.fromString(uuid))

            if (suggestion != null) {
                clearNotes(uuid)
                clearHints(uuid)
                QuestionData.QuestionDataEntry[suggestion.suggestedQuestion].delete()
                suggestion.delete()
                true
            } else {
                false
            }
        }
    }

    private fun createAllTables() {
        SchemaUtils.create(
            CategoryData, DifficultyData, HintData, LanguageData, QuestionData, SuggestionStateData, SuggestionCommentData, SuggestionData
        )
    }

    override fun close() {
        isOpen = false
        TransactionManager.closeAndUnregister(dbConnection)
    }
}