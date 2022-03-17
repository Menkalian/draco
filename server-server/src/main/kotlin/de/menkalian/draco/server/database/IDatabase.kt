package de.menkalian.draco.server.database

import org.jetbrains.exposed.sql.Database

interface IDatabase {
    fun interface IMetaDataChangedListener {
        fun onEntryChanged(key: String, oldValue: String, newValue: String) : Boolean
    }

    val isOpen: Boolean
    val dbConnection: Database

    fun ensureOpen() {
        if (!isOpen)
            throw IllegalStateException("Database may not be modified whilst it is not open")
    }

    fun close()
}