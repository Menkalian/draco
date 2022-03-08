package de.menkalian.draco.data.game.values

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

open class ValueHolder {
    private val listeners: MutableList<ValueChangeListener> = mutableListOf()
    private val scope = CoroutineScope(Dispatchers.Default)

    fun addListener(listener: ValueChangeListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: ValueChangeListener) {
        listeners.remove(listener)
    }

    fun notifyChange(key: String, value: TransferableValue) {
        notifyChanges(mutableMapOf(key to value))
    }

    fun notifyChanges(values: Values) {
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