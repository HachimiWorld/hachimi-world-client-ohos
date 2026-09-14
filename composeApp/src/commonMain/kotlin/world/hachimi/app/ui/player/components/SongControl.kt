package world.hachimi.app.ui.player.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import hachimiworld.composeapp.generated.resources.Res
import hachimiworld.composeapp.generated.resources.ms_pause
import hachimiworld.composeapp.generated.resources.ms_play_arrow
import hachimiworld.composeapp.generated.resources.ms_repeat
import hachimiworld.composeapp.generated.resources.ms_repeat_on
import hachimiworld.composeapp.generated.resources.ms_shuffle
import hachimiworld.composeapp.generated.resources.ms_shuffle_on
import hachimiworld.composeapp.generated.resources.ms_skip_next
import hachimiworld.composeapp.generated.resources.ms_skip_previous
import org.jetbrains.compose.resources.vectorResource

@Composable
fun SongControl(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    isLoading: Boolean,
    shuffle: Boolean,
    onShuffleModeChange: (Boolean) -> Unit,
    repeat: Boolean,
    onRepeatModeChange: (Boolean) -> Unit,
    loadingProgress: () -> Float,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
    ) {
        IconButton(onClick = {
            onShuffleModeChange(!shuffle)
        }) {
            if (shuffle) Icon(vectorResource(Res.drawable.ms_shuffle_on), "Shuffle On")
            else Icon(vectorResource(Res.drawable.ms_shuffle), "Shuffle Off")
        }
        IconButton(onClick = onPreviousClick) {
            Icon(vectorResource(Res.drawable.ms_skip_previous), "Skip Previous")
        }
        IconButton(onClick = onPlayPauseClick, colors = IconButtonDefaults.filledIconButtonColors()) {
            if (isLoading) {
                val animatedProgress by animateFloatAsState(targetValue = loadingProgress())
                val showProgress = animatedProgress > 0f && animatedProgress < 1f

                Crossfade(showProgress) {
                    if (it) CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = LocalContentColor.current,
                        trackColor = LocalContentColor.current.copy(alpha = 0.12f),
                        strokeWidth = 2.dp,
                        progress = { animatedProgress }
                    ) else CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = LocalContentColor.current,
                        strokeWidth = 2.dp
                    )
                }
            } else {
                if (isPlaying) {
                    Icon(vectorResource(Res.drawable.ms_pause), "Pause")
                } else {
                    Icon(vectorResource(Res.drawable.ms_play_arrow), "Play")
                }
            }
        }
        IconButton(onClick = onNextClick) {
            Icon(vectorResource(Res.drawable.ms_skip_next), "Skip Next")
        }
        IconButton(onClick = {
            onRepeatModeChange(!repeat)
        }) {
            if (repeat) Icon(vectorResource(Res.drawable.ms_repeat_on), "Repeat On")
            else Icon(vectorResource(Res.drawable.ms_repeat), "Repeat Off")
        }
    }
}