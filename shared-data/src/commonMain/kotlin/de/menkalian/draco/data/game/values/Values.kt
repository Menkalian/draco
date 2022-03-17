package de.menkalian.draco.data.game.values

typealias Values = MutableMap<String, TransferableValue>

fun Values.addStringList(
    list: List<String>,
    amountKey: String,
    valueKeySupplier: (Int) -> String
) {
    this[amountKey] = TransferableValue.from(list.size)
    list.forEachIndexed { idx, s ->
        this[valueKeySupplier(idx + 1)] = TransferableValue.from(s)
    }
}