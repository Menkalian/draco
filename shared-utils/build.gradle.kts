plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
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
}
