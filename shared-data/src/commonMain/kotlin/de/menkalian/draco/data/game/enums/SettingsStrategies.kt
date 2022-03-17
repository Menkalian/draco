package de.menkalian.draco.data.game.enums

enum class BlindRaiseStrategy
    (
    val dropout: Boolean = false,
    val rounded: Boolean = false,
) {
    ON_DROPOUT(dropout = true),
    ON_ROUNDED(rounded = true),
    ON_DROPOUT_AND_ROUNDED(dropout = true, rounded = true),
    QUIZMASTER
}

enum class AnswerRevealStrategy {
    NEVER,
    ALWAYS
}

enum class TimeoutStrategy {
    AUTO_FOLD,
    AUTO_CALL,
    KICK
}

enum class LateJoinBehaviour {
    DEFAULT_SCORE,
    MIN_SCORE,
    AVG_SCORE,
    MEDIAN_SCORE
}
