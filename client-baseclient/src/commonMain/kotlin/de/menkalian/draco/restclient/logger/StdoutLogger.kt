package de.menkalian.draco.restclient.logger

class StdoutLogger : Logger {
    override fun log(level: LogLevel, msg: String) {
        println("${colored(level)}[$level] $msg${colorReset()}")
    }

    private fun colored(level: LogLevel): String {
        return when (level) {
            LogLevel.TRACE -> "\u001B[38;2;100;100;100m"
            LogLevel.DEBUG -> "\u001B[38;2;110;70;110m"
            LogLevel.INFO  -> "\u001B[38;2;10;110;210m"
            LogLevel.WARN  -> "\u001B[38;2;210;140;10m"
            LogLevel.ERROR -> "\u001B[38;2;200;20;0m"
            LogLevel.FATAL -> "\u001B[38;2;200;20;0m"
        }
    }

    private fun colorReset() = "\u001B[39m"
}