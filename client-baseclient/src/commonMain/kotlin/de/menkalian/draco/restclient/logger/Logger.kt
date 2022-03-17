package de.menkalian.draco.restclient.logger

interface Logger {
    fun trace(msg: String) = log(LogLevel.TRACE, msg)
    fun debug(msg: String) = log(LogLevel.DEBUG, msg)
    fun info(msg: String) = log(LogLevel.INFO, msg)
    fun warn(msg: String) = log(LogLevel.WARN, msg)
    fun error(msg: String) = log(LogLevel.ERROR, msg)
    fun fatal(msg: String) = log(LogLevel.FATAL, msg)

    fun log(level: LogLevel, msg: String)
}