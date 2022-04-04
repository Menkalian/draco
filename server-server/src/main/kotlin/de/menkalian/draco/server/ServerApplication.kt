package de.menkalian.draco.server

import de.menkalian.draco.server.util.logger
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@SpringBootApplication
class ServerApplication {
    @Bean
    fun corsConfiguration() = object : WebMvcConfigurer {
        override fun addCorsMappings(registry: CorsRegistry) {
            super.addCorsMappings(registry)
            logger().info("Configuring CORS")
            registry
                .addMapping("/**")
                .allowedMethods("GET", "PUT", "POST", "DELETE")
                .allowedOrigins("*")
        }
    }
}

fun main(args: Array<String>) {
    runApplication<ServerApplication>(*args)
}
