package de.menkalian.draco.server.database.shared.dao

import org.jetbrains.exposed.dao.id.IntIdTable

abstract class EnumDataTable : IntIdTable() {
    val name = varchar("name", 80)
}