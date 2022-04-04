package de.menkalian.draco.services

import de.menkalian.draco.restclient.DracoClient

interface IClientProviderService {
    fun provideUnauthorizedClient(): DracoClient
    fun provideAuthorizedClient(authToken: String?): DracoClient
}