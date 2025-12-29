plugins {
    id("com.android.application")
    kotlin("android")
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.pulselink.beacon"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "com.pulselink.beacon"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // AdMob: Beacon free
        manifestPlaceholders["admobAppId"] = "ca-app-pub-5327057757821609~6125230037"
        buildConfigField("String", "AD_UNIT_BANNER", "\"ca-app-pub-5327057757821609/8371301189\"")
        buildConfigField("String", "AD_UNIT_INTERSTITIAL", "\"ca-app-pub-5327057757821609/6922488144\"")
        buildConfigField("String", "AD_UNIT_NATIVE", "\"ca-app-pub-5327057757821609/7042919400\"")
        buildConfigField("String", "AD_UNIT_APP_OPEN", "\"ca-app-pub-5327057757821609/5022248965\"")
        buildConfigField("Boolean", "PREMIUM_SEARCH", "false")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.7.0-beta02"
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.04.01"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-process:2.7.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation(project(":shared"))

    implementation("com.google.android.gms:play-services-ads:22.6.0")

    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
