plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.depthclock"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.depthclock"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-ktx:1.9.3")
    // Bundled on-device person segmentation (no Google Play services download needed)
    implementation("com.google.mlkit:segmentation-selfie:16.0.0-beta6")
    // General subject segmentation (characters, animals, objects); model is downloaded by Google Play services
    implementation("com.google.android.gms:play-services-mlkit-subject-segmentation:16.0.0-beta1")
}
