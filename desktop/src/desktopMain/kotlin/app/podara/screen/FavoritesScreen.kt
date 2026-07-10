package app.podara.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.podara.component.AddToQueueButton
import app.podara.component.EpisodeListItem
import app.podara.component.formatEpisodeMetadata
import app.podara.component.FavoriteEpisodeButton
import app.podara.component.PodaraEmptyState
import app.podara.data.AppDatabase
import app.podara.data.model.Podcast
import app.podara.data.model.PodcastEpisode
import app.podara.data.model.PodcastFavorite
import app.podara.player.MediaPlayerState
import app.podara.player.QueueItem
import app.podara.theme.DesignTokens
import app.podara.theme.PodaraTheme
import app.podara.util.Strings
import kotlinx.coroutines.launch
import java.awt.Cursor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun FavoritesScreen(
    database: AppDatabase,
    playerState: MediaPlayerState,
    favoriteVersion: Int,
    onBack: () -> Unit,
    onFavoriteChanged: () -> Unit = {},
    onShowPodcastDetail: (Podcast) -> Unit = {}
) {
    val colors = PodaraTheme.colors
    val header = DesignTokens.PageHeader
    val searchToken = DesignTokens.SearchBar
    val scope = rememberCoroutineScope()

    var allFavoriteItems by remember { mutableStateOf(emptyList<Pair<PodcastFavorite, PodcastEpisode?>>()) }
    var podcastMap by remember { mutableStateOf(mapOf<String, Podcast>()) }
    var searchQuery by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }

    LaunchedEffect(favoriteVersion) {
        allFavoriteItems = database.favorites.getAllWithEpisode()
        podcastMap = database.podcasts.getAllSync().associateBy { it.origin }
    }

    val filteredPairs = remember(allFavoriteItems, searchQuery) {
        if (searchQuery.isBlank()) {
            allFavoriteItems
        } else {
            allFavoriteItems.filter { (favorite, episode) ->
                val title = episode?.title ?: favorite.title
                val podcastTitle = episode?.podcastTitle ?: favorite.podcastTitle
                title.contains(searchQuery, ignoreCase = true) || podcastTitle.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val displayItems = remember(filteredPairs) {
        filteredPairs.mapNotNull { (favorite, episode) ->
            episode?.let { favorite to it }
        }
    }

    val contextItems = remember(displayItems) {
        displayItems.map { (_, episode) ->
            QueueItem(url = episode.audioUrl, title = episode.title, subtitle = episode.podcastTitle, artworkUrl = episode.imageUrl, episodeId = episode.id)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(top = 28.dp, start = 32.dp, end = 32.dp, bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = Strings["favorites_title"],
                    fontSize = header.TitleSize,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(header.Gap))
                Text(
                    text = Strings["favorites_subtitle"],
                    fontSize = header.SubtitleSize,
                    color = colors.textMuted
                )
            }

            Surface(
                modifier = Modifier
                    .width(searchToken.Width)
                    .height(searchToken.Height)
                    .border(
                        DesignTokens.Border.Width,
                        DesignTokens.Border.SecondaryColor,
                        RoundedCornerShape(searchToken.Radius)
                    ),
                shape = RoundedCornerShape(searchToken.Radius),
                color = colors.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = searchToken.PaddingHorizontal),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(searchToken.IconSize)
                    )
                    Spacer(modifier = Modifier.width(searchToken.Gap))
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = Strings["favorites_search_placeholder"],
                                color = colors.textDisabled,
                                fontSize = searchToken.TextSize
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = colors.textPrimary,
                                fontSize = searchToken.TextSize
                            ),
                            cursorBrush = SolidColor(colors.accent)
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = Strings["discover_search_clear"],
                            tint = colors.textMuted,
                            modifier = Modifier
                                .size(searchToken.ClearIconSize)
                                .clickable { searchQuery = "" }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(DesignTokens.Spacing.md))

        if (displayItems.isEmpty()) {
            if (searchQuery.isBlank()) {
                PodaraEmptyState(
                    icon = Icons.Default.Favorite,
                    title = Strings["favorites_empty"],
                    subtitle = Strings["favorites_empty_hint"],
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
            } else {
                val glass = DesignTokens.Glass
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(DesignTokens.EmptyState.IconSize),
                            tint = colors.textMuted
                        )
                        Spacer(modifier = Modifier.height(DesignTokens.EmptyState.Gap))
                        Box(
                            modifier = Modifier
                                .width(DesignTokens.EmptyState.PanelWidth)
                                .shadow(
                                    glass.CompactShadowElevation,
                                    RoundedCornerShape(DesignTokens.EmptyState.PanelRadius),
                                    ambientColor = glass.CompactShadowColor,
                                    spotColor = glass.CompactShadowColor
                                )
                                .clip(RoundedCornerShape(DesignTokens.EmptyState.PanelRadius))
                                .background(glass.CompactGradient)
                                .border(glass.CompactBorderWidth, glass.CompactBorderColor, RoundedCornerShape(DesignTokens.EmptyState.PanelRadius))
                                .padding(DesignTokens.EmptyState.PanelPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = Strings["favorites_search_empty"],
                                    color = colors.textPrimary,
                                    fontSize = DesignTokens.EmptyState.TitleSize,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(DesignTokens.Spacing.xs))
                                Text(
                                    text = Strings["favorites_search_empty_hint"],
                                    color = colors.textSecondary,
                                    fontSize = DesignTokens.EmptyState.SubtitleSize
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = Strings.get("favorites_count", displayItems.size),
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                val toolbarButton = DesignTokens.ToolbarButton
                val clearInteractionSource = remember { MutableInteractionSource() }
                val isClearHovered by clearInteractionSource.collectIsHoveredAsState()
                val isClearPressed by clearInteractionSource.collectIsPressedAsState()
                val clearShape = RoundedCornerShape(toolbarButton.PillRadius)
                val clearTextColor = if (isClearHovered || isClearPressed) toolbarButton.PillHoverTextColor else toolbarButton.PillTextColor
                val clearIconColor = if (isClearHovered || isClearPressed) toolbarButton.PillHoverIconColor else toolbarButton.PillIconColor
                Box(
                    modifier = Modifier
                        .height(toolbarButton.PillHeight)
                        .widthIn(min = toolbarButton.ManageMinWidth)
                        .shadow(
                            if (isClearHovered) toolbarButton.PillHoverShadowElevation else 0.dp,
                            clearShape,
                            ambientColor = toolbarButton.PillHoverShadowColor,
                            spotColor = toolbarButton.PillHoverShadowColor
                        )
                        .clip(clearShape)
                        .background(
                            when {
                                isClearPressed -> toolbarButton.PillPressedBackgroundColor
                                isClearHovered -> toolbarButton.PillHoverBackgroundColor
                                else -> toolbarButton.PillDefaultBackgroundColor
                            }
                        )
                        .border(
                            toolbarButton.BorderWidth,
                            if (isClearHovered || isClearPressed) toolbarButton.PillHoverBorderColor else toolbarButton.PillDefaultBorderColor,
                            clearShape
                        )
                        .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                        .clickable(interactionSource = clearInteractionSource, indication = null) {
                            showClearDialog = true
                        }
                        .padding(horizontal = toolbarButton.PillPaddingHorizontal),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(toolbarButton.PillIconTextGap)
                    ) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = Strings["favorites_clear"],
                            tint = clearIconColor,
                            modifier = Modifier.size(toolbarButton.PillIconSize)
                        )
                        Text(
                            text = Strings["favorites_clear_button"],
                            color = clearTextColor,
                            fontSize = toolbarButton.PillTextSize,
                            lineHeight = toolbarButton.PillLineHeight,
                            fontWeight = toolbarButton.PillTextWeight
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.sm))
            HorizontalDivider(color = colors.divider)
            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = DesignTokens.FavoriteEpisodeList.ListPaddingTop,
                    bottom = DesignTokens.FavoriteEpisodeList.ListPaddingBottom
                ),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.FavoriteEpisodeList.CardGap)
            ) {
                items(displayItems, key = { it.first.episodeId }) { (favorite, episode) ->
                    val podcast = podcastMap[episode.origin]
                    val favoriteList = DesignTokens.FavoriteEpisodeList
                    EpisodeListItem(
                        episode = episode,
                        podcast = podcast,
                        isPlaying = playerState.currentEpisodeId == episode.id || playerState.currentUrl == episode.audioUrl,
                        secondaryText = episode.podcastTitle,
                        tertiaryText = formatEpisodeMetadata(formatFavoriteRelativeTime(favorite.timestamp), episode.duration),
                        onPlay = {
                            playerState.playWithContext(
                                context = contextItems,
                                targetUrl = episode.audioUrl,
                                title = episode.title,
                                subtitle = episode.podcastTitle,
                                artworkUrl = episode.imageUrl,
                                podcastArtworkUrl = podcast?.imageUrl,
                                durationMs = episode.duration * 1000L,
                                episodeId = episode.id
                            )
                            scope.launch {
                                database.history.insert(episode.origin, episode.id)
                            }
                        },
                        onPodcastClick = onShowPodcastDetail
                    ) {
                        AddToQueueButton(
                            size = favoriteList.ActionButtonSize,
                            radius = favoriteList.ActionButtonRadius,
                            iconSize = favoriteList.QueueIconSize,
                            hoverBackgroundColor = favoriteList.ActionButtonHoverBackgroundColor,
                            defaultIconColor = favoriteList.QueueIconColor,
                            hoverIconColor = favoriteList.QueueIconHoverColor
                        ) {
                            playerState.addToQueue(
                                url = episode.audioUrl,
                                title = episode.title,
                                artworkUrl = episode.imageUrl,
                                podcastArtworkUrl = podcast?.imageUrl,
                                episodeId = episode.id
                            )
                        }
                        FavoriteEpisodeButton(
                            isFavorite = true,
                            size = favoriteList.ActionButtonSize,
                            radius = favoriteList.ActionButtonRadius,
                            iconSize = favoriteList.FavoriteIconSize,
                            hoverBackgroundColor = favoriteList.ActionButtonHoverBackgroundColor,
                            defaultIconColor = favoriteList.FavoriteInactiveColor,
                            hoverIconColor = favoriteList.ActionIconHoverColor,
                            selectedIconColor = favoriteList.FavoriteActiveColor
                        ) {
                            scope.launch {
                                database.favorites.delete(episode.id)
                                allFavoriteItems = database.favorites.getAllWithEpisode()
                                onFavoriteChanged()
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(Strings["favorites_clear"], color = colors.textPrimary) },
            text = { Text(Strings["favorites_clear_confirm"], color = colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        database.favorites.deleteAll()
                        allFavoriteItems = emptyList()
                        onFavoriteChanged()
                    }
                    showClearDialog = false
                }) {
                    Text(Strings["favorites_clear_action"], color = colors.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(Strings["dialog_cancel"], color = colors.textSecondary)
                }
            },
            containerColor = colors.surface
        )
    }
}

private fun formatFavoriteRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    if (diff < 0) return formatFavoriteDateAbsolute(timestamp)

    val minutes = diff / 60_000
    val hours = minutes / 60

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        else -> {
            val cal = Calendar.getInstance()
            val today = cal.get(Calendar.DAY_OF_YEAR)
            cal.time = Date(timestamp)
            val tsDay = cal.get(Calendar.DAY_OF_YEAR)
            if (today - tsDay == 1) "Yesterday" else formatFavoriteDateAbsolute(timestamp)
        }
    }
}

private fun formatFavoriteDateAbsolute(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
