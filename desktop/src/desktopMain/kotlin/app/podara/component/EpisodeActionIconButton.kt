package app.podara.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import app.podara.theme.PodaraTheme
import app.podara.util.Strings
import java.awt.Cursor

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
        targetValue = if (enabled && isHovered) colors.elevated else androidx.compose.ui.graphics.Color.Transparent,
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
