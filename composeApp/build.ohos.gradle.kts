import com.codingfeline.buildkonfig.compiler.FieldSpec.Type
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.buildkonfig)
}

kotlin {
    ohosArm64 {
        binaries.sharedLib {
            baseName = "kn"
            export(libs.compose.multiplatform.export)
            linkerOpts("-lz")
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.compose.ui.backhandler)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.encoding)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
            implementation(libs.filekit.dialogs)
            implementation(libs.filekit.dialogs.compose)
            implementation(libs.filekit.coil)
            implementation(libs.haze)
        }
        val ohosMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                api(libs.compose.multiplatform.export)
                implementation(libs.ktor.client.curl)
            }
        }
        ohosArm64Main.get().dependsOn(ohosMain)
    }
    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
        optIn.add("androidx.compose.animation.ExperimentalSharedTransitionApi")
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

// Compile only the upstream vector icons used by shared code for this Native target.
val iconSources = configurations.detachedConfiguration(
    dependencies.create("org.jetbrains.compose.material:material-icons-core:1.7.3:sources@jar"),
    dependencies.create("org.jetbrains.compose.material:material-icons-extended:1.7.3:sources@jar"),
).apply { isTransitive = false }
val iconImports = fileTree("src/commonMain/kotlin").matching { include("**/*.kt") }.files
    .flatMap { file ->
        Regex("import (androidx\\.compose\\.material\\.icons\\.[A-Za-z0-9_.]+)")
            .findAll(file.readText()).map { "commonMain/" + it.groupValues[1].replace('.', '/') + ".kt" }.toList()
    }.toSet()
val unpackOhosIcons = tasks.register<Sync>("unpackOhosIcons") {
    from(iconSources.map { zipTree(it) })
    include(iconImports)
    include("META-INF/LICENSE*", "META-INF/NOTICE*")
    into(layout.buildDirectory.dir("generated/ohosIcons"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
kotlin.sourceSets.getByName("commonMain").kotlin.srcDir(unpackOhosIcons)
kotlin.sourceSets.getByName("commonMain").kotlin.srcDir("src/ohosCompatMain/kotlin")


val gitVersionCode = providers.exec {
    commandLine("git", "rev-list", "--count", "--first-parent", "HEAD")
}.standardOutput.asText.map {
    it.trim().toInt()
}

val gitVersionName = providers.exec {
    commandLine("git", "describe", "--tags", "--match", "v[0-9]*")
}.standardOutput.asText.map {
    it.trim().trimStart('v') // Remove prefix 'vx.x.x'
}

val gitVersionNameShort = gitVersionName.map { it.substringBefore("-") }

// Unix ms of HEAD. Must be stable: System.currentTimeMillis() rewrites BuildKonfig.kt
// on every Gradle run and Compose Hot Reload --auto watches that file → infinite reload.
val gitCommitTimeMs = providers.exec {
    commandLine("git", "log", "-1", "--format=%ct")
}.standardOutput.asText.map { it.trim().toLong() * 1000 }

buildkonfig {
    packageName = "world.hachimi.app"
    exposeObjectWithName = "BuildKonfig"

    val props = Properties().apply { load(rootProject.file("local.properties").reader()) }

    defaultConfigs {
        buildConfigField(Type.LONG, "BUILD_TIME", gitCommitTimeMs.get().toString())
        buildConfigField(Type.INT, "VERSION_CODE", gitVersionCode.get().toString())
        buildConfigField(Type.STRING, "VERSION_NAME", gitVersionName.get())

        buildConfigField(Type.STRING, "BUILD_TYPE", "dev")
        buildConfigField(Type.STRING, "APP_PACKAGE_NAME", "world.hachimi.app.dev")
        buildConfigField(Type.STRING, "APP_NAME", "基米天堂 Dev")
        buildConfigField(Type.STRING, "API_BASE_URL", props.getProperty("app.dev.apiBaseUrl"))
        buildConfigField(Type.STRING, "ASSETS_BASE_URL", props.getProperty("app.dev.assetsBaseUrl"))
    }

    defaultConfigs("release") {
        buildConfigField(Type.STRING, "BUILD_TYPE", "release")
        buildConfigField(Type.STRING, "APP_PACKAGE_NAME", "world.hachimi.app")
        buildConfigField(Type.STRING, "APP_NAME", "基米天堂")
        buildConfigField(Type.STRING, "API_BASE_URL", props.getProperty("app.release.apiBaseUrl"))
        buildConfigField(Type.STRING, "ASSETS_BASE_URL", props.getProperty("app.release.assetsBaseUrl"))
    }

    defaultConfigs("beta") {
        buildConfigField(Type.STRING, "BUILD_TYPE", "beta")
        buildConfigField(Type.STRING, "APP_PACKAGE_NAME", "world.hachimi.app.beta")
        buildConfigField(Type.STRING, "APP_NAME", "基米天堂 Beta")
        buildConfigField(Type.STRING, "API_BASE_URL", props.getProperty("app.release.apiBaseUrl"))
        buildConfigField(Type.STRING, "ASSETS_BASE_URL", props.getProperty("app.release.assetsBaseUrl"))
    }
}

tasks.register("printVersions") {
    println(gitVersionName.get() + " " + gitVersionCode.get() + " " + gitVersionNameShort.get())
}

listOf("Debug", "Release").forEach { variant ->
    tasks.register<Copy>("publish${variant}BinariesToHarmonyApp") {
        dependsOn("link${variant}SharedOhosArm64")
        // Compose reads values-*/**.cvr, so package the prepared resources, not the raw XML sources.
        dependsOn("prepareComposeResourcesTaskForCommonMain")
        val binaryDirectory = "build/bin/ohosArm64/${variant.lowercase()}Shared"
        into(rootProject.file("harmonyApp"))
        from("$binaryDirectory/libkn.so") { into("entry/libs/arm64-v8a") }
        from("$binaryDirectory/libkn_api.h") { into("entry/src/main/cpp/include/arm64-v8a") }
        from("build/generated/compose/resourceGenerator/preparedResources/commonMain/composeResources") {
            into("entry/src/main/resources/rawfile/composeResources/hachimiworld.composeapp.generated.resources")
        }
    }
}
