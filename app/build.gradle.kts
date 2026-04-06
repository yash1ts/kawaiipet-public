import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.util.Properties
import java.util.zip.ZipEntry as JZipEntry
import java.util.zip.ZipFile as JZipFile
import java.util.zip.ZipOutputStream as JZipOutputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Supabase: read from repo-root local.properties (same file as sdk.dir) and/or app/local.properties.
// Root is standard; app/local.properties is merged so keys are not missed.
// Use your hosted URL (https://xxxx.supabase.co). Do not use http://localhost from the app —
// on an emulator, localhost is the emulator itself; use https://....supabase.co or http://10.0.2.2:54321 for local CLI.
val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
    project.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val posthogApiKeyProp = (localProperties.getProperty("posthog.apiKey") ?: "").replace("\\", "\\\\").replace("\"", "\\\"")
val posthogHostProp = (localProperties.getProperty("posthog.host") ?: "").replace("\\", "\\\\").replace("\"", "\\\"")
val supabaseUrlProp = (localProperties.getProperty("supabase.url") ?: "").replace("\\", "\\\\").replace("\"", "\\\"")
val supabaseAnonKeyProp = (localProperties.getProperty("supabase.anon.key") ?: "").replace("\\", "\\\\").replace("\"", "\\\"")
// Must match AndroidManifest deep link and be added under Auth → URL Configuration → Redirect URLs in Supabase.
val supabaseAuthRedirectScheme = "com.kawaiipet.app"
val supabaseAuthRedirectHost = "auth-callback"

// Optional Play Store / release signing: create keystore.properties at repo root (gitignored) with:
// storeFile=release.keystore
// storePassword=...
// keyAlias=...
// keyPassword=...
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
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
    compileSdk = 36

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                keyAlias = keystoreProperties.getProperty("keyAlias")!!
                keyPassword = keystoreProperties.getProperty("keyPassword")!!
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile")!!)
                storePassword = keystoreProperties.getProperty("storePassword")!!
            }
        }
    }

    defaultConfig {
        applicationId = "com.kawaiipet.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "POSTHOG_API_KEY", "\"$posthogApiKeyProp\"")
        buildConfigField("String", "POSTHOG_HOST", "\"$posthogHostProp\"")
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrlProp\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKeyProp\"")
        buildConfigField("String", "SUPABASE_AUTH_REDIRECT_SCHEME", "\"$supabaseAuthRedirectScheme\"")
        buildConfigField("String", "SUPABASE_AUTH_REDIRECT_HOST", "\"$supabaseAuthRedirectHost\"")
        manifestPlaceholders["supabaseAuthScheme"] = supabaseAuthRedirectScheme
        manifestPlaceholders["supabaseAuthHost"] = supabaseAuthRedirectHost
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName(
                if (keystorePropertiesFile.exists()) "release" else "debug"
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

    androidResources {
        noCompress += listOf("onnx", "ort")
    }

}

// One ONNX Runtime in the APK: `onnxruntime-android`. Sherpa’s AAR also ships libonnxruntime.so; we remove it so
// libsherpa-onnx-jni + libonnxruntime4j_jni both use the same libonnxruntime.so.
// Pin `libs.versions.toml` onnxruntimeAndroid to Sherpa’s build (v1.12.35 → onnxruntime 1.23.2 in build-android-arm64-v8a.sh).
// https://github.com/k2-fsa/sherpa-onnx/releases
private val sherpaOnnxReleaseVersion = "1.12.35"
private val sherpaOnnxAarFile = layout.projectDirectory.file("libs/sherpa-onnx-$sherpaOnnxReleaseVersion.aar")
private val sherpaOnnxAppAarFile = layout.projectDirectory.file("libs/sherpa-onnx-$sherpaOnnxReleaseVersion-app.aar")

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

tasks.register("prepareSherpaOnnxAppAar") {
    dependsOn("downloadSherpaOnnxAar")
    val src = sherpaOnnxAarFile.asFile
    val dst = sherpaOnnxAppAarFile.asFile
    inputs.file(src)
    outputs.file(dst)
    doLast {
        dst.parentFile?.mkdirs()
        JZipFile(src).use { zf ->
            FileOutputStream(dst).use { fos ->
                JZipOutputStream(fos).use { zos ->
                    for (e in zf.entries()) {
                        if (e.isDirectory) continue
                        if (e.name.startsWith("jni/") && e.name.endsWith("libonnxruntime.so")) continue
                        val outEntry = JZipEntry(e.name).apply { time = e.time }
                        zos.putNextEntry(outEntry)
                        zf.getInputStream(e).use { input -> input.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            }
        }
    }
}

tasks.named("preBuild").configure {
    dependsOn("prepareSherpaOnnxAppAar")
}

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

    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.auth)
    implementation(libs.supabase.functions)
    implementation(libs.ktor.client.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.onnxruntime.android)

    implementation(files(sherpaOnnxAppAarFile.asFile))

    implementation(libs.posthog.android)
}
