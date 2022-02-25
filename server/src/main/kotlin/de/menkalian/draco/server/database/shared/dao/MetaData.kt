package de.menkalian.draco.server.database.shared.dao

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object MetaData : IntIdTable() {
    val key = varchar("key", 255).uniqueIndex()
    val value = text("value")

    class MetaDataEntry(id: EntityID<Int>) : IntEntity(id) {
        companion object : IntEntityClass<MetaDataEntry>(MetaData)
        var key by MetaData.key
        var value by MetaData.value
    }
}