package de.menkalian.draco.server.database.guesstimate.question.dao

import de.menkalian.draco.data.quesstimate.Category
import de.menkalian.draco.server.database.shared.dao.EnumDataTable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

object CategoryData : EnumDataTable() {
    class CategoryDataEntry(id: EntityID<Int>) : IntEntity(id) {
        companion object : IntEntityClass<CategoryDataEntry>(CategoryData) {
            fun Category.findDao(): CategoryDataEntry {
                return find { CategoryData.name.eq(this@findDao.name) }.first()
            }
        }

        var name by CategoryData.name

        fun toEnum(): Category {
            return Category.valueOf(name)
        }
    }
}