package de.menkalian.draco.data.game.values

interface ValueChangeListener {
    fun filterKey(key: String): Boolean
    fun onValueChanged(key: String, value: TransferableValue)
    fun onValuesChanged(values: Values)
}