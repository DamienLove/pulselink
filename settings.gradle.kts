pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/gradle-plugin")
    }
}

dependencyResolutionManagement {
    // Allow project-level repositories (cocoapods adds an Ivy repo for local pods)
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/gradle-plugin")
        exclusiveContent {
            forRepository {
                maven("https://artifact.bytedance.com/repository/pangle")
            }
            filter {
                includeGroup("com.pangle.global")
                includeGroup("com.bytedance.sdk")
                includeGroup("com.bytedance")
            }
        }
        exclusiveContent {
            forRepository {
                maven("https://android-sdk.is.com")
            }
            filter {
                includeGroup("com.ironsource.sdk")
                includeGroup("com.ironsource")
            }
        }
    }
}

rootProject.name = "PulseLink"
include(":androidApp")
include(":wearApp")
include(":shared")
include(":ringersong")
include(":beaconApp")
