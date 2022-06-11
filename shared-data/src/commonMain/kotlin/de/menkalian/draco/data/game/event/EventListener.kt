package de.menkalian.draco.data.game.event

import de.menkalian.draco.data.game.event.events.Event

fun interface EventListener<T : Event> {
    fun onEvent(event: T)
}