package de.menkalian.draco.data.game.values

class RegexValueChangeListener(val keyRegex: Regex, val action: (String, TransferableValue) -> Unit) :ValueChangeListener {
    override fun filterKey(key: String): Boolean {
        return keyRegex.matches(key)
    }

    override fun onValueChanged(key: String, value: TransferableValue) = action(key, value)

    override fun onValuesChanged(values: Values) {
    }
}