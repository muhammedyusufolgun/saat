plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val releaseKeystorePath = System.getenv("MASASAATI_KEYSTORE_FILE")

android {
    namespace = "tr.com.olgun.masasaati"
    compileSdk = 35

    defaultConfig {
        applicationId = "tr.com.olgun.masasaati"
        minSdk = 23
        targetSdk = 35
        versionCode = 8
        versionName = "1.7"
    }

    signingConfigs {
        create("release") {
            if (!releaseKeystorePath.isNullOrBlank()) {
                storeFile = file(releaseKeystorePath)
            }
            storePassword = System.getenv("MASASAATI_KEYSTORE_PASSWORD") ?: ""
            keyAlias = System.getenv("MASASAATI_KEY_ALIAS") ?: ""
            keyPassword = System.getenv("MASASAATI_KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            if (!releaseKeystorePath.isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }
}
