package de.menkalian.draco.server.database.telemetrie

import de.menkalian.draco.data.telemetrie.LogReport
import de.menkalian.draco.data.telemetrie.TelemetrieReport
import de.menkalian.draco.server.database.IDatabase

/**
 * write-only database with telemetry data
 */
interface ITelemetrieDatabase : IDatabase {
    val uuid: String

    // CREATE
    fun saveTelemetrieReport(report: TelemetrieReport)

    // UPDATE
    fun setReporterName(name: String)
    fun setReporterEmail(email: String)
    fun setReportText(text: String)

    fun addLogReport(report: LogReport)
}