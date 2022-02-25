package de.menkalian.draco.server.database.shared

import de.menkalian.draco.server.database.IDatabase
import de.menkalian.draco.server.database.shared.dao.MetaData
import de.menkalian.draco.server.util.logger
import de.menkalian.draco.variables.DracoKey.Draco
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.boot.info.BuildProperties
import java.time.Instant
import java.time.format.DateTimeFormatter

class MetaDataAwareDatabaseExtension {
    private val metadataChangedListeners = mutableMapOf<String, IDatabase.IMetaDataChangedListener>()
    private var defaultListener = IDatabase.IMetaDataChangedListener { _, _, _ -> true }

    fun setDefaultListener(listener: IDatabase.IMetaDataChangedListener) {
        synchronized(metadataChangedListeners) {
            defaultListener = listener
        }
    }

    fun setListener(key: String, listener: IDatabase.IMetaDataChangedListener) {
        synchronized(metadataChangedListeners) {
            metadataChangedListeners[key] = listener
        }
    }

    fun initMetadata(database: IDatabase, build: BuildProperties, schemaVersion: Int, name: String) {
        setListener(Draco.Database.CreatedAt) { _, _, _ -> false }

        val entriesToSet: MutableMap<String, String> = mutableMapOf()
        entriesToSet[Draco.Database.Type] = name
        entriesToSet[Draco.Database.Version] = schemaVersion.toString()
        entriesToSet[Draco.Database.Timestamp] = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
        entriesToSet[Draco.Database.CreatedAt] = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
        entriesToSet[Draco.Build.Version] = "${build.group}:${build.artifact}:${build.version}"
        entriesToSet[Draco.Build.Timestamp] = DateTimeFormatter.ISO_INSTANT.format(build.time)

        database.ensureOpen()
        updateMetadata(database.dbConnection, entriesToSet)
    }

    fun updateMetadata(connection: Database, newMetadata: Map<String, String>) {
        transaction(connection) {
            SchemaUtils.create(MetaData)

            newMetadata.forEach { (key, value) ->
                insertMetaData(key, value)
            }
        }
    }

    // MUST be called within an transaction
    private fun insertMetaData(key: String, value: String) {
        val oldEntry = MetaData.MetaDataEntry
            .find { MetaData.key.eq(key) }
            .firstOrNull()

        if (oldEntry == null) {
            logger().debug("Adding MetaData: (\"$key\" => \"$value\")")
            MetaData.MetaDataEntry.new {
                this.key = key
                this.value = value
            }
            return
        }

        if (oldEntry.value != value) {
            if (fireChanged(key, oldEntry.value, value)) {
                logger().debug("Changing MetaData: (\"$key\" => \"$value\")")
                oldEntry.value = value
            } else {
                logger().debug("MetaDataChangeListener denied changing \"$key\"")
                logger().debug("Did not update MetaData: (\"$key\" => \"$value\")")
            }
        } else {
            logger().debug("Unchanged MetaData: (\"$key\" => \"$value\")")
        }
    }

    private fun fireChanged(key: String, old: String, new: String): Boolean {
        synchronized(metadataChangedListeners) {
            return metadataChangedListeners[key]?.onEntryChanged(key, old, new)
                ?: defaultListener.onEntryChanged(key, old, new)
        }
    }
}