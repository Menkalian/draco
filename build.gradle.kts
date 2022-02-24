plugins {
    // Kotlin
    val kotlinVersion = "1.6.10"

    kotlin("android") version kotlinVersion apply false
    kotlin("jvm") version kotlinVersion apply false
    kotlin("multiplatform") version kotlinVersion apply false
    kotlin("plugin.spring") version kotlinVersion apply false
    kotlin("plugin.jpa") version kotlinVersion apply false
    kotlin("plugin.serialization") version kotlinVersion apply false

    id("com.android.application") apply false

    id("de.menkalian.vela.keygen") version "1.2.1" apply false
}

allprojects {
    group = "de.menkalian.draco"
    version = "1.0.0"

    repositories {
        mavenCentral()
    }
}