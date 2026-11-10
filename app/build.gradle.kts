plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.tasmee3.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tasmee3.app"
        // 26 = Android 8.0. Provisional; re-checked in step 3 against sherpa-onnx requirements.
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        // arm64 = nearly all phones; x86_64 = emulator. Add "armeabi-v7a" for old 32-bit phones (+22 MB).
        ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
    }
    // Keep model files uncompressed inside the APK so they load fast.
    androidResources { noCompress += listOf("onnx") }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    // Fetched by scripts/fetch_assets.sh (not in git). Contains the Kotlin API and the native libraries.
    implementation(files("libs/sherpa-onnx-2.0.1.aar"))
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
