package world.hachimi.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeImageBitmap
import coil3.Image
import coil3.toBitmap

actual fun calculateAvgColor(bitmap: Image): Color = calculateAvgColor(bitmap.toBitmap().asComposeImageBitmap())
