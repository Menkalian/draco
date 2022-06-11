package de.menkalian.draco.data.game.event.events

import kotlinx.serialization.Serializable

@Serializable
class QuestionHintRevealedEvent(val text: String) : Event()