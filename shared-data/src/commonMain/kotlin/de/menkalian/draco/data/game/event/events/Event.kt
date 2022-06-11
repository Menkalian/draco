package de.menkalian.draco.data.game.event.events

import kotlinx.serialization.Serializable
import kotlinx.serialization.StringFormat
import kotlinx.serialization.encodeToString

@Serializable
sealed class Event {
    fun serialized(serializer: StringFormat) : String {
        return serializer.encodeToString(this)
    }
}