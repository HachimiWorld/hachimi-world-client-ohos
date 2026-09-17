package world.hachimi.app.ui.design.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale

actual fun isDiffusionBackgroundSupported(): Boolean = false

@Composable
actual fun FallbackDiffusionBackground(modifier: Modifier, painter: Painter) {
    Image(painter, contentDescription = null, modifier = modifier.alpha(0.25f), contentScale = ContentScale.Crop)
}
