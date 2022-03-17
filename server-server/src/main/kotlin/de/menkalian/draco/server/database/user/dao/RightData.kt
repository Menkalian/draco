package de.menkalian.draco.server.database.user.dao

import de.menkalian.draco.data.user.UserRight
import de.menkalian.draco.server.database.shared.dao.EnumDataTable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

object RightData : EnumDataTable() {
    class RightDataEntry(id: EntityID<Int>): IntEntity(id) {
        companion object : IntEntityClass<RightDataEntry>(RightData) {
            fun UserRight.findDao() : RightDataEntry {
                return RightDataEntry.find { RightData.name.eq(this@findDao.name) }.first()
            }
        }

        var name by RightData.name

        fun toEnum(): UserRight {
            return UserRight.valueOf(name)
        }
    }
}