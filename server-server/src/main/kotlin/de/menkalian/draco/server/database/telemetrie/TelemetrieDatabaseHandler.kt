@file:Suppress("LeakingThis")

package de.menkalian.draco.server.database.telemetrie

import de.menkalian.draco.server.util.catchBoolean
import de.menkalian.draco.server.util.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.info.BuildProperties
import org.springframework.stereotype.Service
import java.io.File
import java.util.UUID

@Service
class TelemetrieDatabaseHandler(
    @Value("\${draco.telemetrie.export.path}") val databaseExportPath: String,
    private val buildProperties: BuildProperties
) {
    private val exportFolder: File

    init {
        logger().info("TelemetrieDatabaseHandler initialized.")
        logger().debug("Using export path: \"$databaseExportPath\"")
        exportFolder = File(databaseExportPath)

        if (catchBoolean { exportFolder.mkdirs() }) {
            logger().debug("Export directory created successfully")
        } else {
            logger().warn("Could not create export directory")
        }
    }

    fun createTelemetrieDatabase(): ITelemetrieDatabase {
        val uuid = generateUniqueUuid()
        return SqliteTelemetrieDatabase(
            uuid, buildProperties, File(exportFolder, "$uuid.DB3").absolutePath
        )
    }

    private fun generateUniqueUuid(): String {
        var uuid: String
        do {
            uuid = UUID.randomUUID().toString()
        } while (File(exportFolder, "$uuid.DB3").exists())

        return uuid
    }
}