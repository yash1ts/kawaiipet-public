import java.net.URI
import java.util.Properties

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

/**
 * Fetches Sherpa-compatible Moonshine Tiny EN (quantized / int8-style) and Kitten Nano EN v0.2 into assets.
 * Hugging Face Moonshine int8 + KittenML 0.8 ONNX are not Sherpa-JNI compatible; see BundledVoiceModels.kt.
 */
tasks.register("prepareBundledVoiceModels") {
    val moonUrl =
        "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/" +
            "sherpa-onnx-moonshine-tiny-en-quantized-2026-02-27.tar.bz2"
    val kittenUrl =
        "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kitten-nano-en-v0_2-fp16.tar.bz2"

    val moonTar = layout.buildDirectory.file("tmp/bundled-voice/moonshine.tar.bz2").get().asFile
    val kittenTar = layout.buildDirectory.file("tmp/bundled-voice/kitten.tar.bz2").get().asFile
    val sttOut = layout.projectDirectory.dir("src/main/assets/voice_models/stt").asFile
    val ttsOut =
        layout.projectDirectory.dir("src/main/assets/voice_models/tts/kitten-nano-en-v0_2-fp16").asFile
    val revisionFile = layout.projectDirectory.file("src/main/assets/voice_models/REVISION").asFile

    outputs.dir(sttOut)
    outputs.dir(ttsOut)
    outputs.file(revisionFile)

    doLast {
        moonTar.parentFile?.mkdirs()

        fun downloadIfNeeded(url: String, dest: File) {
            if (dest.exists() && dest.length() > 512L * 1024L) return
            dest.parentFile?.mkdirs()
            URI(url).toURL().openStream().use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
        }
        downloadIfNeeded(moonUrl, moonTar)
        downloadIfNeeded(kittenUrl, kittenTar)

        val moonExtractRoot = layout.buildDirectory.dir("tmp/bundled-voice/moonshine_extract").get().asFile
        moonExtractRoot.deleteRecursively()
        moonExtractRoot.mkdirs()
        exec {
            commandLine("tar", "-xjf", moonTar.absolutePath, "-C", moonExtractRoot.absolutePath)
        }
        val moonInner = moonExtractRoot.listFiles()?.singleOrNull()
            ?: error("prepareBundledVoiceModels: expected one root directory in moonshine archive")
        sttOut.mkdirs()
        copy {
            from(moonInner) {
                include("encoder_model.ort", "decoder_model_merged.ort", "tokens.txt")
            }
            into(sttOut)
        }

        val kittenExtractRoot = layout.buildDirectory.dir("tmp/bundled-voice/kitten_extract").get().asFile
        kittenExtractRoot.deleteRecursively()
        kittenExtractRoot.mkdirs()
        exec {
            commandLine("tar", "-xjf", kittenTar.absolutePath, "-C", kittenExtractRoot.absolutePath)
        }
        val kittenInner = kittenExtractRoot.listFiles()?.singleOrNull()
            ?: error("prepareBundledVoiceModels: expected one root directory in kitten archive")
        ttsOut.deleteRecursively()
        copy {
            from(kittenInner)
            into(ttsOut)
        }

        revisionFile.parentFile?.mkdirs()
        revisionFile.writeText("1")
    }
}

tasks.named("preBuild").configure {
    dependsOn("downloadSherpaOnnxAar", "prepareBundledVoiceModels")
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

    implementation(files(sherpaOnnxAarFile.asFile))

    implementation(libs.posthog.android)
}
