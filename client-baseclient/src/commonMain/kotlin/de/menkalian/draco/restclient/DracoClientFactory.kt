package de.menkalian.draco.restclient

import de.menkalian.draco.createHttpClient
import de.menkalian.draco.restclient.config.DracoClientConfiguration

object DracoClientFactory {
    fun createClient(configuration: DracoClientConfiguration): DracoClient {
        return DracoClient(createHttpClient(), configuration)
    }
}

