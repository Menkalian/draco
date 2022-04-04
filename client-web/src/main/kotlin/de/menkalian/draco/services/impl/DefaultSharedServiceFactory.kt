package de.menkalian.draco.services.impl

import com.vaadin.flow.component.UI
import de.menkalian.draco.services.IAuthenticationService
import de.menkalian.draco.services.IClientProviderService
import de.menkalian.draco.services.ISharedServiceFactory
import org.springframework.stereotype.Service

@Service
class DefaultSharedServiceFactory(private val clientProviderService: IClientProviderService) : ISharedServiceFactory {
    val authServices = mutableMapOf<UI, IAuthenticationService>()

    override fun getSharedAuthService(ui: UI): IAuthenticationService {
        synchronized(authServices) {
            if (!authServices.containsKey(ui)) {
                authServices[ui] = DefaultAuthenticationService(clientProviderService)
            }
            return authServices[ui]!!
        }
    }

    override fun hasSharedAuthService(ui: UI): Boolean {
        return authServices.containsKey(ui)
    }

    override fun releaseSharedAuthService(ui: UI) {
        synchronized(authServices) {
            if (authServices.containsKey(ui)) {
                authServices.remove(ui)
            }
        }
    }
}