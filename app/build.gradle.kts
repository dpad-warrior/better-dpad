plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val versionFile = rootProject.file("version.txt")
val semver = versionFile.readText().trim()
val versionCore = semver.split("-")[0]
val (major, minor, patch) = versionCore.split(".").map { it.toInt() }
val computedVersionCode = major * 10000 + minor * 100 + patch

android {
    namespace = "com.dpadwarrior.betterdpad"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.dpadwarrior.betterdpad"
        minSdk = 27
        targetSdk = 36
        versionCode = computedVersionCode
        versionName = "0.1.0-alpha" // x-release-please-version

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
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
