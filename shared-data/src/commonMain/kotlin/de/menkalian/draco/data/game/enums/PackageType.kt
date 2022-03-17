package de.menkalian.draco.data.game.enums

enum class PackageType {
    HEARTBEAT,
    HEARTBEAT_ACK,

    CLIENT_HELLO,
    CLIENT_MSG,
    CLIENT_MSG_ACK,

    SERVER_BROADCAST,
    SERVER_MSG,
    SERVER_MSG_ACK;

    fun getAcknowledge(): PackageType? {
        return when (this) {
            HEARTBEAT -> HEARTBEAT_ACK
            CLIENT_MSG -> CLIENT_MSG_ACK
            SERVER_MSG -> SERVER_MSG_ACK
            else -> null
        }
    }
}