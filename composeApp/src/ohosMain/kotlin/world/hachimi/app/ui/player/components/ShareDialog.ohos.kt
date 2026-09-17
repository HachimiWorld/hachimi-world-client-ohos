package world.hachimi.app.ui.player.components

import coil3.PlatformContext
import world.hachimi.app.sendOhosPlatformAction

// The shared dialog advertises copying the share text; the host uses the system clipboard.
actual fun share(context: PlatformContext, text: String): Int {
    sendOhosPlatformAction("copyText", text)
    return 0
}
