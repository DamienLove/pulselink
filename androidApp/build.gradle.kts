    import java.io.File
import java.util.Locale
import java.util.Properties
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import com.google.gms.googleservices.GoogleServicesTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

fun Project.optionalProperty(name: String): String? =
    if (hasProperty(name)) property(name)?.toString()?.takeIf { it.isNotBlank() } else null

fun Project.optionalIntProperty(name: String): Int? = optionalProperty(name)?.toIntOrNull()

data class SigningSpec(
    val storeFile: File?,
    val storePassword: String?,
    val keyAlias: String?,
    val keyPassword: String?
) {
    val isConfigured: Boolean =
        storeFile?.exists() == true &&
            !storePassword.isNullOrBlank() &&
            !keyAlias.isNullOrBlank() &&
            !keyPassword.isNullOrBlank()
}

fun Properties.optional(key: String): String? =
    getProperty(key)?.takeIf { it.isNotBlank() }

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        load(keystorePropertiesFile.inputStream())
    }
}

val localPropertiesFile = rootProject.file("local.properties")
val localProperties = Properties().apply {
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

val defaultKeystoreFile = rootProject.file("upload-keystore.jks")

// Optional Maps SDK key (keep out of VCS).
val mapsApiKey = optionalProperty("mapsApiKey")
    ?: localProperties.optional("mapsApiKey")
    ?: System.getenv("MAPS_API_KEY")
    ?: System.getenv("GOOGLE_MAPS_API_KEY")
    ?: ""

// Optional Numlookup API key (keep out of VCS).
val numlookupApiKey = optionalProperty("numlookupApiKey")
    ?: System.getenv("NUMLOOKUP_API_KEY")
    ?: ""
val escapedNumlookupApiKey = numlookupApiKey.replace("\"", "\\\"")

// Optional Numverify API key (caller ID fallback; keep out of VCS).
val numverifyApiKey = optionalProperty("numverifyApiKey")
    ?: System.getenv("NUMVERIFY_API_KEY")
    ?: ""
val escapedNumverifyApiKey = numverifyApiKey.replace("\"", "\\\"")

// Optional IPQualityScore API key (caller ID fallback; keep out of VCS).
val ipqualityscoreApiKey = optionalProperty("ipqualityscoreApiKey")
    ?: System.getenv("IPQUALITYSCORE_API_KEY")
    ?: System.getenv("IPQS_API_KEY")
    ?: ""
val escapedIpqualityscoreApiKey = ipqualityscoreApiKey.replace("\"", "\\\"")

// Optional Rapid Lookup API key (RapidAPI); keep secrets out of VCS.
val rapidLookupApiKey = optionalProperty("rapidLookupApiKey")
    ?: System.getenv("RAPID_LOOKUP_API_KEY")
    ?: System.getenv("RAPIDAPI_KEY")
    ?: ""
val escapedRapidLookupApiKey = rapidLookupApiKey.replace("\"", "\\\"")
val rapidLookupApiHost = optionalProperty("rapidLookupApiHost")
    ?: System.getenv("RAPID_LOOKUP_API_HOST")
    ?: "phone-number-lookup-api.p.rapidapi.com"
val escapedRapidLookupApiHost = rapidLookupApiHost.replace("\"", "\\\"")
val rapidLookupMonthlyCap = optionalIntProperty("rapidLookupMonthlyCap")
    ?: System.getenv("RAPID_LOOKUP_MONTHLY_CAP")?.toIntOrNull()
    ?: 100

// Optional Twilio Lookup credentials (keep out of VCS).
val twilioAccountSid = optionalProperty("twilioAccountSid")
    ?: System.getenv("TWILIO_ACCOUNT_SID")
    ?: ""
val escapedTwilioAccountSid = twilioAccountSid.replace("\"", "\\\"")

val twilioAuthToken = optionalProperty("twilioAuthToken")
    ?: System.getenv("TWILIO_AUTH_TOKEN")
    ?: ""
val escapedTwilioAuthToken = twilioAuthToken.replace("\"", "\\\"")
val twilioLookupMonthlyCap = optionalIntProperty("twilioLookupMonthlyCap")
    ?: System.getenv("TWILIO_LOOKUP_MONTHLY_CAP")?.toIntOrNull()
    ?: 0

// Optional Truecaller API key (RapidAPI Truecaller4); keep secrets out of VCS.
val truecallerApiKey = optionalProperty("truecallerApiKey")
    ?: System.getenv("TRUECALLER_API_KEY")
    ?: System.getenv("TRUECALLER_RAPIDAPI_KEY")
    ?: ""
val escapedTruecallerApiKey = truecallerApiKey.replace("\"", "\\\"")
val truecallerApiHost = optionalProperty("truecallerApiHost")
    ?: System.getenv("TRUECALLER_API_HOST")
    ?: "truecaller4.p.rapidapi.com"
val escapedTruecallerApiHost = truecallerApiHost.replace("\"", "\\\"")

val numlookupMonthlyCap = optionalIntProperty("numlookupMonthlyCap")
    ?: System.getenv("NUMLOOKUP_MONTHLY_CAP")?.toIntOrNull()
    ?: Int.MAX_VALUE
val numverifyMonthlyCap = optionalIntProperty("numverifyMonthlyCap")
    ?: System.getenv("NUMVERIFY_MONTHLY_CAP")?.toIntOrNull()
    ?: Int.MAX_VALUE
val ipqsMonthlyCap = optionalIntProperty("ipqsMonthlyCap")
    ?: System.getenv("IPQS_MONTHLY_CAP")?.toIntOrNull()
    ?: Int.MAX_VALUE
val realcallMonthlyCap = optionalIntProperty("realcallMonthlyCap")
    ?: System.getenv("REALCALL_MONTHLY_CAP")?.toIntOrNull()
    ?: 100

fun Project.signingEnvKey(base: String, flavor: String?): String {
    val suffix = flavor?.uppercase(Locale.US)?.let { "_${it}" }.orEmpty()
    val root = when (base) {
        "storeFile" -> "UPLOAD_KEYSTORE_PATH"
        "storePassword" -> "UPLOAD_KEYSTORE_PASSWORD"
        "keyAlias" -> "UPLOAD_KEY_ALIAS"
        "keyPassword" -> "UPLOAD_KEY_PASSWORD"
        else -> error("Unknown signing field: $base")
    }
    return root + suffix
}

fun Project.resolveSigningValue(
    base: String,
    flavor: String?,
    defaultSupplier: (() -> String?)? = null
): String? {
    val suffix = flavor?.lowercase(Locale.US)
    val flavorSuffix = suffix?.let { ".$it" }.orEmpty()
    val injectedBase = "android.injected.signing.$base"
    return optionalProperty("$injectedBase$flavorSuffix")
        ?: optionalProperty(injectedBase)
        ?: optionalProperty("$base$flavorSuffix")
        ?: optionalProperty(base)
        ?: keystoreProperties.optional(listOfNotNull(suffix, base).joinToString("."))
        ?: keystoreProperties.optional(base)
        ?: System.getenv(signingEnvKey(base, flavor))
        ?: System.getenv(signingEnvKey(base, null))
        ?: defaultSupplier?.invoke()
}

fun Project.buildSigningSpec(flavor: String?): SigningSpec {
    val storePath = resolveSigningValue("storeFile", flavor) {
        if (flavor == null) defaultKeystoreFile.takeIf { it.exists() }?.absolutePath else null
    }
    return SigningSpec(
        storeFile = storePath?.let { rootProject.file(it) },
        storePassword = resolveSigningValue("storePassword", flavor),
        keyAlias = resolveSigningValue("keyAlias", flavor),
        keyPassword = resolveSigningValue("keyPassword", flavor)
    )
}

fun com.android.build.api.dsl.SigningConfig.applySpec(
    spec: SigningSpec,
    label: String,
    logger: org.gradle.api.logging.Logger
) {
    if (!spec.isConfigured) {
        logger.warn("Signing configuration '$label' is incomplete; provide keystore credentials via keystore.properties, environment variables, or -P android.injected.signing.*")
        return
    }
    storeFile = spec.storeFile
    storePassword = spec.storePassword
    keyAlias = spec.keyAlias
    keyPassword = spec.keyPassword
}

val releaseSigningSpec = project.buildSigningSpec(flavor = null)
val freeSigningSpec = project.buildSigningSpec("free")
val proSigningSpec = project.buildSigningSpec("pro")
val premiumSigningSpec = project.buildSigningSpec("premium")

android {
    namespace = "com.pulselink"
    compileSdk = 35
    buildToolsVersion = "35.0.0"
    ndkVersion = "28.0.12433562"

    flavorDimensions += "tier"

    signingConfigs {
        create("release") { applySpec(releaseSigningSpec, "release", logger) }
        if (freeSigningSpec.isConfigured) {
            create("freeRelease") { applySpec(freeSigningSpec, "freeRelease", logger) }
        }
        if (proSigningSpec.isConfigured) {
            create("proRelease") { applySpec(proSigningSpec, "proRelease", logger) }
        }
        if (premiumSigningSpec.isConfigured) {
            create("premiumRelease") { applySpec(premiumSigningSpec, "premiumRelease", logger) }
        }
    }

    defaultConfig {
        applicationId = "com.pulselink"
        minSdk = 26
        targetSdk = 35
        versionCode = 93
        versionName = "93"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        resValue("string", "google_maps_key", mapsApiKey)

        buildConfigField("String", "NUMLOOKUP_API_BASE", "\"https://api.numlookupapi.com/v1\"")
        buildConfigField("String", "NUMLOOKUP_API_KEY", "\"$escapedNumlookupApiKey\"")
        buildConfigField("String", "IPQS_API_BASE", "\"https://www.ipqualityscore.com/api/json/phone\"")
        buildConfigField("String", "IPQS_API_KEY", "\"$escapedIpqualityscoreApiKey\"")
        buildConfigField("String", "NUMVERIFY_API_KEY", "\"$escapedNumverifyApiKey\"")
        buildConfigField("String", "IPQUALITYSCORE_API_KEY", "\"$escapedIpqualityscoreApiKey\"")
        buildConfigField("String", "RAPID_LOOKUP_BASE", "\"https://phone-number-lookup-api.p.rapidapi.com/phone-lookup\"")
        buildConfigField("String", "RAPID_LOOKUP_API_KEY", "\"$escapedRapidLookupApiKey\"")
        buildConfigField("String", "RAPID_LOOKUP_API_HOST", "\"$escapedRapidLookupApiHost\"")
        buildConfigField("int", "RAPID_LOOKUP_MONTHLY_CAP", "${rapidLookupMonthlyCap}")
        buildConfigField("String", "REALCALL_LOOKUP_BASE", "\"https://www.realcall.ai/lookup/search?k=\"")
        buildConfigField("int", "REALCALL_MONTHLY_CAP", "${realcallMonthlyCap}")
        buildConfigField("String", "TWILIO_LOOKUP_BASE", "\"https://lookups.twilio.com/v2/PhoneNumbers\"")
        buildConfigField("String", "TWILIO_ACCOUNT_SID", "\"$escapedTwilioAccountSid\"")
        buildConfigField("String", "TWILIO_AUTH_TOKEN", "\"$escapedTwilioAuthToken\"")
        buildConfigField("int", "TWILIO_LOOKUP_MONTHLY_CAP", "${twilioLookupMonthlyCap}")
        buildConfigField("String", "TRUECALLER_API_KEY", "\"$escapedTruecallerApiKey\"")
        buildConfigField("String", "TRUECALLER_API_HOST", "\"$escapedTruecallerApiHost\"")
        buildConfigField("int", "NUMLOOKUP_MONTHLY_CAP", "${numlookupMonthlyCap}")
        buildConfigField("int", "NUMVERIFY_MONTHLY_CAP", "${numverifyMonthlyCap}")
        buildConfigField("int", "IPQS_MONTHLY_CAP", "${ipqsMonthlyCap}")

        javaCompileOptions {
            annotationProcessorOptions {
                // Export Room schemas for migration tracking; stays under build/ so it won't dirty git.
                arguments += mapOf(
                    "room.schemaLocation" to "$projectDir/build/room-schemas",
                    "room.incremental" to "true",
                    "room.expandProjection" to "true"
                )
            }
        }
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
        }
    }

    productFlavors {
        val releaseConfig = signingConfigs.getByName("release")
        val freeReleaseConfig = signingConfigs.findByName("freeRelease")
        val proReleaseConfig = signingConfigs.findByName("proRelease")
        val premiumReleaseConfig = signingConfigs.findByName("premiumRelease")

        create("free") {
            dimension = "tier"
            applicationId = "com.free.pulselink"
            manifestPlaceholders += mapOf(
                "admobAppId" to "ca-app-pub-5327057757821609~9533221188"
            )
            buildConfigField("boolean", "ADS_ENABLED", "true")
            buildConfigField("boolean", "PRO_FEATURES", "false")
            buildConfigField("boolean", "PREMIUM_FEATURES", "false")
            buildConfigField("boolean", "SUBSCRIPTION_ENABLED", "true")
            buildConfigField("String", "ALERT_RELAY_BASE_URL", "\"https://us-central1-pulselink-prod.cloudfunctions.net\"")
            buildConfigField("String", "AD_APP_ID", "\"ca-app-pub-5327057757821609~9533221188\"")
            buildConfigField("String", "AD_UNIT_BANNER", "\"ca-app-pub-5327057757821609/3955684775\"")
            buildConfigField("String", "AD_UNIT_INTERSTITIAL", "\"ca-app-pub-5327057757821609/3170992810\"")
            buildConfigField("String", "AD_UNIT_REWARDED_INTERSTITIAL", "\"ca-app-pub-5327057757821609/8428571815\"")
            buildConfigField("String", "AD_UNIT_NATIVE_ADVANCED", "\"ca-app-pub-5327057757821609/2153424615\"")
            buildConfigField("String", "AD_UNIT_APP_OPEN", "\"ca-app-pub-5327057757821609/4210125201\"")
            buildConfigField("String", "SUBS_MONTHLY_PRODUCT_ID", "\"pulselink_premium_monthly\"")
            buildConfigField("String", "SUBS_ANNUAL_PRODUCT_ID", "\"pulselink_premium_yearly\"")
            resValue("string", "app_name", "PulseLink Beacon")
            val targetSigning = when {
                freeSigningSpec.isConfigured -> freeReleaseConfig
                releaseSigningSpec.isConfigured -> releaseConfig
                else -> null
            }
            if (targetSigning != null) {
                signingConfig = targetSigning
            }
        }
        create("pro") {
            dimension = "tier"
            applicationIdSuffix = ".pro"
            manifestPlaceholders += mapOf(
                "admobAppId" to ""
            )
            buildConfigField("boolean", "ADS_ENABLED", "false")
            buildConfigField("boolean", "PRO_FEATURES", "true")
            buildConfigField("boolean", "PREMIUM_FEATURES", "false")
            buildConfigField("boolean", "SUBSCRIPTION_ENABLED", "true")        
            buildConfigField("String", "ALERT_RELAY_BASE_URL", "\"https://us-central1-pulselink-prod.cloudfunctions.net\"")
            buildConfigField("String", "AD_APP_ID", "\"\"")
            buildConfigField("String", "AD_UNIT_BANNER", "\"\"")
            buildConfigField("String", "AD_UNIT_INTERSTITIAL", "\"\"")
            buildConfigField("String", "AD_UNIT_REWARDED_INTERSTITIAL", "\"\"")
            buildConfigField("String", "AD_UNIT_NATIVE_ADVANCED", "\"\"")
            buildConfigField("String", "AD_UNIT_APP_OPEN", "\"\"")
            buildConfigField("String", "SUBS_MONTHLY_PRODUCT_ID", "\"pulselink_premium_monthly\"")
            buildConfigField("String", "SUBS_ANNUAL_PRODUCT_ID", "\"pulselink_premium_yearly\"")
            resValue("string", "app_name", "PulseLink Pro")
            val targetSigning = when {
                proSigningSpec.isConfigured -> proReleaseConfig
                releaseSigningSpec.isConfigured -> releaseConfig
                else -> null
            }
            if (targetSigning != null) {
                signingConfig = targetSigning
            }
        }
        create("premium") {
            dimension = "tier"
            applicationIdSuffix = ".premium"
            manifestPlaceholders += mapOf(
                "admobAppId" to ""
            )
            buildConfigField("boolean", "ADS_ENABLED", "false")
            buildConfigField("boolean", "PRO_FEATURES", "true")
            buildConfigField("boolean", "PREMIUM_FEATURES", "true")
            buildConfigField("boolean", "SUBSCRIPTION_ENABLED", "true")
            buildConfigField("String", "ALERT_RELAY_BASE_URL", "\"https://us-central1-pulselink-prod.cloudfunctions.net\"")
            buildConfigField("String", "AD_APP_ID", "\"\"")
            buildConfigField("String", "AD_UNIT_BANNER", "\"\"")
            buildConfigField("String", "AD_UNIT_INTERSTITIAL", "\"\"")
            buildConfigField("String", "AD_UNIT_REWARDED_INTERSTITIAL", "\"\"")
            buildConfigField("String", "AD_UNIT_NATIVE_ADVANCED", "\"\"")
            buildConfigField("String", "AD_UNIT_APP_OPEN", "\"\"")
            buildConfigField("String", "SUBS_MONTHLY_PRODUCT_ID", "\"pulselink_premium_monthly\"")
            buildConfigField("String", "SUBS_ANNUAL_PRODUCT_ID", "\"pulselink_premium_yearly\"")
            resValue("string", "app_name", "PulseLink Premium")
            val targetSigning = when {
                premiumSigningSpec.isConfigured -> premiumReleaseConfig
                proSigningSpec.isConfigured -> proReleaseConfig
                releaseSigningSpec.isConfigured -> releaseConfig
                else -> null
            }
            if (targetSigning != null) {
                signingConfig = targetSigning
            }
        }
    }

    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.7.0-beta02"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        disable += listOf(
            "StateFlowValueCalledInComposition",
            "FlowOperatorInvokedInComposition"
        )
    }


    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes.forEach {
        it.manifestPlaceholders["googleServices"] = "true"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// Automatically copy per-flavor google-services.json from secure folders if present.
listOf(
    "free" to rootProject.file("Free-Certs/google-services.json"),
    "pro" to rootProject.file("PRO-CERTS/google-services.json"),
    "premium" to rootProject.file("PRO-CERTS/google-services-premium.json")
).forEach { (flavor, sourceFile) ->
    tasks.register("sync${flavor.replaceFirstChar { it.titlecase(Locale.US) }}GoogleServices", Copy::class) {
        onlyIf { sourceFile.exists() }
        from(sourceFile)
        into(file("src/$flavor"))
        rename { "google-services.json" }
    }
}

tasks.matching { it.name == "preBuild" }.configureEach {
    dependsOn("syncFreeGoogleServices", "syncProGoogleServices", "syncPremiumGoogleServices")
}

tasks.withType(GoogleServicesTask::class.java).configureEach {
    dependsOn("syncFreeGoogleServices", "syncProGoogleServices", "syncPremiumGoogleServices")
}

kapt {
    correctErrorTypes = true
    arguments {
        arg("room.schemaLocation", "$projectDir/build/room-schemas")
        arg("room.incremental", "true")
        arg("room.expandProjection", "true")
    }
}

// Allow building against dependencies compiled with newer Kotlin metadata (e.g., Play/Ads).
tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xskip-metadata-version-check")
    }
}

dependencies {
        implementation(project(":shared"))
        implementation(platform("androidx.compose:compose-bom:2024.04.01"))
        implementation("com.google.firebase:firebase-auth")
        implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.04.01"))

    implementation(platform("com.google.firebase:firebase-bom:34.7.0"))

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.core:core:1.13.1")
    implementation("com.google.guava:guava:32.1.2-android")
    implementation("androidx.concurrent:concurrent-futures:1.1.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    // Update for Android 15 edge-to-edge handling (avoids deprecated status/nav bar color APIs)
    implementation("com.google.android.material:material:1.13.0")

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

    implementation("androidx.room:room-runtime:2.7.0")
    implementation("androidx.room:room-ktx:2.7.0")
    kapt("androidx.room:room-compiler:2.7.0")

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
    // AdMob + mediation adapters
    implementation("com.google.android.gms:play-services-ads:24.9.0")
    implementation("com.google.ads.mediation:applovin:13.5.1.0")
    implementation("com.google.ads.mediation:inmobi:11.1.0.0")
    implementation("com.google.ads.mediation:ironsource:9.2.0.0")
    implementation("com.google.ads.mediation:facebook:6.21.0.0")
    implementation("com.google.ads.mediation:pangle:7.8.0.8.0")
    implementation("com.google.ads.mediation:vungle:7.5.0.0")
    implementation("com.google.android.gms:play-services-auth:21.1.0")
    implementation("com.google.android.gms:play-services-wearable:18.2.0")
    implementation("com.android.billingclient:billing-ktx:8.2.1")
    implementation("com.google.firebase:firebase-ai")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-functions")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-config")
    implementation("com.google.assistant.appactions:suggestions:1.0.0")
    implementation("com.google.android.play:integrity:1.5.0")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("io.coil-kt:coil-compose:2.6.0")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("com.google.dagger:hilt-android:2.57.2")
    kapt("com.google.dagger:hilt-compiler:2.57.2")
    kapt("androidx.hilt:hilt-compiler:1.1.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.57.2")
    kaptAndroidTest("com.google.dagger:hilt-compiler:2.57.2")
}
