package de.menkalian.draco.restclient

object DracoClientFactory {
    fun createClient(configuration: DracoClientConfiguration): DracoClient {
        return DracoClient(createHttpClient(), configuration)
    }
}

