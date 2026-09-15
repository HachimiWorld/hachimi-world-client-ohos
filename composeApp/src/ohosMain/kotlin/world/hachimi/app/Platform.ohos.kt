@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.experimental.ExperimentalNativeApi::class)

package world.hachimi.app

import io.github.vinceglb.filekit.*
import io.github.vinceglb.filekit.dialogs.init
import io.github.vinceglb.filekit.dialogs.dispose
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.ArkTS.ArkTS_Napi_NativeModule.*
import platform.BasicServicesKit.DeviceInfo.OH_GetOSFullName
import world.hachimi.app.logging.Logger

class OhosPlatform : Platform {
    override val name: String = "ohos"
    override val platformVersion: String get() = OH_GetOSFullName()?.toKString() ?: "OHOS"
    override val variant: String = "${BuildKonfig.BUILD_TYPE}-ohos"
    override fun getCacheDir(): PlatformFile = FileKit.cacheDir
    override fun getDataDir(): PlatformFile = FileKit.filesDir
    override fun openUrl(url: String) {
        require(url.isNotBlank() && '\u0000' !in url) { "Invalid URL" }
        OhosUrlLauncher.open(url)
    }
}

actual fun getPlatform(): Platform = OhosPlatform()

// N-API references are only accessed on the ArkTS/UI thread.
private object OhosUrlLauncher {
    var environment: napi_env? = null
    var callback: napi_ref? = null
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun dispose() {
        callback?.let { napi_delete_reference(environment, it) }
        callback = null
        environment = null
    }

    fun open(url: String) {
        scope.launch {
            try {
                val env = checkNotNull(environment) { "EntryAbility must initialize the OHOS platform bridge" }
                val ref = checkNotNull(callback) { "OHOS URL launcher has been disposed" }
                memScoped {
                    val handles = alloc<napi_handle_scopeVar>()
                    check(napi_open_handle_scope(env, handles.ptr) == napi_ok)
                    try {
                        val function = alloc<napi_valueVar>()
                        val receiver = alloc<napi_valueVar>()
                        val argument = alloc<napi_valueVar>()
                        check(napi_get_reference_value(env, ref, function.ptr) == napi_ok)
                        check(napi_get_undefined(env, receiver.ptr) == napi_ok)
                        check(napi_create_string_utf8(env, url, url.encodeToByteArray().size.convert(), argument.ptr) == napi_ok)
                        val status = napi_call_function(env, receiver.value, function.value, 1u, argument.ptr, null)
                        if (status == napi_pending_exception) {
                            val exception = alloc<napi_valueVar>()
                            napi_get_and_clear_last_exception(env, exception.ptr)
                        }
                        check(status == napi_ok) { "URL launcher failed: $status" }
                    } finally {
                        napi_close_handle_scope(env, handles.value)
                    }
                }
            } catch (error: Exception) {
                Logger.e("OhosPlatform", "Cannot open URL", error)
            }
        }
    }
}

/** Called by EntryAbility.onCreate with its URL launcher and UIAbilityContext. */
@CName("HachimiInitPlatform")
fun hachimiInitPlatform(env: napi_env?, info: napi_callback_info?): napi_value? = memScoped {
    try {
        require(env != null)
        val count = alloc<size_tVar> { value = 2u }
        val arguments = allocArray<napi_valueVar>(2)
        check(napi_get_cb_info(env, info, count.ptr, arguments, null, null) == napi_ok)
        require(count.value == 2uL) { "Expected URL launcher and UIAbilityContext" }
        val type = alloc<napi_valuetype.Var>()
        check(napi_typeof(env, arguments[0], type.ptr) == napi_ok)
        require(type.value == napi_valuetype.napi_function) { "Expected URL launcher function" }
        FileKit.init(env, requireNotNull(arguments[1]))
        val reference = alloc<napi_refVar>()
        check(napi_create_reference(env, arguments[0], 1u, reference.ptr) == napi_ok)
        OhosUrlLauncher.dispose()
        OhosUrlLauncher.environment = env
        OhosUrlLauncher.callback = reference.value
    } catch (error: Exception) {
        napi_throw_error(env, null, error.message ?: "Cannot initialize platform bridge")
    }
    null
}

@CName("HachimiDisposePlatform")
fun hachimiDisposePlatform(env: napi_env?, info: napi_callback_info?): napi_value? {
    try { FileKit.dispose() }
    catch (error: Exception) { napi_throw_error(env, null, error.message ?: "Cannot dispose FileKit") }
    OhosUrlLauncher.dispose()
    return null
}
