package de.menkalian.draco.data.game.values

class RegexValueMultiChangeListener(val keyRegex: Regex, val action: (Values) -> Unit) : ValueChangeListener {
    override fun filterKey(key: String): Boolean {
        return keyRegex.matches(key)
    }

    override fun onValueChanged(key: String, value: TransferableValue) {
    }

    override fun onValuesChanged(values: Values) = action(values)
}