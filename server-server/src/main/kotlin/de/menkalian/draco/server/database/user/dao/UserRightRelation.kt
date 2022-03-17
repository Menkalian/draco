package de.menkalian.draco.server.database.user.dao

import org.jetbrains.exposed.sql.Table

object UserRightRelation : Table() {
    val user = reference("userId", UserData)
    val right = reference("rightId", RightData)
    override val primaryKey = PrimaryKey(user, right, name = "PK_User_Right")
}