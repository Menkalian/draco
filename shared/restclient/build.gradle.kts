plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux"    -> linuxX64("native")
        isMingwX64           -> mingwX64("native")
        else                 -> null
    }

    nativeTarget?.apply {
        binaries {
            sharedLib()
        }
    }
    jvm()
    jvm("jvmExamples")

    val ktorVersion = "1.6.7"
    fun ktor(module: String) = "io.ktor:ktor-$module:$ktorVersion"

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation(kotlin("reflect"))

                implementation(ktor("client-core"))
                implementation(ktor("client-serialization"))
                implementation(ktor("client-logging"))

                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.3.2")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")

                implementation(project(":shared:data"))
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
        val jvmExamplesMain by getting {
            dependsOn(jvmMain)
            kotlin.srcDir("src/examples/kotlin")

            dependencies {
                implementation("ch.qos.logback:logback-classic:1.2.10")
            }
        }
    }
}