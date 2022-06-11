package de.menkalian.draco.data.game

import de.menkalian.draco.data.Draco
import de.menkalian.draco.data.game.enums.AnswerRevealStrategy
import de.menkalian.draco.data.game.enums.BlindRaiseStrategy
import de.menkalian.draco.data.game.enums.LateJoinBehaviour
import de.menkalian.draco.data.game.enums.LobbyPublicity
import de.menkalian.draco.data.game.enums.TimeoutStrategy
import de.menkalian.draco.data.game.values.TransferableValue
import de.menkalian.draco.data.game.values.ValueHolder
import de.menkalian.draco.data.game.values.addStringList
import de.menkalian.draco.data.quesstimate.Category
import de.menkalian.draco.data.quesstimate.Difficulty
import de.menkalian.draco.data.quesstimate.Language
import kotlin.properties.Delegates

@Suppress("unused")
@kotlinx.serialization.Serializable
class PokerSettings constructor() : ValueHolder() {
    constructor(builder: PokerSettingsBuilder) : this() {
        this.lobbyName = builder.lobbyName
        this.publicity = builder.publicity
        this.defaultStartScore = builder.defaultStartScore
        this.maxQuestionCount = builder.maxQuestionCount
        this.allowedCategories = builder.allowedCategories
        this.allowedDifficulties = builder.allowedDifficulties
        this.allowedLanguages = builder.allowedLanguages
        this.showHelpWarnings = builder.showHelpWarnings

        this.allowLateJoin = builder.allowLateJoin
        this.kickWhenBroke = builder.kickWhenBroke
        this.lateJoinBehaviour = builder.lateJoinBehaviour
        this.answerRevealStrategy = builder.answerRevealStrategy

        this.timeoutMs = builder.timeoutMs
        this.blindRaiseStrategy = builder.blindRaiseStrategy
        this.timeoutStrategy = builder.timeoutStrategy
        this.blindLevels = builder.blindLevels
    }

    var lobbyName: String by Delegates.observable("") { _, _, new ->
        notifyChange(Draco.Game.Poker.Settings.Lobby.Name, TransferableValue.from(new))
    }

    var publicity: LobbyPublicity by Delegates.observable(LobbyPublicity.PUBLIC) { _, _, new ->
        notifyChange(Draco.Game.Poker.Settings.Lobby.Publicity, TransferableValue.from(new.name))
    }

    var defaultStartScore: Long by Delegates.observable(0) { _, _, new ->
        notifyChange(Draco.Game.Poker.Settings.DefaultPoints, TransferableValue.from(new))
    }

    var maxQuestionCount: Long by Delegates.observable(0) { _, _, new ->
        notifyChange(Draco.Game.Poker.Settings.MaxQuestions, TransferableValue.from(new))
    }

    var timeoutMs: Long by Delegates.observable(0) { _, _, new ->
        notifyChange(Draco.Game.Poker.Settings.Timeout, TransferableValue.from(new))
    }

    var kickWhenBroke: Boolean by Delegates.observable(false) { _, _, new ->
        notifyChange(Draco.Game.Poker.Settings.KickBroke, TransferableValue.from(new))
    }

    var showHelpWarnings: Boolean by Delegates.observable(false) { _, _, new ->
        notifyChange(Draco.Game.Poker.Settings.ShowHelpWarnings, TransferableValue.from(new))
    }

    var allowLateJoin: Boolean by Delegates.observable(false) { _, _, new ->
        notifyChange(Draco.Game.Poker.Settings.LateJoin, TransferableValue.from(new))
    }

    var blindLevels: List<Pair<Long, Long>> by Delegates.observable(listOf()) { _, _, new ->
        val values = mutableMapOf<String, TransferableValue>()
        values[Draco.Game.Poker.Settings.Blinds.n] = TransferableValue.from(new.size)
        new.forEachIndexed { idx, bds ->
            values[Draco.Game.Poker.Settings.Blinds.XXX(idx + 1).Small] = TransferableValue.from(bds.first)
            values[Draco.Game.Poker.Settings.Blinds.XXX(idx + 1).Big] = TransferableValue.from(bds.second)
        }

        notifyChanges(values)
    }

    var blindRaiseStrategy: BlindRaiseStrategy by Delegates.observable(BlindRaiseStrategy.QUIZMASTER) { _, _, new ->
        notifyChange(Draco.Game.Poker.Settings.BlindStrategy, TransferableValue.from(new.name))
    }

    var answerRevealStrategy: AnswerRevealStrategy by Delegates.observable(AnswerRevealStrategy.NEVER) { _, _, new ->
        notifyChange(Draco.Game.Poker.Settings.RevealStrategy, TransferableValue.from(new.name))
    }

    var timeoutStrategy: TimeoutStrategy by Delegates.observable(TimeoutStrategy.AUTO_FOLD) { _, _, new ->
        notifyChange(Draco.Game.Poker.Settings.TimeoutStrategy, TransferableValue.from(new.name))
    }

    var lateJoinBehaviour: LateJoinBehaviour by Delegates.observable(LateJoinBehaviour.MEDIAN_SCORE) { _, _, new ->
        notifyChange(Draco.Game.Poker.Settings.LateJoinStrategy, TransferableValue.from(new.name))
    }

    var allowedCategories by Delegates.observable(setOf<Category>()) { _, _, new ->
        val values = mutableMapOf<String, TransferableValue>()
        values.addStringList(
            new.map { it.name },
            Draco.Game.Poker.Settings.Categories.n
        ) { Draco.Game.Poker.Settings.Categories.XXX(it).Name }
        notifyChanges(values)
    }

    var allowedDifficulties by Delegates.observable(setOf<Difficulty>()) { _, _, new ->
        val values = mutableMapOf<String, TransferableValue>()
        values.addStringList(
            new.map { it.name },
            Draco.Game.Poker.Settings.Difficulties.n
        ) { Draco.Game.Poker.Settings.Difficulties.XXX(it).Name }
        notifyChanges(values)
    }

    var allowedLanguages by Delegates.observable(setOf<Language>()) { _, _, new ->
        val values = mutableMapOf<String, TransferableValue>()
        values.addStringList(
            new.map { it.name },
            Draco.Game.Poker.Settings.Languages.n
        ) { Draco.Game.Poker.Settings.Languages.XXX(it).Name }

        notifyChanges(values)
    }

    override fun toString(): String {
        return """
            Poker-Settings:
              - Lobby name            = $lobbyName
              - Publicity             = $publicity
              - Start score           = $defaultStartScore
              - Timeout:
                - Duration (ms)       = $timeoutMs
                - Strategy            = $timeoutStrategy
              - Behaviour:
                - Kick broke players  = $kickWhenBroke
                - Show Warnings       = $showHelpWarnings
                - Reveal Answers      = $answerRevealStrategy
                - Allow late join     = $allowLateJoin
                - Late join behaviour = $lateJoinBehaviour
              - Blinds:
                - Levels              = $blindLevels
                - Raising             = $blindRaiseStrategy
              - Questions:
                - Max. Amount         = $maxQuestionCount
                - Languages           = $allowedLanguages
                - Difficulties        = $allowedDifficulties
                - Categories          = $allowedCategories
        """.trimIndent()
    }

    class PokerSettingsBuilder {
        internal var lobbyName: String = "_"
        internal var publicity: LobbyPublicity = LobbyPublicity.CODE_ONLY
        internal var defaultStartScore: Long = 5000
        internal var maxQuestionCount: Long = -1
        internal var timeoutMs: Long = 60_000

        internal var kickWhenBroke: Boolean = false
        internal var showHelpWarnings: Boolean = false
        internal var allowLateJoin: Boolean = false

        internal var blindRaiseStrategy: BlindRaiseStrategy = BlindRaiseStrategy.ON_ROUNDED
        internal var answerRevealStrategy: AnswerRevealStrategy = AnswerRevealStrategy.NEVER
        internal var timeoutStrategy: TimeoutStrategy = TimeoutStrategy.AUTO_FOLD
        internal var lateJoinBehaviour: LateJoinBehaviour = LateJoinBehaviour.MEDIAN_SCORE

        internal var blindLevels: List<Pair<Long, Long>> =
            listOf(
                50L to 100L,
                100L to 200L,
                200L to 400L,
                500L to 1000L
            )

        internal var allowedCategories: Set<Category> = setOf()
        internal var allowedDifficulties: Set<Difficulty> = setOf()
        internal var allowedLanguages: Set<Language> = setOf()

        fun lobbyName(lobbyName: String) = apply {
            this.lobbyName = lobbyName
        }

        fun publicity(publicity: LobbyPublicity) = apply {
            this.publicity = publicity
        }

        fun defaultStartScore(defaultStartScore: Long) = apply {
            this.defaultStartScore = defaultStartScore
        }

        fun maxQuestionCount(maxQuestionCount: Long) = apply {
            this.maxQuestionCount = maxQuestionCount
        }

        fun timeoutMs(timeoutMs: Long) = apply {
            this.timeoutMs = timeoutMs
        }

        fun kickWhenBroke(kickWhenBroke: Boolean) = apply {
            this.kickWhenBroke = kickWhenBroke
        }

        fun showHelpWarnings(showHelpWarnings: Boolean) = apply {
            this.showHelpWarnings = showHelpWarnings
        }

        fun allowLateJoin(allowLateJoin: Boolean) = apply {
            this.allowLateJoin = allowLateJoin
        }

        fun blindRaiseStrategy(blindRaiseStrategy: BlindRaiseStrategy) = apply {
            this.blindRaiseStrategy = blindRaiseStrategy
        }

        fun answerRevealStrategy(answerRevealStrategy: AnswerRevealStrategy) = apply {
            this.answerRevealStrategy = answerRevealStrategy
        }

        fun timeoutStrategy(timeoutStrategy: TimeoutStrategy) = apply {
            this.timeoutStrategy = timeoutStrategy
        }

        fun lateJoinBehaviour(lateJoinBehaviour: LateJoinBehaviour) = apply {
            this.lateJoinBehaviour = lateJoinBehaviour
        }

        fun blindLevels(blindLevels: List<Pair<Long, Long>>) = apply {
            this.blindLevels = blindLevels
        }

        fun allowedCategories(allowedCategories: Set<Category>) = apply {
            this.allowedCategories = allowedCategories
        }

        fun allowedDifficulties(allowedDifficulties: Set<Difficulty>) = apply {
            this.allowedDifficulties = allowedDifficulties
        }

        fun allowedLanguages(allowedLanguages: Set<Language>) = apply {
            this.allowedLanguages = allowedLanguages
        }

        fun build() = PokerSettings(this)
    }
}