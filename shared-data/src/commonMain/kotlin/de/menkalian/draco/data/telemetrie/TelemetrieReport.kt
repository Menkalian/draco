package de.menkalian.draco.data.telemetrie

import kotlinx.serialization.Serializable

@Serializable
data class TelemetrieReport(
    val reporterName: String?,
    val reporterEmail: String?,
    val reportText: String?,
    val logs: List<LogReport>?
)
