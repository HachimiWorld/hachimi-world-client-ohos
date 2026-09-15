import com.android.SdkConstants
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import java.util.*

plugins {
    // Kotlin Commmon
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinx.atomicfu)
    alias(libs.plugins.kotlinSerialization)

    // Common
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.ksp)

    // Android
    alias(libs.plugins.androidApplication)

    // JVM
    alias(libs.plugins.composeHotReload)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    /*listOf(
        iosArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
        iosTarget.compilations.getByName("main") {
            // Add the NSKeyValueObserving interface so we can use KVO in kotlin native
            // https://proandroiddev.com/leveraging-key-value-observing-kvo-in-kotlin-multiplatform-kmp-for-ios-231519e5c1ff
            // FIXME: Enabling this will get KLIB resolver error
//            val nskeyvalueobserving by cinterops.creating
        }
    }*/

//    jvm()
/*
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName.set("composeApp")
        browser {
            val rootDirPath = project.rootDir.path
            val projectDirPath = project.projectDir.path
            commonWebpackConfig {
                outputFileName = "composeApp.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    static = (static ?: mutableListOf()).apply {
                        // Serve sources to debug inside browser
                        add(rootDirPath)
                        add(projectDirPath)
                    }
                }
            }
        }
        compilerOptions {
            freeCompilerArgs.add("-Xwasm-attach-js-exception")
        }
        binaries.executable()
    }*/

    // Harmony OS
    listOf(
        ohosArm64(), /*ohosX64()*/
    ).forEach { ohosTarget ->
        ohosTarget.binaries.sharedLib {
            baseName = "kn"
            export(libs.compose.multiplatform.export)
            linkerOpts("-lz")
            // 渲染模式
            // 背景：当 libkn.so 为旧编译产物时，其 DT_NEEDED 可能缺少以下库（正确构建时
            // NativeTasksConfiguration.kt 已通过 -l 选项将它们写入 DT_NEEDED）。
            // 在 build.gradle.kts 中统一补全，避免在 CMakeLists.txt 中硬编码。
            val rendererBackend = rootProject.findProperty("rendererBackend")?.toString() ?: "fusion-renderer"
            if (rendererBackend == "fusion-renderer") {
                linkerOpts(
                    "-lnative_drawing",    // OH_Drawing_*（字体、绘制）
                    "-limage_source",       // OH_ImageSourceNative_*（图像解码）
                    "-lpixelmap",           // OH_PixelMap_*
                    "-lpixelmap_ndk.z",     // OH_PixelMapNdk_*
                    "-lnative_window",      // OH_NativeWindow_*
                    "-lace_napi.z",         // N-API
                    "-lhilog_ndk.z",        // HiLog 日志
                    "-lhitrace_ndk.z",      // HiTrace 性能追踪
                    "-luv",                 // libuv 事件循环
                    "-lunwind",             // 栈展开
                    "-licu",               // ICU 文本处理
                )
            }
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.browser)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.media3.session)
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.media3.exoplayer.dash)

            implementation(libs.koin.android)
            implementation(libs.room.runtime)

            implementation(libs.ktor.client.cio)
            implementation(libs.androidx.datastore.preferences)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.compose.ui.backhandler)
//            implementation(libs.androidx.lifecycle.viewmodelCompose)
//            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.compose.viewmodelNavigation)

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

        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        /*jvmMain.dependencies {
            implementation(compose.desktop.currentOs) {
                exclude("org.jetbrains.compose.material")
            }
            implementation(libs.kotlinx.coroutinesSwing)

            implementation(libs.ktor.client.cio)
            implementation(libs.logback)

            implementation(libs.androidx.datastore.preferences)
//            implementation(libs.room.runtime)
//            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.mp3spi)
            implementation(libs.jflac)
        }
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.cio)
            implementation(libs.kotlinx.browser)
            implementation(npm("howler", "2.2.4"))
        }*/
        /*listOf(iosArm64Main).forEach {
            it.dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }*/

        val ohosMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                api(libs.compose.multiplatform.export)
                implementation(libs.ktor.client.curl)
            }
        }
        val ohosArm64Main by getting {
            dependsOn(ohosMain)
        }
        /*val ohosX64Main by getting {
            dependsOn(ohosMain)
        }*/
    }

    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
//    add("kspJvm", libs.room.compiler)
//    add("kspAndroid", libs.room.compiler)
//    add("kspIosSimulatorArm64", libs.room.compiler)
//    add("kspIosX64", libs.room.compiler)
//    add("kspIosArm64", libs.room.compiler)
}

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

android {
    namespace = "world.hachimi.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "world.hachimi.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = 35
        versionCode = gitVersionCode.get()
        versionName = gitVersionName.get()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        if (System.getenv("IS_CI") == "true") {
            register("release") {
                storeFile = file(System.getenv("ANDROID_KEYSTORE_FILE"))
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            if (System.getenv("IS_CI") == "true") {
                signingConfig = signingConfigs.getByName("release")
            }
            resValue("string", "app_name", "@string/app_name_base")
        }
        debug {
            applicationIdSuffix = ".dev"
//            resValue("string", "app_name", "@string/app_name_dev")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "world.hachimi.app.MainKt"

        val flavor = project.findProperty("buildkonfig.flavor")
        when (flavor) {
            "release" -> nativeDistributions {
                targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
                packageName = "Hachimi World"
                packageVersion = gitVersionNameShort.get()
                modules("jdk.unsupported", "java.naming")

                windows {
                    upgradeUuid = "1544B476-25C9-4A01-705E-B374B14B2F1B"
                    perUserInstall = true
                    dirChooser = false
                    shortcut = true
                    menu = true
                    iconFile.set(rootProject.file("icons/icon.ico"))
                }
                macOS {
                    appCategory = "public.app-category.entertainment"
                    packageName = "基米天堂"
                    dockName = "基米天堂"
                    bundleID = "world.hachimi.app"
                    iconFile.set(rootProject.file("icons/icon.icns"))
                }
                linux {
                    packageName = "hachimi-world" // Linux does not support Chinese characters
                    iconFile.set(rootProject.file("icons/icon.png"))
                }
            }
            else -> nativeDistributions {
                targetFormats(TargetFormat.Dmg, TargetFormat.Exe, TargetFormat.Deb)
                packageName = "Hachimi World Dev"
                packageVersion = gitVersionNameShort.get()
                modules("jdk.unsupported", "java.naming")

                windows {
                    upgradeUuid = "8AD88FC9-A6A2-478D-9E80-FF735EE15785"
                    perUserInstall = true
                    dirChooser = false
                    shortcut = true
                    menu = true
                    iconFile.set(rootProject.file("icons/icon.ico"))
                }
                macOS {
                    packageName = "基米天堂 Dev"
                    bundleID = "world.hachimi.app.dev"
                    appCategory = "public.app-category.entertainment"
                    iconFile.set(rootProject.file("icons/icon.icns"))
                }
                linux {
                    packageName = "hachimi-world-dev" // Linux does not support Chinese characters
                    iconFile.set(rootProject.file("icons/icon.png"))
                }
            }
        }
    }
}

buildkonfig {
    packageName = "world.hachimi.app"

    val props = Properties().apply { load(rootProject.file(SdkConstants.FN_LOCAL_PROPERTIES).reader()) }

    defaultConfigs {
        buildConfigField(Type.LONG, "BUILD_TIME", System.currentTimeMillis().toString())
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

// 为不同类型(debug、release)OHOS构建注册Copy任务并发布到Harmony App目录
arrayOf("debug", "release").forEach { type ->
    tasks.register<Copy>("publish${type.capitalizeUS()}BinariesToHarmonyApp") {
        group = "harmony" // 归类到harmony任务组
        dependsOn(
            "link${type.capitalizeUS()}SharedOhosArm64"        )
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        into(harmonyAppDir) // 输出目标目录
        from("build/bin/ohosArm64/${type}Shared/libkn_api.h") { // 复制头文件
            into("entry/src/main/cpp/include/arm64-v8a/")         // 指定目录
        }
        from(project.file("build/bin/ohosArm64/${type}Shared/libkn.so")) { // 复制共享库文件
            into("entry/libs/arm64-v8a/")           // 指定目标目录
        }
        val composeResourcePackage = "${rootProject.name.lowercase()}.${project.name.lowercase()}.generated.resources"
        from("src/commonMain/composeResources") {
            into("entry/src/main/resources/rawfile/composeResources/$composeResourcePackage/")
        }

    }
}

val harmonyAppDir: File = run {
    val cliPath = project.findProperty("harmonyAppPath") as String?
    if (cliPath.isNullOrBlank()) {
        // 默认：项目根目录 /harmonyApp
        rootProject.file("harmonyApp")
    } else {
        // 命令行传入的路径
        file(cliPath)
    }
}

fun String.capitalizeUS(): String = this.replaceFirstChar {
    if (it.isLowerCase()) it.titlecase() else it.toString()
}