# OHOS 移植（长期分支）

基线：本地 dev，`cb19001`（2026-08-31）。工作分支：`codex/ohos-latest-dev`。

这个分支**只构建 OHOS**：不再维护 `androidApp` / `desktopApp` / `webApp` / `baselineprofile`，
也不再需要「一份 build script 同时兼容 Android 与 OHOS」。入口只有一个 `./gradlew`。

## 为什么 OHOS 用独立的构建脚本

上游是 Kotlin 2.3.21 / Compose 1.11.1，而 CPF-KMP-CMP 只发布 2.2.21-1.0.0 / 1.9.2-1.0.0
（版本矩阵见 `gradle/ohos.versions.toml`）。差一个大版本，而一个 Gradle project 只能有一套
插件版本，所以 `composeApp/build.ohos.gradle.kts` 取代 `composeApp/build.gradle.kts`。

上游那份构建脚本**原样保留**（不参与构建），作为下次同步上游基线时的对照。

替换发生在 `settings.gradle.kts`：

```kotlin
include(":composeApp")
rootProject.buildFileName = "build.ohos.gradle.kts"
project(":composeApp").buildFileName = "build.ohos.gradle.kts"
```

`buildFileName` 是 per-project 的，settings 在项目求值前设置它，所以 `build.gradle.kts`
根本不会被读取。`libs` 同样在 settings 里指向 `gradle/ohos.versions.toml`，上游那份
catalog 改名为 `upstreamLibs`。

## 目录结构

**新增，同步基线时可整包复制**

- `composeApp/src/ohosMain/**` —— 所有 expect 的 OHOS actual：平台信息、日志、语言、
  偏好存储、AVPlayer、窗口 Insets、系统栏、分享、Coil 磁盘缓存、图片取色、返回键，
  外加 `cinterop/`（rawfile）。
- `composeApp/src/ohosCompatMain/**` —— 上游有、CPF 没有的东西的替身：
  Navigation3 宿主、Koin 注解，以及 `androidx.compose.ui.tooling.preview.Preview`
  （CPF 把 Preview 放在 `org.jetbrains.compose.ui.tooling.preview`，AndroidX 那个包在 OHOS
  类路径上不存在，这个 shim 让 commonMain 保持与上游一致）。
- `composeApp/build.ohos.gradle.kts`、`build.ohos.gradle.kts`
- `harmonyApp/**` —— HarmonyOS 宿主工程（NAPI、EntryAbility、Index.ets）
- `OHOS_MIGRATION.md`（本文件）

**对上游文件的改动（全部，共 8 个文件）**

| 文件 | 改动 |
| --- | --- |
| `settings.gradle.kts` | 换 catalog、换 buildFileName、只 include `:composeApp` |
| `gradle/wrapper/gradle-wrapper.properties` | `9.6.1` → `8.14.5` |
| `gradle.properties` | 增加 `rendererBackend=skia` |
| `.gitignore` | 忽略 `/runscript`（DevEco run 配置生成） |
| `.../ui/settings/SettingsScreen.kt` | 删 2 行 `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` |
| `.../model/{GlobalStore, ArtworkDetailViewModel, PublishViewModel}.kt` | 非 Composable 的 `getString(StringResource)` 换成 `getStringOnMain`（见下） |

## 构建与运行

```sh
export DEVECO_HOME=/Applications/DevEco-Studio.app/Contents
export DEVECO_SDK_HOME="$DEVECO_HOME/sdk"
export PATH="$DEVECO_SDK_HOME:$DEVECO_HOME/tools/node/bin:$DEVECO_HOME/tools/ohpm/bin:$DEVECO_HOME/tools/hvigor/bin:$PATH"

# 1. 编译并发布 libkn.so + Compose 资源到 harmonyApp/
./gradlew :composeApp:publishDebugBinariesToHarmonyApp --offline

# 2. HAP
cd harmonyApp
ohpm install --all
node "$DEVECO_HOME/tools/hvigor/bin/hvigorw.js" --mode module -p module="entry@default" \
  -p product=default -p buildMode=debug -p requiredDeviceType=phone assembleHap
```

直接跑 `./runscript/runOhosApp-Mac.sh` 会串起上面全部步骤 + `hdc install` + `aa start`
（DevEco Studio 的 run 配置走的也是它，脚本内部只调用 `./gradlew`）。

## 同步上游基线 checklist

1. `git fetch origin`，从新基线开新分支
2. 复制 `composeApp/src/ohosMain`、`ohosCompatMain`、`build.ohos.gradle.kts`、`harmonyApp/`
3. 重贴 `settings.gradle.kts` 的那几行
4. 对齐 `gradle/ohos.versions.toml`：先看新基线用了哪些依赖，再映射到 CPF 已发布的 `-1.0.0`
5. `gradle-wrapper.properties`、`gradle.properties` 各一行
6. 编译 → 链接 → HAP → 模拟器，按报错补 shim：
   - 新增 `@Preview` → shim 已覆盖，通常不用动
   - 用到 CPF 还没有、或还是 `internal` 的 API → 优先在 `ohosCompatMain` 加 shim，实在不行再改 commonMain
   - 新增 `expect` 声明 → 在 `ohosMain` 补 actual
7. 更新本文件顶部的基线 commit

## 已知约束与关键修复

- **Compose 资源必须打包 `preparedResources`**：`publish*BinariesToHarmonyApp` 复制的是
  `build/generated/compose/resourceGenerator/preparedResources` 里的 `.cvr`，不是
  `src/commonMain/composeResources` 的原始 XML；打错会在启动时抛 `Failed to open raw file`。
- **非 Composable 的 `getString(StringResource)` 在 OHOS 上必须走主线程**。它会读取平台资源
  环境，而 CPF 每次都经 N-API 查询 ArkTS 状态；在工作线程调用会让进程以
  `ecma_vm cannot run in multi-thread!` 终止。统一用 `getStringOnMain(...)`（`model/` 下 3 处）。
  若 CPF 把 `getSystemEnvironment()` 改成缓存，这 3 个文件可以还原成上游原样。
- **OHOS 侧依赖比其它平台旧**（CMP 1.9.2 vs 1.11.1），行为可能有细微差异。
- **Haze** 用官方的 CPF fork：`dev.chrisbanes.haze:haze:1.7.2-1.0.0`
  （源码 `gitcode.com/CPF-KMP-CMP/haze`，tag `v1.7.2-1.0.0`，CPF nexus 上有）。
  它的做法是 `ohosMain { dependsOn(skikoMain) }`——直接复用 Skia 那套 actual，只额外补
  `Log`/`Time` 两个文件，所以 OHOS 上是**真模糊**（`isBlurEnabledByDefault() = true`）。
  不要再往 Maven Local 发布同坐标的包：`mavenLocal()` 排在 nexus 前面，会把它遮蔽掉。
- **签名配置不入库**：`harmonyApp/build-profile.json5` 在本机生成（已被 .gitignore），
  模板是 `harmonyApp/build-profile.example.json5`。
- `libkn.so`、`libkn_api.h` 和复制过去的 Compose 资源都是构建产物，宿主侧忽略。

## 依赖来源

## 媒体控制中心与后台播放

- 播放信息通过 **AVSession** 发布（`world/hachimi/app/player/OhosMediaSession.kt`，CPF 的
  K/N 发行版自带 `OHAVSession.def`，不需要自己写 cinterop）。系统媒体卡片显示封面/歌名/歌手，
  并能回传播放、暂停、上一首、下一首和拖动进度；命令统一转给 `PlayerService`，与播放引擎解耦。
- **`assetId` 是必需的**：不设置时 `OH_AVSession_SetAVMetadata` 返回
  `AV_SESSION_ERR_SERVICE_EXCEPTION (6600101)`，而其它 setter 都返回成功，很容易误判成封面问题。
- 后台播放用长时任务：Kotlin 在播放状态翻转时通知宿主（`playbackActive`），
  `EntryAbility` 调 `backgroundTaskManager.startBackgroundRunning(context, AUDIO_PLAYBACK, wantAgent)`，
  暂停时停止。三个前提缺一不可：
  1. `module.json5` 的 `requestPermissions` 要有 `ohos.permission.KEEP_BACKGROUND_RUNNING`；
  2. `module.json5` 的 `abilities` 要有 `"backgroundModes": ["audioPlayback"]`，
     否则报 `9800005 Continuous Task verification failed. The bgMode is invalid.`；
  3. 应用必须持有处于播放状态的 AVSession，否则系统会取消这个长时任务。

| 依赖 | 来源 |
| --- | --- |
| `org.jetbrains.*`（Kotlin / Compose / skiko / collection） | CPF nexus（`maven.eazytec-cloud.com`） |
| `dev.chrisbanes.haze:*:1.7.2-1.0.0` | CPF nexus（官方 fork） |
| `io.coil-kt.coil3:*:3.3.0-1.0.0`、`io.insert-koin:*:4.1.1-1.0.0` | CPF nexus |
| `io.github.vinceglb:filekit-*:0.12.0-1.0.0` | **只有本机 Maven Local** |

最后一条意味着 `settings.gradle.kts` 里的 `mavenLocal()` 暂时还不能去掉：FileKit 的
OHOS fork（`CPF-KMP-CMP/FileKit-ohos`）还没有发到 nexus，换台机器就构建不了。
如果要让这个分支能被别人 clone 下来直接构建，需要把 FileKit fork 也发到 nexus。

## 签名与分发

- `harmonyApp/AppScope/app.json5` 的 `bundleName` 必须和签名 profile 里的 `bundle-name`
  完全一致，否则 `SignHap` 直接报 `00303074`。当前 profile 是 `com.example.harmonyapp`
  的 debug 版；一旦把 bundleName 改成正式包名，就必须在 AGC / DevEco 重新生成签名材料。
- 调试签名（`appProvisionType: "debug"`）只能装在 profile 里登记的设备上，且有有效期
  （当前这份到 2026-09-28）。给别人装要走 AGC 内测/公测或上架，见 release 打包流程。
