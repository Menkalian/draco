package de.menkalian.draco.server.database.guesstimate.question.dao

import de.menkalian.draco.data.quesstimate.Difficulty
import de.menkalian.draco.server.database.shared.dao.EnumDataTable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

object DifficultyData : EnumDataTable() {
    class DifficultyDataEntry(id: EntityID<Int>) : IntEntity(id) {
        companion object : IntEntityClass<DifficultyDataEntry>(DifficultyData) {
            fun Difficulty.findDao(): DifficultyDataEntry {
                return find { DifficultyData.name.eq(this@findDao.name) }.first()
            }
        }

        var name by DifficultyData.name

        fun toEnum(): Difficulty {
            return Difficulty.valueOf(name)
        }
    }
}