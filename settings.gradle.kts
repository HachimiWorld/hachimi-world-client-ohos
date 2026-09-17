rootProject.name = "HachimiWorld"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// This branch only builds the OHOS target, so the CPF-published Kotlin/Compose artifacts are the
// only ones needed. Upstream's own version matrix stays in gradle/libs.versions.toml for reference
// (see OHOS_MIGRATION.md) but is not used.
pluginManagement {
    repositories {
        mavenLocal()
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public/")
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    defaultLibrariesExtensionName = "upstreamLibs"
    versionCatalogs {
        create("libs") { from(files("gradle/ohos.versions.toml")) }
    }
    repositories {
        mavenLocal()
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public/")
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// Only the shared module is built here; the runnable app is the HarmonyOS project in harmonyApp/.
// build.ohos.gradle.kts replaces build.gradle.kts because upstream (Kotlin 2.3.21 / Compose 1.11.1)
// and CPF (2.2.21-1.0.0 / 1.9.2-1.0.0) cannot share one build script. The upstream
// composeApp/build.gradle.kts is left untouched as a reference for the next upstream rebase.
include(":composeApp")
rootProject.buildFileName = "build.ohos.gradle.kts"
project(":composeApp").buildFileName = "build.ohos.gradle.kts"
