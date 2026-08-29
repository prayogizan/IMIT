plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.uncaan.imit.feature.details"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:network"))
    implementation(project(":core:database"))
    implementation(project(":core:player"))
    implementation(project(":core:download"))
    implementation(project(":core:designsystem"))

    // Koin
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.androidx.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)

    // Navigation
    implementation(libs.navigation.compose)

    // Coroutines
    implementation(libs.coroutines.android)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
}
