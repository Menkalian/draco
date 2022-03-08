package de.menkalian.draco.data.game.values

class SingleKeyValueChangeListener(val key: String, val action: (String, TransferableValue) -> Unit) :ValueChangeListener {
    override fun filterKey(key: String): Boolean {
        return this.key == key
    }

    override fun onValueChanged(key: String, value: TransferableValue) = action(key, value)

    override fun onValuesChanged(values: Values) {
    }
}