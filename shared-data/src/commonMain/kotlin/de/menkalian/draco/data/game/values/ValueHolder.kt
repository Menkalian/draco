package de.menkalian.draco.data.game.values

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

open class ValueHolder {
    private val knownValues: MutableMap<String, TransferableValue> = mutableMapOf()
    private val listeners: MutableList<ValueChangeListener> = mutableListOf()
    private val listenerMutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.Default)

    fun addListener(listener: ValueChangeListener) {
        scope.launch {
            listenerMutex.withLock {
                listeners.add(listener)
            }
        }
    }

    fun removeListener(listener: ValueChangeListener) {
        scope.launch {
            listenerMutex.withLock {
                listeners.remove(listener)
            }
        }
    }

    fun notifyChange(key: String, value: TransferableValue) {
        notifyChanges(mutableMapOf(key to value))
    }

    fun notifyChanges(values: Values) {
        if (values.all { knownValues[it.key] == it.value }) {
            return
        }
        knownValues.putAll(values)

        scope.launch {
            listenerMutex.withLock {
                for (listener in listeners) {
                    if (values.any { listener.filterKey(it.key) }) {
                        // First run bulk updates
                        scope.launch {
                            listener.onValuesChanged(values)
                        }

                        values
                            .filter { listener.filterKey(it.key) }
                            .forEach {
                                scope.launch {
                                    listener.onValueChanged(it.key, it.value)
                                }
                            }
                    }
                }
            }
        }
    }
}