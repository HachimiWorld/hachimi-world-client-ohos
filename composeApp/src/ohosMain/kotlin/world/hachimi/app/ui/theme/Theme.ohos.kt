package world.hachimi.app.ui.theme

import androidx.compose.runtime.Composable
import world.hachimi.app.sendOhosPlatformAction

private object OhosSystemUIController : SystemUIController {
    override fun setSystemBarsTheme(darkTheme: Boolean) {
        sendOhosPlatformAction("systemBars", darkTheme.toString())
    }
}

@Composable
actual fun rememberSystemUIController(): SystemUIController = OhosSystemUIController
