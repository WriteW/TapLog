@file:Suppress("DEPRECATION")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.roroi.taplog"
    compileSdk = 36 // 保持你的新版本
    defaultConfig {
        applicationId = "com.roroi.taplog"
        minSdk = 23 // **重要**: 使用 Unity 项目的 minSdkVersion (22)，因为它比你原来的 (21) 更高
        targetSdk = 36 // 保持你的新版本
        versionCode = 7
        versionName = "1.12"

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
        // **【添加】**：为 debug 构建类型添加 jniDebuggable
        // 这对于调试 Unity 的原生代码非常重要
        debug {
            isJniDebuggable = true
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
}

dependencies {
    // **【添加】**：这是最重要的依赖，将 unityLibrary 模块包含进来

    // --- 你现有的其他依赖项保持不变 ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.accompanist.flowlayout.v0301)
    implementation(libs.kotlin.reflect)
    implementation(libs.coil.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.android.image.cropper)
    //noinspection NewerVersionAvailable,UseTomlInstead
    implementation("dev.chrisbanes.haze:haze:0.7.3")
    //noinspection NewerVersionAvailable,UseTomlInstead
    implementation("dev.chrisbanes.haze:haze-materials:0.7.3")
    implementation(libs.androidx.material.icons.extended)
}