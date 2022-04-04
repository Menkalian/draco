package de.menkalian.draco.services

import com.vaadin.flow.component.UI

interface ISharedServiceFactory {
    fun getSharedAuthService(ui: UI) : IAuthenticationService
    fun hasSharedAuthService(ui: UI) : Boolean
    fun releaseSharedAuthService(ui: UI)
}