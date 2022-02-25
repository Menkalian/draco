package de.menkalian.draco.server.database.telemetrie.dao

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object LogData : IntIdTable() {
    val file = varchar("file", 255)
    val domain = varchar("domain", 80)
    val date = varchar("date", 80)
    val data = text("data")

    class LogDataEntry(id: EntityID<Int>): IntEntity(id) {
        companion object : IntEntityClass<LogDataEntry>(LogData)
        var file by LogData.file
        var domain by LogData.domain
        var date by LogData.date
        var data by LogData.data
    }
}