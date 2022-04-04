package de.menkalian.draco.services.impl

import de.menkalian.draco.restclient.DracoClient
import de.menkalian.draco.restclient.DracoClientFactory
import de.menkalian.draco.restclient.config.DracoClientConfiguration
import de.menkalian.draco.restclient.logger.Slf4jLogger
import de.menkalian.draco.services.IClientProviderService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class DefaultClientProviderService(
    @Value("\${draco.api.tls}") private val useHttps: Boolean,
    @Value("\${draco.api.host}") private val serverUrl: String,
    @Value("\${draco.api.port}") private val serverPort: Short,
) : IClientProviderService {
    private val defaultClientConfiguration: DracoClientConfiguration
    private val defaultClient: DracoClient

    init {
        defaultClientConfiguration = DracoClientConfiguration
            .DracoClientConfigurationBuilder()
            .logger(Slf4jLogger("DefaultRestClient"))
            .useHttps(useHttps)
            .serverUrl(serverUrl)
            .serverPort(serverPort)
            .build()

        defaultClient = DracoClientFactory.createClient(defaultClientConfiguration)
    }

    override fun provideUnauthorizedClient() = defaultClient

    override fun provideAuthorizedClient(authToken: String?): DracoClient {
        if (authToken == null) {
            return defaultClient
        }

        val config = defaultClientConfiguration
            .copy(authToken = authToken)

        return DracoClientFactory.createClient(config)
    }
}