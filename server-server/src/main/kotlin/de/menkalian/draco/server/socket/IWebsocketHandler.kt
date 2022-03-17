package de.menkalian.draco.server.socket

import de.menkalian.draco.data.game.SocketPackage
import de.menkalian.draco.data.game.enums.PackageType

interface IWebsocketHandler {
    fun sendPackage(targetUuid: String, pkg: SocketPackage, acknowledgeTimeoutMs: Long = 5000, onSent: (success: Boolean) -> Unit = {}) =
        sendPackage(targetUuid, pkg, acknowledgeTimeoutMs, onSent, onAcknowledge = {}, onAcknowledgeFailed = {})

    fun sendPackage(
        targetUuid: String,
        pkg: SocketPackage,
        acknowledgeTimeoutMs: Long = 5000,
        onSent: (success: Boolean) -> Unit,
        onAcknowledge: () -> Unit,
        onAcknowledgeFailed: () -> Unit
    )

    fun addPackageHandler(type: PackageType, handler: ISocketPackageHandler)
    fun removePackageHandler(type: PackageType, handler: ISocketPackageHandler)
}