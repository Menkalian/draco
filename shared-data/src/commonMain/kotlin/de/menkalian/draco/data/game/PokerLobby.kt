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
) {
    override fun toString(): String {
        return """
            Lobby "$uuid" (Access-Token: "$accessToken")

            Settings: $settings
            ConnectionSettings: $connectionValues

            State: $gameStateValues

            Host: ${host.name}
            Players: $players
        """.trimIndent()
    }
}
