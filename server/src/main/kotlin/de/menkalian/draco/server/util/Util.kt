package de.menkalian.draco.server.util

fun catchBoolean(function: () -> Boolean) : Boolean {
    return try {
        function()
    } catch (ex: Exception) {
        false
    }
}