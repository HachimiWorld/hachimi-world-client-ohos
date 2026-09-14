package world.hachimi.app.ui.player.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import hachimiworld.composeapp.generated.resources.Res
import hachimiworld.composeapp.generated.resources.ms_music_note
import kotlin.math.abs
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.vectorResource

@Composable
fun Lyrics(
    loading: Boolean,
    currentLine: Int,
    lines: List<String>,
    fadeColor: Color,
    modifier: Modifier
) {
    BoxWithConstraints(modifier) {
        val lazyListState = rememberLazyListState()
        LaunchedEffect(currentLine) {
            if (currentLine == -1) {
                lazyListState.scrollToItem(0)
            } else {
                lazyListState.animateScrollToItem(currentLine, scrollOffset = -(constraints.maxHeight * 0.3).roundToInt())
            }
        }

        val fadeHeight = 64.dp
        if (loading) {
            Icon(
                modifier = Modifier.padding(top = 64.dp).size(28.dp),
                imageVector = vectorResource(Res.drawable.ms_music_note),
                contentDescription = "Loading",
                tint = LocalContentColor.current.copy(0.48f)
            )
        } else if (lines.isNotEmpty()) LazyColumn(Modifier.fillMaxSize().drawWithContent {
            drawContent()
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(fadeColor, Color.Transparent),
                    startY = 0f,
                    endY = fadeHeight.toPx()
                ),
                size = size.copy(height = fadeHeight.toPx())
            )
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color.Transparent, fadeColor),
                    startY = size.height - fadeHeight.toPx(),
                    endY = size.height
                ),
                size = size.copy(height = fadeHeight.toPx()),
                topLeft = Offset(
                    x = 0f,
                    y = size.height - fadeHeight.toPx()
                )
            )
        }, lazyListState, contentPadding = PaddingValues(
            top = 64.dp,
            bottom = maxHeight,
        )) {
            itemsIndexed(lines) { index, line ->
                val offsetToCurrent = currentLine - index
                val transition = updateTransition(offsetToCurrent)

                val scale by transition.animateFloat {
                    val offset = abs(it)
                    when (offset) {
                        0 -> 1f
//                        1 -> 0.8f
//                        2 -> 0.7f
                        else -> 0.7f
                    }
                    /*val fraction = (abs(it).toFloat() / 3).coerceIn(0f, 1f)
                    lerp(1f, 0.7f, fraction)*/
                }
                val alpha by transition.animateFloat {
                    if (it == 0) 1f else 0.48f
                }

                if (line.isBlank()) Icon(
                    imageVector = vectorResource(Res.drawable.ms_music_note),
                    contentDescription = "Interlude",
                    tint = LocalContentColor.current.copy(alpha = alpha),
                    modifier = Modifier.padding(vertical = 12.dp).size(28.dp).graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
                ) else Text(
                    modifier = Modifier.padding(vertical = 12.dp).graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    },
                    text = line,
                    color = LocalContentColor.current.copy(alpha = alpha),
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        } else {
            Icon(
                modifier = Modifier.padding(top = 64.dp).size(28.dp),
                imageVector = vectorResource(Res.drawable.ms_music_note),
                contentDescription = "No Lyrics",
                tint = LocalContentColor.current.copy(0.48f)
            )
        }
    }
}
