package de.menkalian.draco.restclient.error

data class DracoError(
    val code: Int,
    val message: String,
    val cause: Throwable,
) {
    companion object {
        const val ERR_TIMEOUT = -0xffff
        const val ERR_UNKNOWN = -1
    }
}
