package de.menkalian.draco.restclient.error

import io.ktor.client.features.ClientRequestException
import io.ktor.client.features.HttpRequestTimeoutException
import io.ktor.client.features.RedirectResponseException
import io.ktor.client.features.ServerResponseException

suspend fun catchErrors(
    onError: (DracoError) -> Unit,
    executable: suspend () -> Unit
) {
    try {
        executable()
    } catch (ex: RedirectResponseException) {
        onError(
            DracoError(
                ex.response.status.value,
                "${ex.response.status.description}: ${ex.message}",
                ex
            )
        )
    } catch (ex: ClientRequestException) {
        onError(
            DracoError(
                ex.response.status.value,
                "${ex.response.status.description}: ${ex.message}",
                ex
            )
        )
    } catch (ex: ServerResponseException) {
        onError(
            DracoError(
                ex.response.status.value,
                "${ex.response.status.description}: ${ex.message}",
                ex
            )
        )
    } catch (ex: HttpRequestTimeoutException) {
        onError(
            DracoError(
                DracoError.ERR_TIMEOUT,
                "Timeout for request: \"${ex.message}\"",
                ex
            )
        )
    } catch (ex: Exception) {
        onError(
            DracoError(
                DracoError.ERR_UNKNOWN,
                "Unknown Error: \"${ex.message}\"",
                ex
            )
        )
    }
}
