import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.6.3"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"

    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")

    id("de.menkalian.vela.keygen")
}

springBoot {
    buildInfo()
}

java.sourceCompatibility = JavaVersion.VERSION_1_8

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    implementation(project(":shared:data"))

    implementation("org.jetbrains.exposed:exposed-core:0.37.3")
    implementation("org.jetbrains.exposed:exposed-dao:0.37.3")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.37.3")

    // Database Drivers
    runtimeOnly("org.xerial:sqlite-jdbc:3.36.0.3")
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client:3.0.3")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

keygen {
    targetPackage = "de.menkalian.draco.variables"
    finalLayerAsString = true
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
