import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
    id("com.google.firebase.crashlytics")
}

val keystorePropsFile = rootProject.file("ringersong-keystore/keystore.properties")

val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) {
        keystorePropsFile.inputStream().use(this::load)
    }
}

android {
    namespace = "com.RingerSong.free"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.RingerSong.free"
        minSdk = 35
        targetSdk = 35
        versionCode = 17
        versionName = "4.6"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["redirectSchemeName"] = "com.RingerSong.free"
        manifestPlaceholders["redirectHostName"] = "callback"

        // RapidAPI Configuration for Spotify, YouTube Music, and Truecaller
        val localProps = Properties().apply {
            val localPropsFile = rootProject.file("local.properties")
            if (localPropsFile.exists()) {
                localPropsFile.inputStream().use(this::load)
            }
        }

        buildConfigField(
            "String",
            "RAPIDAPI_KEY",
            "\"${localProps.getProperty("rapidapi.key", "")}\""
        )
        buildConfigField("String", "RAPIDAPI_SPOTIFY_HOST",
            "\"spotify-downloader9.p.rapidapi.com\"")
        buildConfigField("String", "RAPIDAPI_YOUTUBE_HOST",
            "\"youtube-music-api-yt.p.rapidapi.com\"")
        buildConfigField("String", "RAPIDAPI_TRUECALLER_HOST",
            "\"truecaller4.p.rapidapi.com\"")
    }

    signingConfigs {
        if (keystorePropsFile.exists()) {
            create("release") {
                val storeFilePath = requireNotNull(keystoreProps.getProperty("storeFile")) {
                    "Missing storeFile in keystore.properties"
                }
                storeFile = rootProject.file(storeFilePath)
                storePassword = requireNotNull(keystoreProps.getProperty("storePassword")) {
                    "Missing storePassword in keystore.properties"
                }
                keyAlias = requireNotNull(keystoreProps.getProperty("keyAlias")) {
                    "Missing keyAlias in keystore.properties"
                }
                keyPassword = requireNotNull(keystoreProps.getProperty("keyPassword")) {
                    "Missing keyPassword in keystore.properties"
                }
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            if (keystorePropsFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
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
        buildConfig = true
    }
}

repositories {
    google()
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation(files("libs/spotify-app-remote-release-0.8.0.aar"))
    implementation(project(":shared"))
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-analytics")
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.jsoup)
    implementation(libs.play.services.ads)
    implementation(libs.youtubedl.android)
    implementation(libs.youtubedl.android.ffmpeg)
    
    implementation("com.spotify.android:auth:2.1.1")

    implementation("com.google.code.gson:gson:2.10.1")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
