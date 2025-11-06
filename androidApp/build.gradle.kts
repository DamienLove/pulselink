import java.util.Properties
import org.gradle.api.Project

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

fun Project.optionalProperty(name: String): String? =
    if (hasProperty(name)) property(name)?.toString()?.takeIf { it.isNotBlank() } else null

fun Properties.optional(key: String): String? =
    getProperty(key)?.takeIf { it.isNotBlank() }

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        load(keystorePropertiesFile.inputStream())
    }
}

val defaultKeystoreFile = rootProject.file("pulselink-upload-2025.jks")

val signingStoreFilePath = project.optionalProperty("android.injected.signing.store.file")
    ?: keystoreProperties.optional("storeFile")
    ?: System.getenv("UPLOAD_KEYSTORE_PATH")
    ?: defaultKeystoreFile.takeIf { it.exists() }?.absolutePath

val signingStorePassword = project.optionalProperty("android.injected.signing.store.password")
    ?: keystoreProperties.optional("storePassword")
    ?: System.getenv("UPLOAD_KEYSTORE_PASSWORD")

val signingKeyAlias = project.optionalProperty("android.injected.signing.key.alias")
    ?: keystoreProperties.optional("keyAlias")
    ?: System.getenv("UPLOAD_KEY_ALIAS")

val signingKeyPassword = project.optionalProperty("android.injected.signing.key.password")
    ?: keystoreProperties.optional("keyPassword")
    ?: System.getenv("UPLOAD_KEY_PASSWORD")

val isSigningConfigured = listOf(
    signingStoreFilePath,
    signingStorePassword,
    signingKeyAlias,
    signingKeyPassword
).all { !it.isNullOrBlank() }

android {
    namespace = "com.pulselink"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    flavorDimensions += "tier"

    signingConfigs {
        create("release") {
            if (isSigningConfigured) {
                storeFile = rootProject.file(signingStoreFilePath!!)
                storePassword = signingStorePassword!!
                keyAlias = signingKeyAlias!!
                keyPassword = signingKeyPassword!!
            } else {
                logger.warn("Release signing configuration is incomplete; provide keystore credentials via keystore.properties, environment variables, or -P android.injected.signing.*")
            }
        }
    }

    defaultConfig {
        applicationId = "com.pulselink"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (isSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    productFlavors {
        create("free") {
            dimension = "tier"
            applicationIdSuffix = ".free"
            versionNameSuffix = "-free"
            manifestPlaceholders += mapOf(
                "admobAppId" to "ca-app-pub-5327057757821609~9533221188"
            )
            buildConfigField("boolean", "ADS_ENABLED", "true")
            buildConfigField("String", "AD_APP_ID", "\"ca-app-pub-5327057757821609~9533221188\"")
            buildConfigField("String", "AD_UNIT_BANNER", "\"ca-app-pub-5327057757821609/3955684775\"")
            buildConfigField("String", "AD_UNIT_INTERSTITIAL", "\"ca-app-pub-5327057757821609/3170992810\"")
            buildConfigField("String", "AD_UNIT_REWARDED_INTERSTITIAL", "\"ca-app-pub-5327057757821609/8428571815\"")
            buildConfigField("String", "AD_UNIT_NATIVE_ADVANCED", "\"ca-app-pub-5327057757821609/2153424615\"")
            buildConfigField("String", "AD_UNIT_APP_OPEN", "\"ca-app-pub-5327057757821609/4210125201\"")
            resValue("string", "app_name", "PulseLink")
        }
        create("pro") {
            dimension = "tier"
            applicationIdSuffix = ".pro"
            versionNameSuffix = "-pro"
            manifestPlaceholders += mapOf(
                "admobAppId" to ""
            )
            buildConfigField("boolean", "ADS_ENABLED", "false")
            buildConfigField("String", "AD_APP_ID", "\"\"")
            buildConfigField("String", "AD_UNIT_BANNER", "\"\"")
            buildConfigField("String", "AD_UNIT_INTERSTITIAL", "\"\"")
            buildConfigField("String", "AD_UNIT_REWARDED_INTERSTITIAL", "\"\"")
            buildConfigField("String", "AD_UNIT_NATIVE_ADVANCED", "\"\"")
            buildConfigField("String", "AD_UNIT_APP_OPEN", "\"\"")
            resValue("string", "app_name", "PulseLink Pro")
        }
    }

    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kapt {
    correctErrorTypes = true
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.04.01"))
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.04.01"))

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.burnoutcrew.composereorderable:reorderable:0.9.6")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.1.0")
    implementation("androidx.hilt:hilt-common:1.1.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-ads:22.6.0")
    implementation("com.google.android.play:integrity:1.5.0")

    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-compiler:2.51.1")
    kapt("androidx.hilt:hilt-compiler:1.1.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
