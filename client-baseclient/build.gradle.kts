plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")

    `maven-publish`
}

kotlin {
    val ktorVersion = "1.6.7"
    fun ktor(module: String) = "io.ktor:ktor-$module:$ktorVersion"

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(ktor("client-core"))
                implementation(ktor("client-serialization"))
                implementation(ktor("client-logging"))
                implementation(ktor("client-websockets"))

                implementation(project(":shared-data"))
                implementation(project(":shared-utils"))
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(ktor("client-js"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(ktor("client-cio"))
                implementation("org.slf4j:slf4j-api:1.7.36")
            }
        }
        val nativeMain by getting {
            dependencies {
                implementation(ktor("client-curl"))
            }
        }
//        val jvmExamplesMain by getting {
//            dependsOn(jvmMain)
//            kotlin.srcDir("src/examples/kotlin")
//
//            dependencies {
//                implementation("ch.qos.logback:logback-classic:1.2.10")
//            }
//        }
    }
}

afterEvaluate {
    // Build fat jar, since transitive dependencies do not work properly at the moment
    tasks.getByName("jvmJar", Jar::class) {
        from(
            configurations
                .getByName("jvmRuntimeClasspath")
                .map { if (it.isDirectory) it else zipTree(it) }
        )
    }

    publishing {
    }
}
