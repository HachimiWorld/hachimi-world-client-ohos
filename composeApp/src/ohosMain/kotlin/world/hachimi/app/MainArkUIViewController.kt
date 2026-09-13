
package world.hachimi.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.window.ComposeArkUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import platform.ArkTS.ArkTS_Napi_NativeModule.napi_value
import platform.ArkTS.ArkTS_Napi_NativeModule.napi_env
import kotlin.experimental.ExperimentalNativeApi
import kotlinx.coroutines.initMainHandler
import org.koin.compose.koinInject
import world.hachimi.app.model.GlobalStore
import world.hachimi.app.ui.App

@OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class, ExperimentalComposeUiApi::class)
@CName("MainArkUIViewController")
fun MainArkUIViewController(env: napi_env): napi_value {
    initMainHandler(env)
    return ComposeArkUIViewController(env) {
        val global = koinInject<GlobalStore>()
        BackHandler {
            if (global.nav.backStack.size > 1) {
                global.nav.back()
            }/* else {
                finish()
            }*/
        }
        App()
    }
}