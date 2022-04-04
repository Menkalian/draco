package de.menkalian.draco

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DracoApplication

fun main(args: Array<String>) {
    runApplication<DracoApplication>(*args)
}

fun Any.logger() : Logger {
    return LoggerFactory.getLogger(this::class.java)
}