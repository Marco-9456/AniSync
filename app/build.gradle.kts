import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.dagger.hilt)
    alias(libs.plugins.apollo)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.anisync.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.anisync.android"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Base app name (can be overridden per build type)
        resValue("string", "app_name", "AniSync")
    }

    signingConfigs {
        create("release") {
            // Read from local keystore.properties file if it exists
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val keystoreProperties = Properties()
                keystoreProperties.load(FileInputStream(keystorePropertiesFile))
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            } else {
                // Fall back to environment variables (for CI)
                storeFile = file(System.getenv("KEYSTORE_FILE") ?: "release.keystore")
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Release-specific config
            buildConfigField("Boolean", "IS_DEBUG_BUILD", "false")
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isDebuggable = true
            // Distinct app name for debug builds (side-by-side installation)
            resValue("string", "app_name", "AniSync Debug")
            // Debug-specific config
            buildConfigField("Boolean", "IS_DEBUG_BUILD", "true")
        }
    }

    // Custom APK naming: AniSync-v1.0.0-release.apk
    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            val appName = "AniSync"
            val versionName = variant.versionName
            val buildType = variant.buildType.name
            output.outputFileName = "${appName}-v${versionName}-${buildType}.apk"
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
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

// Room schema export configuration
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    
    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Kotlin Serialization (for type-safe navigation)
    implementation(libs.kotlinx.serialization.json)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.androidx.compose.animation)
    
    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.compose.runtime)
    ksp(libs.androidx.room.compiler)
    
    implementation(libs.androidx.compose.material3)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.androidx.material3.adaptive.navigation.suite)
    implementation(libs.androidx.ui.text.google.fonts)

    // Apollo GraphQL
    implementation(libs.apollo.runtime)
    implementation(libs.apollo.normalized.cache)
    implementation(libs.apollo.normalized.cache.sqlite)

// Coil
    implementation(libs.coil.compose)

    // MaterialKolor for dynamic M3 color schemes
    implementation(libs.materialkolor)

    // Security Crypto for encrypted prefs
    implementation(libs.security.crypto)

    // WorkManager & Hilt Work
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Glance Widgets
    implementation("androidx.glance:glance:1.2.0-rc01")
    implementation("androidx.glance:glance-appwidget:1.2.0-rc01")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.room.testing)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    
    // LeakCanary for memory leak detection (debug builds only)
    debugImplementation(libs.leakcanary)
}

apollo {
    service("service") {
        packageName.set("com.anisync.android")
        introspection {
            endpointUrl.set("https://graphql.anilist.co")
            schemaFile.set(file("src/main/graphql/schema.graphqls"))
        }
        // Apollo 4 requires explicit opt-in for certain features
        generateKotlinModels.set(true)
    }
}
