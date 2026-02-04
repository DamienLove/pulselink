import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.pulselink.wear"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pulselink.wear"
        minSdk = 26
        targetSdk = 35
        versionCode = 19
        versionName = "19"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures { compose = true }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.04.01"))
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.wear.compose:compose-foundation:1.2.1")
    implementation("androidx.wear.compose:compose-material:1.2.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.google.android.gms:play-services-wearable:18.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("com.google.guava:guava:32.1.2-android")

    // Wear OS watch face rendering
    implementation("androidx.wear.watchface:watchface:1.2.0")
    implementation("androidx.wear.watchface:watchface-complications-data:1.2.0")
    implementation("androidx.wear.watchface:watchface-complications-rendering:1.2.0")
    implementation("com.google.firebase:firebase-auth:24.0.1")
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
}

// Automatically copy google-services.json from secure folders if present.
val sourceFile = rootProject.file("PRO-CERTS/google-services-premium.json")
tasks.register("syncGoogleServices", Copy::class) {
    onlyIf { sourceFile.exists() }
    from(sourceFile)
    into(projectDir)
    rename { "google-services.json" }
}

tasks.matching { it.name == "preBuild" }.configureEach {
    dependsOn("syncGoogleServices")
}

tasks.withType(com.google.gms.googleservices.GoogleServicesTask::class.java).configureEach {
    dependsOn("syncGoogleServices")
}
