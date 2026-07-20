package world.hachimi.app.ui.committee

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import hachimiworld.composeapp.generated.resources.Res
import hachimiworld.composeapp.generated.resources.nav_committee_center
import org.jetbrains.compose.resources.stringResource
import world.hachimi.app.ui.component.ScreenScaffold
import world.hachimi.app.ui.design.components.Text

@Composable
fun CommitteeCenterScreen() {
    ScreenScaffold(
        title = { Text(stringResource(Res.string.nav_committee_center), maxLines = 1) },
    ) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            Text("委员会中心正在开发中，敬请期待", style = MaterialTheme.typography.headlineLarge)
        }
    }
}