package world.hachimi.app.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import world.hachimi.app.getPlatform

/**
 * Resolves a [StringResource] on the main thread.
 *
 * Reading a string also reads the platform resource environment (language, region, dark theme and
 * density). On OHOS that environment is queried from ArkTS over N-API, and N-API calls are only
 * valid on the JS/UI thread. Resolving a string from a worker dispatcher therefore aborts the
 * process with `ecma_vm cannot run in multi-thread!`.
 *
 * Use this instead of [getString] whenever the caller may run off the main thread.
 */
suspend fun getStringOnMain(resource: StringResource, vararg formatArgs: Any): String =
    if (getPlatform().name == "ohos") {
        withContext(Dispatchers.Main) { getString(resource, *formatArgs) }
    } else {
        // Other platforms resolve the environment without crossing into a UI toolkit.
        getString(resource, *formatArgs)
    }
