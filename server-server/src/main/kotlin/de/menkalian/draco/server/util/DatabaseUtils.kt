package de.menkalian.draco.server.util

import de.menkalian.draco.server.database.shared.dao.EnumDataTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

fun initEnumDatabase(dbConnection: Database, table: EnumDataTable, names: List<String>) {
    transaction(dbConnection) {
        SchemaUtils.create(table)

        for (name in names) {
            val existsNot = table
                .select { table.name.eq(name) }
                .empty()

            if (existsNot) {
                table.insert {
                    it[table.name] = name
                }
            }
        }
    }
}