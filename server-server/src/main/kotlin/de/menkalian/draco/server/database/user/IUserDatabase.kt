package de.menkalian.draco.server.database.user

import de.menkalian.draco.data.user.EntitledUser
import de.menkalian.draco.data.user.UserRight
import de.menkalian.draco.server.database.IDatabase

interface IUserDatabase : IDatabase {
    // CREATE
    fun createUser(name: String): EntitledUser

    // READ
    fun getUsers(): List<EntitledUser>
    fun getUser(id: Int): EntitledUser?
    fun getUser(accessHash: String): EntitledUser?

    // UPDATE
    fun updateUser(userId: Int, modified: EntitledUser) : EntitledUser?
    fun changeName(userId: Int, name: String): EntitledUser?
    fun addRight(userId: Int, right: UserRight): EntitledUser?
    fun clearRights(userId: Int) : EntitledUser?

    // DELETE
    fun deleteUser(userId: Int): Boolean
}