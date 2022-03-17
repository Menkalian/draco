package de.menkalian.draco.restclient.logger

import org.slf4j.LoggerFactory

class Slf4jLogger(val logger: org.slf4j.Logger) : Logger {
    constructor(loggerName: String = "DracoClient") : this(LoggerFactory.getLogger(loggerName))

    override fun log(level: LogLevel, msg: String) {
        when (level) {
            LogLevel.TRACE -> logger.trace(msg)
            LogLevel.DEBUG -> logger.debug(msg)
            LogLevel.INFO  -> logger.info(msg)
            LogLevel.WARN  -> logger.warn(msg)
            LogLevel.ERROR -> logger.error(msg)
            LogLevel.FATAL -> logger.error("FATAL: $msg")
        }
    }
}