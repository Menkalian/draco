package de.menkalian.draco.server.socket

import de.menkalian.draco.data.game.SocketPackage

fun interface ISocketPackageHandler {
    fun onPackageReceived(sessionUuid: String, pkg: SocketPackage)
}