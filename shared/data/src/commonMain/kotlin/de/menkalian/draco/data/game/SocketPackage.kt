package de.menkalian.draco.data.game

import de.menkalian.draco.data.game.enums.PackageType
import de.menkalian.draco.data.game.values.Values

@kotlinx.serialization.Serializable
data class SocketPackage(
    val id : Long,
    val type: PackageType,
    val timestamp: Long,
    val data: Values
)