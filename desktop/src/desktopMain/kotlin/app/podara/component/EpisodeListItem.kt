package app.podara.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import app.podara.data.model.Podcast
import app.podara.data.model.PodcastEpisode
import app.podara.theme.DesignTokens
import coil3.compose.AsyncImage
import java.awt.Cursor

enum class EpisodeListItemSecondaryTextRole {
    PodcastName,
    Metadata
}

@Composable
fun EpisodeListItem(
    episode: PodcastEpisode,
    podcast: Podcast?,
    isPlaying: Boolean,
    secondaryText: String,
    tertiaryText: String,
    onPlay: () -> Unit,
    secondaryTextRole: EpisodeListItemSecondaryTextRole = EpisodeListItemSecondaryTextRole.PodcastName,
    onPodcastClick: ((Podcast) -> Unit)? = null,
    modifier: Modifier = Modifier,
    trailingActions: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val favoriteList = DesignTokens.FavoriteEpisodeList
    val shape = RoundedCornerShape(favoriteList.CardRadius)
    val itemBg by animateColorAsState(
        targetValue = when {
            isPlaying -> favoriteList.PlayingBackgroundColor
            isPressed -> favoriteList.PressedBackgroundColor
            isHovered -> favoriteList.HoverBackgroundColor
            else -> favoriteList.BackgroundColor
        },
        animationSpec = tween(durationMillis = DesignTokens.Animation.HoverMs)
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isPlaying -> favoriteList.PlayingBorderColor
            isHovered -> favoriteList.HoverBorderColor
            else -> favoriteList.BorderColor
        },
        animationSpec = tween(durationMillis = DesignTokens.Animation.HoverMs)
    )
    val podcastClick = onPodcastClick
    val podcastModifier = if (podcast != null && podcastClick != null) {
        Modifier
            .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
            .clickable { podcastClick(podcast) }
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(favoriteList.CardHeight)
            .clip(shape)
            .background(itemBg)
            .border(favoriteList.BorderWidth, borderColor, shape)
            .clickable(
                enabled = episode.audioUrl.isNotBlank(),
                interactionSource = interactionSource,
                indication = null,
                onClick = onPlay
            )
            .padding(horizontal = favoriteList.CardPaddingHorizontal, vertical = favoriteList.CardPaddingVertical),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(favoriteList.CoverSize)
                .shadow(
                    elevation = favoriteList.CoverShadowElevation,
                    shape = RoundedCornerShape(favoriteList.CoverRadius),
                    ambientColor = favoriteList.CoverShadowColor,
                    spotColor = favoriteList.CoverShadowColor
                )
                .clip(RoundedCornerShape(favoriteList.CoverRadius))
        ) {
            val imageUrl = episode.imageUrl?.takeIf { it.isNotBlank() }
                ?: podcast?.imageUrl?.takeIf { it.isNotBlank() }
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = episode.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Podcasts,
                    contentDescription = null,
                    tint = favoriteList.MetadataColor,
                    modifier = Modifier.size(favoriteList.CoverSize * 0.45f)
                )
            }
            if (episode.duration > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(favoriteList.DurationInset)
                        .height(favoriteList.DurationHeight)
                        .clip(RoundedCornerShape(favoriteList.DurationRadius))
                        .background(favoriteList.DurationBackgroundColor)
                        .padding(horizontal = favoriteList.DurationPaddingHorizontal),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = formatDurationCompact(episode.duration),
                        color = favoriteList.DurationTextColor,
                        fontSize = favoriteList.DurationTextSize,
                        fontWeight = favoriteList.DurationTextWeight
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(favoriteList.CoverContentGap))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = episode.title,
                color = favoriteList.TitleColor,
                fontSize = favoriteList.TitleSize,
                lineHeight = favoriteList.TitleLineHeight,
                fontWeight = favoriteList.TitleWeight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(favoriteList.PodcastNameMarginTop))
            Text(
                text = secondaryText,
                color = if (secondaryTextRole == EpisodeListItemSecondaryTextRole.PodcastName) {
                    favoriteList.PodcastNameColor
                } else {
                    favoriteList.MetadataColor
                },
                fontSize = if (secondaryTextRole == EpisodeListItemSecondaryTextRole.PodcastName) {
                    favoriteList.PodcastNameSize
                } else {
                    favoriteList.MetadataSize
                },
                lineHeight = if (secondaryTextRole == EpisodeListItemSecondaryTextRole.PodcastName) {
                    favoriteList.PodcastNameLineHeight
                } else {
                    favoriteList.MetadataSize
                },
                fontWeight = if (secondaryTextRole == EpisodeListItemSecondaryTextRole.PodcastName) {
                    favoriteList.PodcastNameWeight
                } else {
                    FontWeight.Normal
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = if (secondaryTextRole == EpisodeListItemSecondaryTextRole.PodcastName) podcastModifier else Modifier
            )
            if (tertiaryText.isNotBlank()) {
                Spacer(modifier = Modifier.height(favoriteList.MetadataMarginTop))
                Text(
                    text = tertiaryText,
                    color = favoriteList.MetadataColor,
                    fontSize = favoriteList.MetadataSize,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(favoriteList.ContentActionsGap))

        Row(horizontalArrangement = Arrangement.spacedBy(favoriteList.ActionsGap), content = trailingActions)
    }
}

fun formatEpisodeMetadata(metadata: String, durationSeconds: Int): String {
    val duration = if (durationSeconds > 0) formatDurationCompact(durationSeconds) else ""
    return listOf(metadata, duration).filter { it.isNotBlank() }.joinToString(" · ")
}

private fun formatDurationCompact(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}
