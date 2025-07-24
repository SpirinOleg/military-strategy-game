plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    kotlin("plugin.parcelize")
}

android {
    namespace = "com.example.military_strategy_game"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.military_strategy_game"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    packaging { // Note: it's 'packaging' not 'packagingOptions' in Kotlin DSL for this
        resources {
            excludes += "/META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.androidx.activity.compose)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // ViewModel
    //noinspection GradleDependency
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Koin DI
    //noinspection GradleDependency
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation (libs.koin.androidx.workmanager)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Core modules
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":feature:menu"))
    implementation(project(":feature:shop"))
    implementation(project(":feature:game"))
}