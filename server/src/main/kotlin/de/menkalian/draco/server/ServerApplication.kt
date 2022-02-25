package de.menkalian.draco.server

import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class ServerApplication {
    @Bean
    fun testRunner() : CommandLineRunner {
        return CommandLineRunner {
        }
    }
}

fun main(args: Array<String>) {
    runApplication<ServerApplication>(*args)
}
