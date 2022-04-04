import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    // Kotlin
    val kotlinVersion = "1.6.10"

    kotlin("android") version kotlinVersion apply false
    kotlin("jvm") version kotlinVersion apply false
    kotlin("multiplatform") version kotlinVersion apply false
    kotlin("plugin.spring") version kotlinVersion apply false
    kotlin("plugin.jpa") version kotlinVersion apply false
    kotlin("plugin.serialization") version kotlinVersion apply false

    id("com.vaadin") version "23.0.0" apply false

    id("com.android.application") apply false

    id("de.menkalian.vela.keygen") version "1.2.1" apply false
}

allprojects {
    group = "de.menkalian.draco"
    version = "1.0.0"

    repositories {
        mavenCentral()
    }

    // Configure multiplatform projects
    pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
        extensions.getByType(KotlinMultiplatformExtension::class).apply {
            jvm {
                withJava()
            }

            js {
                binaries.library()
                useCommonJs()
                browser()
                nodejs()
            }

            val hostOs = System.getProperty("os.name")
            logger.info("Configuring for build on $hostOs")

            val isMingwX64 = hostOs.startsWith("Windows")
            val nativeTarget = when {
                hostOs == "Mac OS X" -> macosX64("native")
                hostOs == "Linux"    -> linuxX64("native")
                isMingwX64           -> mingwX64("native")
                else                 -> null
            }

            if (nativeTarget == null) {
                logger.error("Native compilation on your OS is not supported.")
            }

            // Configure targets
            nativeTarget?.apply {
                binaries {
                    sharedLib()
                }
            }

            sourceSets {
                getByName("commonMain") {
                    dependencies {
                        implementation(kotlin("stdlib-common"))
                        implementation(kotlin("reflect"))

                        // Include coroutines
                        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")

                        // Include serialization if the plugin is there
                        pluginManager.withPlugin("org.jetbrains.kotlin.plugin.serialization") {
                            implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.3.2")
                            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
                        }
                    }
                }

                getByName("commonTest") {
                    dependencies {
                        implementation(kotlin("test-common"))
                        implementation(kotlin("test-annotations-common"))
                    }
                }
            }
        }
    }

    pluginManager.withPlugin("maven-publish") {
        extensions.getByType(PublishingExtension::class.java).apply {
            repositories {
                maven {
                    url = uri("https://artifactory.menkalian.de/artifactory/draco")
                    name = "artifactory-menkalian"
                    credentials {
                        username = System.getenv("MAVEN_REPO_USER")
                        password = System.getenv("MAVEN_REPO_PASS")
                    }
                }
            }
        }
    }

    tasks.withType(AbstractCopyTask::class) {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}