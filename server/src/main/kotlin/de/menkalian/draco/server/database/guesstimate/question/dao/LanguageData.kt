package de.menkalian.draco.server.database.guesstimate.question.dao

import de.menkalian.draco.data.quesstimate.Language
import de.menkalian.draco.server.database.shared.dao.EnumDataTable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

object LanguageData : EnumDataTable() {
    class LanguageDataEntry(id: EntityID<Int>) : IntEntity(id) {
        companion object : IntEntityClass<LanguageDataEntry>(LanguageData) {
            fun Language.findDao(): LanguageDataEntry {
                return find { LanguageData.name.eq(this@findDao.name) }.first()
            }
        }

        var name by LanguageData.name

        fun toEnum(): Language {
            return Language.valueOf(name)
        }
    }
}