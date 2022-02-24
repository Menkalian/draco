rootProject.name = "draco"

// Backend
include(
    ":server"
)

// Frontend
include(
    ":client:android",
    ":client:android:app",
)

// Shared
include(
    ":shared:data",
    ":shared:restclient"
)

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven {
            name = "artifactory-menkalian"
            url = uri("https://artifactory.menkalian.de/artifactory/menkalian")
        }
    }

    // Android Tools Version
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id.startsWith("com.android")) {
                useModule("com.android.tools.build:gradle:7.0.0")
            }
        }
    }
}