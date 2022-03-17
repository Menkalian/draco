package de.menkalian.draco.server.database.guesstimate.suggestion.dao

import de.menkalian.draco.data.quesstimate.SuggestionState
import de.menkalian.draco.server.database.shared.dao.EnumDataTable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

object SuggestionStateData : EnumDataTable() {
    class SuggestionStateDataEntry(id: EntityID<Int>) : IntEntity(id) {
        companion object : IntEntityClass<SuggestionStateDataEntry>(SuggestionStateData) {
            fun SuggestionState.findDao(): SuggestionStateDataEntry {
                return find { SuggestionStateData.name.eq(this@findDao.name) }.first()
            }
        }

        var name by SuggestionStateData.name

        fun toEnum(): SuggestionState {
            return SuggestionState.valueOf(name)
        }

    }
}