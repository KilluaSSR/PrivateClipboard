plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "killua.dev.privateclipboard"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "killua.dev.privateclipboard"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.arsenal.core)
    implementation(libs.arsenal.logging)
    implementation(libs.arsenal.spi)
    implementation(libs.arsenal.data)
    implementation(libs.arsenal.navigation)
    implementation(libs.arsenal.navigation.annotations)
    implementation(libs.arsenal.error)
    implementation(libs.arsenal.material)
    implementation(libs.arsenal.permission)
    implementation(libs.arsenal.update)
    implementation(libs.arsenal.utils)
    ksp(libs.arsenal.navigation.processor)
    ksp(libs.arsenal.spi.processor)
    ksp(libs.arsenal.gate.processor)

    // Xposed (YukiHookAPI)
    implementation(libs.yukihookapi.api)
    compileOnly(libs.xposed.api)
    ksp(libs.yukihookapi.ksp.xposed)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
