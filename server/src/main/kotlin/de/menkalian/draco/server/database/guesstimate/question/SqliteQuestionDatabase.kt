package de.menkalian.draco.server.database.guesstimate.question

import de.menkalian.draco.data.quesstimate.Category
import de.menkalian.draco.data.quesstimate.Difficulty
import de.menkalian.draco.data.quesstimate.GuesstimateQuestion
import de.menkalian.draco.data.quesstimate.Language
import de.menkalian.draco.data.quesstimate.QuestionQuery
import de.menkalian.draco.server.database.guesstimate.question.dao.CategoryData
import de.menkalian.draco.server.database.guesstimate.question.dao.CategoryData.CategoryDataEntry.Companion.findDao
import de.menkalian.draco.server.database.guesstimate.question.dao.DifficultyData
import de.menkalian.draco.server.database.guesstimate.question.dao.DifficultyData.DifficultyDataEntry.Companion.findDao
import de.menkalian.draco.server.database.guesstimate.question.dao.HintData
import de.menkalian.draco.server.database.guesstimate.question.dao.LanguageData
import de.menkalian.draco.server.database.guesstimate.question.dao.LanguageData.LanguageDataEntry.Companion.findDao
import de.menkalian.draco.server.database.guesstimate.question.dao.QuestionData
import de.menkalian.draco.server.database.shared.MetaDataAwareDatabaseExtension
import de.menkalian.draco.server.util.initEnumDatabase
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.info.BuildProperties
import org.springframework.stereotype.Component
import java.io.File


@Component
@Suppress("LeakingThis")
class SqliteQuestionDatabase(
    @Value("\${draco.sqlite.database.path}") private val databaseFolderPath: String,
    build: BuildProperties
) : IQuestionDatabase {
    companion object {
        private const val DATABASE_SCHEMA_VERSION = 1
    }

    override var isOpen: Boolean = false
    override val dbConnection: Database

    private val metadataExtension = MetaDataAwareDatabaseExtension()

    init {
        File(databaseFolderPath).mkdirs()

        dbConnection = Database.connect("jdbc:sqlite:$databaseFolderPath/questions.db3", driver = "org.sqlite.JDBC")
        isOpen = true

        metadataExtension.initMetadata(this, build, DATABASE_SCHEMA_VERSION, "Questions")
        initEnums()
    }

    private fun initEnums() {
        ensureOpen()
        initEnumDatabase(dbConnection, CategoryData, Category.values().map { it.name })
        initEnumDatabase(dbConnection, DifficultyData, Difficulty.values().map { it.name })
        initEnumDatabase(dbConnection, LanguageData, Language.values().map { it.name })
    }

    override fun createQuestion(question: GuesstimateQuestion): GuesstimateQuestion {
        ensureOpen()
        return transaction(dbConnection) {
            createAllTables()

            val daoQuestion = QuestionData.QuestionDataEntry
                .new {
                    this.author = question.author
                    this.createdAt = question.createdAt
                    this.language = question.language.findDao().id
                    this.difficulty = question.difficulty.findDao().id
                    this.category = question.category.findDao().id
                    this.question = question.question
                    this.answer = question.answer
                }

            question.hints.forEach {
                HintData.HintDataEntry
                    .new {
                        this.question = daoQuestion
                        this.text = it
                    }
            }

            daoQuestion.toQuestionObject()
        }
    }

    override fun getAllQuestions(): List<GuesstimateQuestion> {
        ensureOpen()
        return transaction(dbConnection) {
            createAllTables()

            QuestionData.QuestionDataEntry.all()
                .map { it.toQuestionObject() }
                .toList()
        }
    }

    override fun getQuestion(id: Int): GuesstimateQuestion? {
        ensureOpen()
        return transaction(dbConnection) {
            createAllTables()

            QuestionData.QuestionDataEntry
                .findById(id)
                ?.toQuestionObject()
        }
    }

    override fun queryQuestions(filter: QuestionQuery): List<GuesstimateQuestion> {
        ensureOpen()
        return transaction(dbConnection) {
            createAllTables()

            // Create filter parameters
            val languageList = if (filter.languages.isEmpty()) {
                Language.values().map { it.findDao().id }
            } else {
                filter.languages.map { it.findDao().id }
            }
            val categoryList = if (filter.categories.isEmpty()) {
                Category.values().map { it.findDao().id }
            } else {
                filter.categories.map { it.findDao().id }
            }
            val difficultyList = if (filter.difficulties.isEmpty()) {
                Difficulty.values().map { it.findDao().id }
            } else {
                filter.categories.map { it.findDao().id }
            }

            QuestionData.QuestionDataEntry
                .find {
                    QuestionData.language.inList(languageList)
                        .and(QuestionData.category.inList(categoryList))
                        .and(QuestionData.difficulty.inList(difficultyList))
                }
                .map { it.toQuestionObject() }
                .shuffled()
                .take(filter.amount)
        }
    }

    override fun updateQuestion(id: Int, updated: GuesstimateQuestion): GuesstimateQuestion? {
        ensureOpen()
        var success = true
        success = success && setAuthor(id, updated.author) != null
        success = success && setCreatedAt(id, updated.createdAt) != null
        success = success && setQuestion(id, updated.question) != null
        success = success && setAnswer(id, updated.answer) != null
        success = success && setLanguage(id, updated.language) != null
        success = success && setCategory(id, updated.category) != null
        success = success && setDifficulty(id, updated.difficulty) != null
        success = success && clearHints(id) != null
        updated.hints.forEach {
            success = success && setAuthor(id, it) != null
        }

        return getQuestion(id)
    }

    override fun setAuthor(id: Int, author: String): GuesstimateQuestion? {
        ensureOpen()
        return transaction(dbConnection) {
            createAllTables()

            QuestionData.QuestionDataEntry
                .findById(id)
                ?.apply { this.author = author }
                ?.toQuestionObject()
        }
    }

    override fun setCreatedAt(id: Int, timestamp: Long): GuesstimateQuestion? {
        ensureOpen()
        return transaction(dbConnection) {
            createAllTables()

            QuestionData.QuestionDataEntry
                .findById(id)
                ?.apply { this.createdAt = createdAt }
                ?.toQuestionObject()
        }
    }

    override fun setLanguage(id: Int, language: Language): GuesstimateQuestion? {
        ensureOpen()
        return transaction(dbConnection) {
            createAllTables()

            QuestionData.QuestionDataEntry
                .findById(id)
                ?.apply { this.language = language.findDao().id }
                ?.toQuestionObject()
        }
    }

    override fun setDifficulty(id: Int, difficulty: Difficulty): GuesstimateQuestion? {
        ensureOpen()
        return transaction(dbConnection) {
            createAllTables()

            QuestionData.QuestionDataEntry
                .findById(id)
                ?.apply { this.difficulty = difficulty.findDao().id }
                ?.toQuestionObject()
        }
    }

    override fun setCategory(id: Int, category: Category): GuesstimateQuestion? {
        ensureOpen()
        return transaction(dbConnection) {
            createAllTables()

            QuestionData.QuestionDataEntry
                .findById(id)
                ?.apply { this.category = category.findDao().id }
                ?.toQuestionObject()
        }
    }

    override fun setQuestion(id: Int, question: String): GuesstimateQuestion? {
        ensureOpen()
        return transaction(dbConnection) {
            createAllTables()

            QuestionData.QuestionDataEntry
                .findById(id)
                ?.apply { this.question = question }
                ?.toQuestionObject()
        }
    }

    override fun setAnswer(id: Int, answer: Long): GuesstimateQuestion? {
        ensureOpen()
        return transaction(dbConnection) {
            createAllTables()

            QuestionData.QuestionDataEntry
                .findById(id)
                ?.apply { this.answer = answer }
                ?.toQuestionObject()
        }
    }

    override fun clearHints(id: Int): GuesstimateQuestion? {
        ensureOpen()
        transaction(dbConnection) {
            createAllTables()

            val question = QuestionData.QuestionDataEntry
                .findById(id)

            question?.hints?.forEach {
                it.delete()
            }

        }
        return getQuestion(id)
    }

    override fun addHint(id: Int, hint: String): GuesstimateQuestion? {
        ensureOpen()
        transaction(dbConnection) {
            createAllTables()

            HintData.HintDataEntry
                .new {
                    this.question = QuestionData.QuestionDataEntry[id]
                    this.text = hint
                }
        }
        return getQuestion(id)
    }

    override fun deleteQuestion(id: Int): Boolean {
        ensureOpen()
        return transaction(dbConnection) {
            createAllTables()

            val question = QuestionData.QuestionDataEntry
                .findById(id)

            if (question != null) {
                clearHints(id)
                question.delete()
                true
            } else {
                false
            }
        }
    }

    private fun createAllTables() {
        SchemaUtils.create(CategoryData, DifficultyData, LanguageData, QuestionData, HintData)
    }

    override fun close() {
        isOpen = false
        TransactionManager.closeAndUnregister(dbConnection)
    }
}
