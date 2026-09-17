@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package androidx.navigation3.ui

import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation3.runtime.NavEntry
import world.hachimi.app.ui.LocalAnimatedVisibilityScope
import world.hachimi.app.util.PlatformBackHandler

private class EntryOwner : ViewModelStoreOwner, LifecycleOwner {
    override val viewModelStore = ViewModelStore()
    override val lifecycle = LifecycleRegistry(this)
    fun destroy() {
        lifecycle.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()
    }
}

private class Slot<T : Any>(val id: Int, val key: T, val owner: EntryOwner = EntryOwner())

/**
 * CPF 1.9 host: AnimatedContent, per-entry ViewModel stores and saved UI state.
 * Predictive-back gestures fall back to a completed back action on OHOS.
 */
@Composable
fun <T : Any> NavDisplay(
    backStack: List<T>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    transitionSpec: AnimatedContentTransitionScope<Int>.() -> ContentTransform = { fadeIn() togetherWith fadeOut() },
    popTransitionSpec: AnimatedContentTransitionScope<Int>.() -> ContentTransform = transitionSpec,
    predictivePopTransitionSpec: AnimatedContentTransitionScope<Int>.() -> ContentTransform = popTransitionSpec,
    entryProvider: (T) -> NavEntry<T>,
) {
    if (backStack.isEmpty()) return
    val holder = rememberSaveableStateHolder()
    val slots = remember { mutableListOf<Slot<T>>() }
    val retired = remember { mutableListOf<Slot<T>>() }
    var nextId by remember { mutableIntStateOf(0) }
    var isPop by remember { mutableStateOf(false) }
    val keys = backStack.toList()
    val oldKeys = slots.map { it.key }
    if (oldKeys != keys) {
        isPop = keys.size < oldKeys.size
        var prefix = 0
        while (prefix < minOf(keys.size, oldKeys.size) && keys[prefix] == oldKeys[prefix]) prefix++
        while (slots.size > prefix) retired.add(slots.removeAt(slots.lastIndex))
        keys.drop(prefix).forEach { slots.add(Slot(nextId++, it)) }
    }
    val top = slots.last()
    val parent = LocalLifecycleOwner.current
    DisposableEffect(parent, top.id) {
        fun update() {
            slots.forEach { slot ->
                val cap = if (slot === top) Lifecycle.State.RESUMED else Lifecycle.State.CREATED
                slot.owner.lifecycle.currentState = minOf(parent.lifecycle.currentState, cap)
            }
        }
        val observer = LifecycleEventObserver { _, _ -> update() }
        parent.lifecycle.addObserver(observer)
        update()
        onDispose { parent.lifecycle.removeObserver(observer) }
    }
    PlatformBackHandler(backStack.size > 1, onBack)
    AnimatedContent(
        targetState = top.id,
        modifier = modifier,
        transitionSpec = { if (isPop) popTransitionSpec() else transitionSpec() },
        label = "OhosNavigation",
    ) { id ->
        val slot = (slots + retired).firstOrNull { it.id == id }
        if (slot != null) {
            DisposableEffect(id) {
                onDispose {
                    if (slot in retired) {
                        retired.remove(slot)
                        holder.removeState(id)
                        slot.owner.destroy()
                    }
                }
            }
            holder.SaveableStateProvider(id) {
                CompositionLocalProvider(
                    LocalViewModelStoreOwner provides slot.owner,
                    LocalLifecycleOwner provides slot.owner,
                    LocalAnimatedVisibilityScope provides this,
                ) {
                    entryProvider(slot.key).content(slot.key)
                }
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose { (slots + retired).forEach { it.owner.destroy() } }
    }
}
