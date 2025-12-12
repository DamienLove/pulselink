plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.pulselink.beacon"
    compileSdk = 35

    flavorDimensions += "tier"

    productFlavors {
        create("free") {
            dimension = "tier"
            applicationId = "com.pulselink.beacon"
            resValue("string", "app_name", "PulseLink Beacon")
        }
        create("pro") {
            dimension = "tier"
            applicationId = "com.pulselink.beacon.pro"
            resValue("string", "app_name", "PulseLink Beacon Pro")
        }
    }

    defaultConfig {
        applicationId = "com.pulselink.beacon"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes.forEach { type ->
        // Firebase manual init placeholders (override via env variables)
        type.buildConfigField("String", "FIREBASE_API_KEY", "\"${System.getenv("BEACON_FIREBASE_API_KEY") ?: ""}\"")
        type.buildConfigField("String", "FIREBASE_APP_ID", "\"${System.getenv("BEACON_FIREBASE_APP_ID") ?: ""}\"")
        type.buildConfigField("String", "FIREBASE_PROJECT_ID", "\"${System.getenv("BEACON_FIREBASE_PROJECT_ID") ?: ""}\"")
        type.buildConfigField("String", "FIREBASE_SENDER_ID", "\"${System.getenv("BEACON_FIREBASE_SENDER_ID") ?: ""}\"")
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core"))

    implementation(platform("androidx.compose:compose-bom:2024.04.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Firebase (manual init via BeaconApp)
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-auth-ktx:23.0.0")
    implementation("com.google.firebase:firebase-functions-ktx")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.04.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
