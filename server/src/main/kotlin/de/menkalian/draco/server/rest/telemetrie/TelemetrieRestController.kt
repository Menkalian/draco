package de.menkalian.draco.server.rest.telemetrie

import de.menkalian.draco.data.telemetrie.TelemetrieReport
import de.menkalian.draco.server.database.telemetrie.TelemetrieDatabaseHandler
import de.menkalian.draco.server.util.logger
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class TelemetrieRestController(
    val telemetrieDatabaseHandler: TelemetrieDatabaseHandler
) {
    @PostMapping("/telemetrie/upload")
    fun uploadReport(@RequestBody report: TelemetrieReport) {
        logger().info("Received Telemetrie report")

        val db = telemetrieDatabaseHandler.createTelemetrieDatabase()
        db.saveTelemetrieReport(report)
        db.close()

        logger().info("Saved Report to Database ${db.uuid}")
    }
}