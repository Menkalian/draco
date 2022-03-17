rootProject.name = "draco"

// Backend
include(
    ":server-server"
)

// Shared
include(
    ":shared-data",
    ":shared-utils",
)

// Frontend
include(
    ":client-baseclient",

    ":client:android",
    ":client:android:app",

    ":client-web",
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