package app.podara.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import app.podara.theme.PodaraTheme
import app.podara.util.Strings
import java.awt.Cursor
import kotlinx.coroutines.launch

@Composable
fun EpisodeActionIconButton(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    val colors = PodaraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val bg by animateColorAsState(
        targetValue = if (enabled && isHovered) colors.elevated else Color.Transparent,
        animationSpec = tween(durationMillis = 150)
    )

    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(bg)
            .then(if (enabled) Modifier.pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR))) else Modifier)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = when {
                !enabled -> colors.textDisabled
                selected -> colors.accent
                else -> colors.textSecondary
            },
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun FavoriteEpisodeButton(
    isFavorite: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onToggle: () -> Unit
) {
    EpisodeActionIconButton(
        icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
        contentDescription = if (isFavorite) Strings["episode_remove_from_favorites"] else Strings["episode_add_to_favorites"],
        modifier = modifier,
        enabled = enabled,
        selected = isFavorite,
        onClick = onToggle
    )
}

/**
 * "Add to Queue" button with fly-away animation feedback.
 * When clicked, a translucent PlaylistAdd icon flies up-right, shrinks, and fades out
 * to visually confirm the episode was added to the queue.
 */
@Composable
fun AddToQueueButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val colors = PodaraTheme.colors
    val scope = rememberCoroutineScope()
    val flyProgress = remember { Animatable(0f) }
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val bg by animateColorAsState(
        targetValue = if (enabled && isHovered) colors.elevated else Color.Transparent,
        animationSpec = tween(durationMillis = 150)
    )

    Box(modifier = modifier) {
        // Main button
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(bg)
                .then(if (enabled) Modifier.pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR))) else Modifier)
                .clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        scope.launch {
                            flyProgress.snapTo(0f)
                            flyProgress.animateTo(1f, tween(600))
                            flyProgress.snapTo(0f)
                        }
                        onClick()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.PlaylistAdd,
                contentDescription = Strings["episode_add_to_queue"],
                tint = if (enabled) colors.textSecondary else colors.textDisabled,
                modifier = Modifier.size(20.dp)
            )
        }

        // Fly-away icon — uses offset so it paints outside parent bounds without clipping
        if (flyProgress.value > 0f) {
            val p = flyProgress.value
            val alpha = (1f - p) * 0.7f

            Icon(
                Icons.Default.PlaylistAdd,
                tint = colors.accent.copy(alpha = alpha),
                contentDescription = null,
                modifier = Modifier
                    .offset(x = -(p * 70).dp, y = -(p * 50).dp)
                    .graphicsLayer(alpha = alpha, scaleX = 1f - p * 0.4f, scaleY = 1f - p * 0.4f)
                    .size(20.dp)
            )
        }
    }
}
