plugins {
    id("com.android.application")
}

android {
    namespace = "com.sokachat.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sokachat.app"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.8.0")
}
