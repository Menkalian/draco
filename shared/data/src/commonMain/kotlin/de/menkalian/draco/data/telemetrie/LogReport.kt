package de.menkalian.draco.data.telemetrie

import kotlinx.serialization.Serializable

@Serializable
data class LogReport(
    val logFileName: String,
    val logDomain: String,
    val date: String,
    val logData: String
)
