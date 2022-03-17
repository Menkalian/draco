package de.menkalian.draco.server.socket

import de.menkalian.draco.data.game.enums.PackageType
import kotlinx.coroutines.Job

data class AcknowledgeRequest(
    val id: Long,
    val type: PackageType,
    val timeoutJob: Job,
    val onAcknowledge: () -> Unit
)
