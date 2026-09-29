pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.10.1"
        id("com.android.library") version "8.10.1"
        id("org.jetbrains.kotlin.android") version "2.1.20"
        kotlin("jvm") version "2.1.20"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "goreecloud-gallery-native"
include(":core", ":android-adapter", ":app")
