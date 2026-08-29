plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.uncaan.imit.core.download"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:database"))

    // Koin
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)

    // WorkManager
    implementation(libs.work.runtime)

    // OkHttp (for download streams)
    implementation(libs.okhttp)

    // Coroutines
    implementation(libs.coroutines.android)

    // Testing
    testImplementation(libs.junit)
}
