package de.menkalian.draco.services

import de.menkalian.draco.data.user.EntitledUser
import de.menkalian.draco.restclient.DracoClient

interface IAuthenticationService {
    interface IAuthenticationListener {
        fun onLoggedIn(user: EntitledUser)
        fun onLoggedOut()
    }

    val hasAuthToken: Boolean
    val currentUser: EntitledUser?
    val clientWithAuthentication: DracoClient

    fun login(token: String)
    fun logout()

    fun addListener(listener: IAuthenticationListener, fireImmediately: Boolean = false)
    fun removeListener(listener: IAuthenticationListener)
}