package de.menkalian.draco.server.database.user.dao

import de.menkalian.draco.data.user.EntitledUser
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object UserData : IntIdTable() {
    val name = varchar("name", 255)
    val accessHash = text("hash")

    class UserDataEntry(id: EntityID<Int>): IntEntity(id) {
        companion object : IntEntityClass<UserDataEntry>(UserData)
        var name by UserData.name
        var accessHash by UserData.accessHash

        var rights by RightData.RightDataEntry via UserRightRelation

        fun toUserObject() : EntitledUser {
            return EntitledUser(
                id.value,
                accessHash,
                name,
                rights.map { it.toEnum() }
            )
        }
    }
}