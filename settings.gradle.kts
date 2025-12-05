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
    }
}

rootProject.name = "PulseLink"
include(":androidApp")
include(":wearApp")
include(":shared")
include(":core")
