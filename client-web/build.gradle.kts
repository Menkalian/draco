import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.6.3"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"

    id("com.vaadin")

    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")

    `maven-publish`
}

springBoot {
    buildInfo()
}

java.sourceCompatibility = JavaVersion.VERSION_1_8

repositories {
    mavenCentral()
    maven { setUrl("https://maven.vaadin.com/vaadin-addons") }
}

configurations {
    developmentOnly
    runtimeClasspath {
        extendsFrom(developmentOnly.get())
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("com.vaadin:vaadin-spring-boot-starter")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    // Include coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")

    implementation(project(":client-baseclient")) {
        isTransitive = true
    }
    implementation(project(":shared-data"))
    implementation(project(":shared-utils"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

dependencyManagement {
    imports {
        mavenBom("com.vaadin:vaadin-bom:23.0.0")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.bootJar.configure {
    dependsOn(tasks.vaadinBuildFrontend)
}

vaadin {
    gradle.taskGraph.whenReady {
        productionMode = hasTask(tasks.vaadinBuildFrontend.get())
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifact(tasks.jar)
            artifact(tasks.kotlinSourcesJar)
            artifact(tasks.bootJar)
        }
    }
}
