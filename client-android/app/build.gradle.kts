plugins {
    id("com.android.application")
    kotlin("android")
}

fun getVersionCode(): Int {
    val projTxt = project.version
    return projTxt
        .toString()
        .split(".")
        .mapIndexed { idx, str -> Math.pow(100.toDouble(), idx.toDouble()) * (str.toIntOrNull() ?: 0) }
        .sum()
        .toInt()
}

android {
    setCompileSdkVersion(31)

    defaultConfig {
        applicationId = "de.menkalian.draco"
        minSdk = 26
        targetSdk = 31

        versionCode = getVersionCode()
        versionName = project.version.toString()
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("SIGNING_KEYSTORE_LOCATION") ?: "keystore.jks")
            storePassword = System.getenv("SIGNING_KEYSTORE_PASS")
            keyAlias = System.getenv("SIGNING_KEY_ALIAS")
            keyPassword = System.getenv("SIGNING_KEY_PASS")

            this.enableV1Signing = true
            this.enableV2Signing = true
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".dbg"
            resValue("string", "app_name", "Draco")
        }
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release")!!
            resValue("string", "app_name", "QuizPoker")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
}
