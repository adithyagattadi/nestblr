plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.nestblr"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.nestblr"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Emulator → host machine: 192.168.1.7 is the Android emulator's alias for your Mac's localhost
        // Physical device on same WiFi: use your Mac's LAN IP (e.g. http://192.168.1.x:8080/)
        // Production: change to your deployed backend URL
        buildConfigField("String", "BASE_URL", "\"https://nestblr-backend.onrender.com/\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        // AGP 8.7.3's bundled lint engine ships an older Kotlin Analysis API
        // than the detectors in newer Compose / Lifecycle libraries expect
        // ("Found class Ka...Call, but interface was expected"). Several
        // detectors hard-crash when invoked; disable the known ones.
        disable += setOf(
            "FlowOperatorInvokedInComposition",
            "NullSafeMutableLiveData",
            "RememberInComposition",
        )
        abortOnError = false
        checkReleaseBuilds = false
    }
}

// Skip lint tasks entirely — AGP 8.7.3 lint engine is incompatible with the
// Compose/Lifecycle detectors shipped via Compose BOM 2026.05 + Kotlin 2.1.21,
// and various detectors hard-crash with IncompatibleClassChangeError.
// The app still compiles and assembles; only static analysis is skipped.
tasks.matching { it.name.startsWith("lint") }.configureEach {
    enabled = false
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.compose.ui.text.google.fonts)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization.converter)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)

    // Image loading
    implementation(libs.coil.compose)

    // Location
    implementation(libs.play.services.location)

    // Map
    implementation(libs.osmdroid.android)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.coroutines.play.services)
}
