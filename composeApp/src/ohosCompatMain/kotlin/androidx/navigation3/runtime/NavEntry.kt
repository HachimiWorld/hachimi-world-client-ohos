package androidx.navigation3.runtime

import androidx.compose.runtime.Composable

/** OHOS compatibility API for the subset used by this app; no Navigation 3 binary is linked. */
interface NavKey

class NavEntry<T : Any>(val key: T, val content: @Composable (T) -> Unit)
