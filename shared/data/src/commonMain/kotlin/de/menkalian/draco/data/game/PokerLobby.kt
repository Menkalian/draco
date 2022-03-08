package de.menkalian.draco.data.game

@kotlinx.serialization.Serializable
data class PokerLobby(
    val uuid: String,
    val accessToken: String,
    val host: Player,
    val players: MutableList<Player>,
    var settings: PokerSettings,
    var connectionValues: ConnectionSettings,
    var gameStateValues: PokerGameValues
)
