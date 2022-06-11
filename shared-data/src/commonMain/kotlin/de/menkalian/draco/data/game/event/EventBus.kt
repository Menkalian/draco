package de.menkalian.draco.data.game.event

import de.menkalian.draco.data.game.event.events.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.reflect.KClass

open class EventBus {
    protected val eventScope = CoroutineScope(Dispatchers.Default)
    private val dispatcherLock = Mutex()
    private val dispatchers = mutableListOf<EventDispatcher<out Event>>()

    suspend fun <T : Event> addListenerSync(eventType: KClass<T>, listener: EventListener<T>) {
        addListener(eventType, listener).join()
    }

    fun <T : Event> addListener(eventType: KClass<T>, listener: EventListener<T>): Job {
        return eventScope.launch {
            getEventDispatcherWithType(eventType)
                .addListener(listener)
        }
    }

    suspend fun <T : Event> removeListenerSync(eventType: KClass<T>, listener: EventListener<T>) {
        removeListener(eventType, listener).join()
    }

    fun <T : Event> removeListener(eventType: KClass<T>, listener: EventListener<T>): Job {
        return eventScope.launch {
            getEventDispatcherWithType(eventType)
                .removeListener(listener)
        }
    }

    suspend fun <T : Event> fireEventSync(event: T) {
        fireEvent(event).join()
    }

    open fun <T : Event> fireEvent(event: T): Job {
        val eventClass = event::class
        return eventScope.launch {
            getEventDispatcherWithType(eventClass)
                .fireEvent(event)
        }
    }

    private suspend fun <T : Event> getEventDispatcherWithType(eventClass: KClass<T>): EventDispatcher<T> {
        dispatcherLock.withLock {
            val toReturn = dispatchers
                .filter { it.eventClass == eventClass }
                .firstOrNull()
            if (toReturn != null) {
                // Checked by filter above. Generics can be annoying.
                @Suppress("UNCHECKED_CAST")
                return toReturn as EventDispatcher<T>
            }

            val newDispatcher = EventDispatcher(eventClass)
            dispatchers.add(newDispatcher)
            return newDispatcher
        }
    }

    private inner class EventDispatcher<T : Event>(val eventClass: KClass<T>) {
        private val lock = Mutex()
        private val listeners = mutableListOf<EventListener<T>>()

        suspend fun addListener(listener: EventListener<T>) {
            lock.withLock {
                listeners.add(listener)
            }
        }

        suspend fun removeListener(listener: EventListener<T>) {
            lock.withLock {
                listeners.remove(listener)
            }
        }

        suspend fun fireEvent(event: Event) {
            if (eventClass.isInstance(event).not())
                throw IllegalArgumentException("Event $event not valid for type $eventClass")

            // Cast is checked above. Generics are fun :)
            @Suppress("UNCHECKED_CAST")
            val castedEvent = event as T
            lock.withLock {
                listeners.forEach {
                    eventScope.launch {
                        it.onEvent(event)
                    }
                }
            }
        }
    }

}

