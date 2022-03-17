package de.menkalian.draco.data.user

import kotlinx.serialization.Serializable

@Serializable
data class EntitledUser(
    val id: Int = -1,
    val accessHash: String = "",
    val name: String,
    val rights: List<UserRight>
) {
    infix fun hasRight(right: UserRight): Boolean {
        return rights.contains(right) || rights.contains(UserRight.ADMIN)
    }
}
