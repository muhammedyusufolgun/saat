plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "tr.com.olgun.masasaati"
    compileSdk = 35
    defaultConfig {
        applicationId = "tr.com.olgun.masasaati"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    kotlinOptions { jvmTarget = "17" }
}
