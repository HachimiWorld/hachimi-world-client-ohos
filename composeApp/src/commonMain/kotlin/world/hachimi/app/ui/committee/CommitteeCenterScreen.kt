package world.hachimi.app.ui.committee

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import world.hachimi.app.ui.LocalContentInsets
import world.hachimi.app.ui.design.components.Text

@Composable
fun CommitteeCenterScreen() {
    Box(
        Modifier
            .fillMaxSize()
            .padding(top = LocalContentInsets.current.asPaddingValues().calculateTopPadding()),
        contentAlignment = Alignment.Center,
    ) {
        Text("委员会中心正在开发中，敬请期待", style = MaterialTheme.typography.headlineLarge)
    }
}
