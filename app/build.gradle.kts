import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

configurations.configureEach {
    resolutionStrategy {
        // Compose BOM can pull core-ktx 1.17+ which requires compileSdk 36 / AGP 8.9.1+
        force("androidx.core:core-ktx:1.15.0")
        force("androidx.core:core:1.15.0")
    }
}

android {
    namespace = "com.kawaiipet.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kawaiipet.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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
    }
}

// Official k2fsa AAR (16 KB page-size–aligned native libs). Not on Maven Central; fetched on first build.
// See: https://github.com/k2-fsa/sherpa-onnx/releases
private val sherpaOnnxReleaseVersion = "1.12.35"
private val sherpaOnnxAarFile = layout.projectDirectory.file("libs/sherpa-onnx-$sherpaOnnxReleaseVersion.aar")

tasks.register("downloadSherpaOnnxAar") {
    val out = sherpaOnnxAarFile.asFile
    val url =
        "https://github.com/k2-fsa/sherpa-onnx/releases/download/v$sherpaOnnxReleaseVersion/" +
            "sherpa-onnx-$sherpaOnnxReleaseVersion.aar"
    outputs.file(out)
    doLast {
        if (out.exists()) return@doLast
        out.parentFile.mkdirs()
        URI(url).toURL().openStream().use { input ->
            out.outputStream().use { output -> input.copyTo(output) }
        }
    }
}

tasks.named("preBuild").configure { dependsOn("downloadSherpaOnnxAar") }

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.core.ktx)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.service)

    implementation(libs.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.datastore.preferences)

    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    implementation(libs.lottie.compose)

    implementation(libs.commons.compress)

    implementation(libs.google.ai)

    implementation(files(sherpaOnnxAarFile.asFile))
}
