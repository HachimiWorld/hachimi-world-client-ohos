package world.hachimi.app.ui.util

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import world.hachimi.app.ui.LocalContentInsets

/**
 * Bottom-only padding for list tails (MiniPlayer / shell [LocalContentInsets]).
 * Does not apply inset top — that belongs in scroll contentPadding so content can draw under chrome.
 */
@Composable
fun Modifier.listTailPadding(): Modifier {
    val bottom = LocalContentInsets.current.asPaddingValues().calculateBottomPadding()
    return this.navigationBarsPadding().padding(bottom = bottom)
}

/**
 * Adds a spacer to the bottom of a list to account for navigation bars and content insets.
 */
@Composable
fun ListTailSpacer() {
    val bottom = LocalContentInsets.current.asPaddingValues().calculateBottomPadding()
    Spacer(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(bottom = bottom)
    )
}

/**
 * Adds a spacer to the bottom of a LazyList to account for navigation bars and content insets.
 */
fun LazyListScope.listTailSpacerItem() {
    item("button_safe_area") {
        ListTailSpacer()
    }
}

/**
 * Adds a spacer to the bottom of a LazyGrid to account for navigation bars and content insets.
 */
fun LazyGridScope.listTailSpacerItem() {
    item("button_safe_area", span = { GridItemSpan(maxLineSpan) }) {
        ListTailSpacer()
    }
}