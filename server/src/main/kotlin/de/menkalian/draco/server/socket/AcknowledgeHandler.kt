package de.menkalian.draco.server.socket

import de.menkalian.draco.data.game.SocketPackage
import de.menkalian.draco.server.util.logger

class AcknowledgeHandler : ISocketPackageHandler {
    private val openAcknowledgements = mutableListOf<AcknowledgeRequest>()

    fun requestAcknowledgement(request: AcknowledgeRequest) {
        synchronized(openAcknowledgements) {
            logger().debug("Awaiting Acknowledgement for $request")
            openAcknowledgements.add(request)
        }
    }

    fun cancelAcknowledgement(id: Long): Boolean {
        synchronized(openAcknowledgements) {
            val canceled = openAcknowledgements.firstOrNull { it.id == id }
            if (canceled != null) {
                logger().debug("Canceling Acknowledgement for $canceled")
                openAcknowledgements.remove(canceled)
            }
            return canceled != null
        }
    }

    override fun onPackageReceived(sessionUuid: String, pkg: SocketPackage) {
        synchronized(openAcknowledgements) {
            val acknowledged = openAcknowledgements.firstOrNull { it.id == pkg.id }

            if (acknowledged != null) {
                logger().debug("Acknowledgement received for $acknowledged")
                openAcknowledgements.remove(acknowledged)
                acknowledged.timeoutJob.cancel()
                acknowledged.onAcknowledge()
            }
        }
    }
}