@file:Suppress("UnstableApiUsage")

import com.android.build.api.artifact.ArtifactTransformationRequest
import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.FilterConfiguration
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.dagger.hilt)
    alias(libs.plugins.apollo)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

// Map unique integers to each architecture for dynamic version codes
val abiCodes = mapOf("armeabi-v7a" to 1, "arm64-v8a" to 2, "x86" to 3, "x86_64" to 4)

abstract class RenameApksTask : DefaultTask() {
    @get:InputDirectory
    abstract val inputApkFolder: DirectoryProperty

    @get:OutputDirectory
    abstract val outputApkFolder: DirectoryProperty

    @get:Input
    abstract val appName: Property<String>

    @get:Input
    abstract val flavorSuffix: Property<String>

    @get:Input
    abstract val buildTypeName: Property<String>

    @get:Input
    abstract val fallbackVersionName: Property<String>

    @get:Internal
    abstract val apkTransformationRequest: Property<ArtifactTransformationRequest<RenameApksTask>>

    @TaskAction
    fun renameApks() {
        apkTransformationRequest.get().submit(this) { builtArtifact ->
            val sourceFile = builtArtifact.path.toFile()
            val abi =
                builtArtifact.filters
                    .firstOrNull { it.filterType == FilterConfiguration.FilterType.ABI }
                    ?.identifier ?: "universal"
            val versionName = builtArtifact.versionName ?: fallbackVersionName.get()
            val renamedFileName =
                "${appName.get()}${flavorSuffix.get()}-v${versionName}-${abi}-${buildTypeName.get()}.apk"
            val outputFile = outputApkFolder.file(renamedFileName).get().asFile
            sourceFile.copyTo(outputFile, overwrite = true)
            outputFile
        }
    }
}

android {
    namespace = "com.anisync.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.anisync.android"
        minSdk = 26
        targetSdk = 36
        versionCode = 16
        versionName = "1.8.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        resValue("string", "app_name", "AniSync")
    }

    // Strip AGP-injected dependency-metadata signing block so F-Droid's
    // Binaries reproducibility check accepts the upstream-signed APK.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    // Split configuration to create a separate APK for each ABI
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            isUniversalApk =
                true // Creates a fat APK with all ABIs for users who don't know their phone's specs
        }
    }

    // Automatically handle Version Code math for ABI splits
    androidComponents {
        onVariants { variant ->
            variant.outputs.forEach { output ->
                val abiFilter =
                    output.filters.find { it.filterType == FilterConfiguration.FilterType.ABI }?.identifier
                val baseAbiCode = abiCodes[abiFilter]
                if (baseAbiCode != null) {
                    // E.g., if versionCode is 1, arm64 (2) becomes 12.
                    // This ensures the device always prefers the optimized split over the universal APK.
                    output.versionCode.set((output.versionCode.get() ?: 0) * 10 + baseAbiCode)
                }
            }

            val channelFlavor = variant.productFlavors.firstOrNull { it.first == "channel" }?.second
            val flavorSuffixValue =
                channelFlavor
                    ?.takeIf { it != "stable" }
                    ?.let { "-$it" }
                    ?: ""
            val versionNameFallback = variant.outputs.firstOrNull()?.versionName?.orNull ?: "unknown"
            val variantNameTitle =
                variant.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

            val renameApksTask = tasks.register<RenameApksTask>("rename${variantNameTitle}Apks") {
                appName.set("AniSync")
                flavorSuffix.set(flavorSuffixValue)
                buildTypeName.set(variant.buildType)
                fallbackVersionName.set(versionNameFallback)
            }

            val renameRequest =
                variant.artifacts
                    .use(renameApksTask)
                    .wiredWithDirectories(
                        RenameApksTask::inputApkFolder,
                        RenameApksTask::outputApkFolder,
                    ).toTransformMany(SingleArtifact.APK)

            renameApksTask.configure {
                apkTransformationRequest.set(renameRequest)
            }
        }
    }

    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val keystoreProperties = Properties()
                keystoreProperties.load(FileInputStream(keystorePropertiesFile))
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            } else {
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
            buildConfigField("Boolean", "IS_DEBUG_BUILD", "false")
            signingConfig = signingConfigs.getByName("release")
            vcsInfo { include = false }
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isDebuggable = true
            resValue("string", "app_name", "AniSync Debug")
            buildConfigField("Boolean", "IS_DEBUG_BUILD", "true")
        }
    }

    // Set up Product Flavors
    flavorDimensions += "channel"

    productFlavors {
        create("stable") {
            dimension = "channel"
            isDefault = true
        }

        create("preview") {
            dimension = "channel"
            applicationIdSuffix = ".preview"
            versionNameSuffix = "-preview"
            // This renames the app on the phone's home screen so you can tell them apart!
            resValue("string", "app_name", "AniSync Preview")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
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

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.hilt.android)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.compose.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.compose.material3)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.material3.adaptive.navigation.suite)
    implementation(libs.apollo.runtime)
    implementation(libs.apollo.normalized.cache)
    implementation(libs.apollo.normalized.cache.sqlite)
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.coil.svg)
    implementation(libs.materialkolor)
    implementation(libs.jsoup)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.security.crypto)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.androidx.glance)
    implementation(libs.androidx.glance.appwidget.v120rc01)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.reorderable)
    implementation(libs.okhttp)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.room.testing)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    debugImplementation(libs.leakcanary)
}

apollo {
    service("service") {
        packageName.set("com.anisync.android")
        introspection {
            endpointUrl.set("https://graphql.anilist.co")
            schemaFile.set(file("src/main/graphql/schema.graphqls"))
        }
        generateKotlinModels.set(true)
        mapScalar("Json", "kotlin.Any", "com.apollographql.apollo.api.AnyAdapter")
        mapScalar("CountryCode", "kotlin.String", "com.apollographql.apollo.api.StringAdapter")
        mapScalar("FuzzyDateInt", "kotlin.Int", "com.apollographql.apollo.api.IntAdapter")
    }
}
