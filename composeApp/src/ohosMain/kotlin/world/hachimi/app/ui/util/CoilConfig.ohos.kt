package world.hachimi.app.ui.util

import coil3.disk.DiskCache
import io.github.vinceglb.filekit.*
import okio.Path.Companion.toPath

actual fun getPlatformDiskCache(): DiskCache? =
    DiskCache.Builder().directory((FileKit.cacheDir / "image_caches").path.toPath()).build()
