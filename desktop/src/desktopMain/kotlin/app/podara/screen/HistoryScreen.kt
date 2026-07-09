package app.podara.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.podara.data.AppDatabase
import app.podara.component.FavoriteEpisodeButton
import app.podara.data.model.Podcast
import app.podara.data.model.PodcastEpisode
import app.podara.data.model.PodcastHistory
import app.podara.player.MediaPlayerState
import app.podara.player.QueueItem
import app.podara.util.Strings
import app.podara.theme.DesignTokens
import app.podara.theme.PodaraTheme
import coil3.compose.AsyncImage
import java.awt.Cursor
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private data class HistorySection(
    val title: String,
    val items: List<Pair<PodcastHistory, PodcastEpisode>>
)

@Composable
fun HistoryScreen(
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

    var allHistoryItems by remember { mutableStateOf(emptyList<Pair<PodcastHistory, PodcastEpisode?>>()) }
    var podcastMap by remember { mutableStateOf(mapOf<String, Podcast>()) }
    var favoriteIds by remember { mutableStateOf(setOf<String>()) }
    var searchQuery by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        allHistoryItems = database.history.getAllWithEpisode()
        podcastMap = database.podcasts.getAllSync().associateBy { it.origin }
    }

    LaunchedEffect(favoriteVersion) {
        favoriteIds = database.favorites.getAllEpisodeIds()
    }

    // Filter by search query
    val filteredPairs = remember(allHistoryItems, searchQuery) {
        if (searchQuery.isBlank()) {
            allHistoryItems
        } else {
            allHistoryItems.filter { (_, episode) ->
                episode != null && (episode.title.contains(searchQuery, ignoreCase = true)
                        || episode.podcastTitle.contains(searchQuery, ignoreCase = true))
            }
        }
    }

    // Keep only non-null episodes for display
    val displayItems = remember(filteredPairs) {
        filteredPairs.mapNotNull { (history, episode) ->
            episode?.let { history to it }
        }
    }

    // Group by date
    val sections = remember(displayItems) {
        groupByDate(displayItems)
    }

    // Build context queue items for playWithContext
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
        // ── Header: Title + Subtitle + Search — always visible ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left: Title + Subtitle
            Column {
                Text(
                    text = Strings["history_title"],
                    fontSize = header.TitleSize,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(header.Gap))
                Text(
                    text = Strings["history_subtitle"],
                    fontSize = header.SubtitleSize,
                    color = colors.textMuted
                )
            }

            // Right: Search bar
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
                                text = Strings["history_search_placeholder"],
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
                                .clickable {
                                    searchQuery = ""
                                }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(DesignTokens.Spacing.md))

        if (displayItems.isEmpty()) {
            // ── Empty state in remaining space ──
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = colors.textMuted
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = Strings["history_empty"],
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = Strings["history_empty_hint"],
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )
                }
            }
        } else {
            // ── Toolbar: count + clear all ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = Strings.get("history_count", allHistoryItems.size),
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                // Clear All button
                val clearInteractionSource = remember { MutableInteractionSource() }
                val isClearHovered by clearInteractionSource.collectIsHoveredAsState()
                val clearAnimatedBg by animateColorAsState(
                    targetValue = if (isClearHovered) colors.elevated else Color.Transparent,
                    animationSpec = tween(durationMillis = 150)
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(clearAnimatedBg)
                        .border(1.dp, colors.border, RoundedCornerShape(6.dp))
                        .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                        .clickable(interactionSource = clearInteractionSource, indication = null) {
                            showClearDialog = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.DeleteSweep,
                        contentDescription = Strings["history_clear"],
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.sm))

            // ── Divider ──
            HorizontalDivider(color = colors.divider)

            Spacer(modifier = Modifier.height(6.dp))

            // ── History list grouped by date ──
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                sections.forEach { section ->
                    // Section header
                    item(key = "section_${section.title}") {
                        Spacer(modifier = Modifier.height(DesignTokens.Spacing.md))
                        Text(
                            text = section.title,
                            fontSize = DesignTokens.SectionHeader.TitleSize,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Serif,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(DesignTokens.Spacing.sm))
                    }

                    // Section items
                    items(section.items, key = { it.first.id }) { (history, episode) ->
                        HistoryItem(
                            history = history,
                            episode = episode,
                            podcast = podcastMap[episode.origin],
                            contextItems = contextItems,
                            colors = colors,
                            scope = scope,
                            database = database,
                            playerState = playerState,
                            isFavorite = episode.id in favoriteIds,
                            onFavoriteToggled = { isFavorite ->
                                favoriteIds = if (isFavorite) favoriteIds + episode.id else favoriteIds - episode.id
                                onFavoriteChanged()
                            },
                            onHistoryChanged = { allHistoryItems = it },
                            onShowPodcastDetail = onShowPodcastDetail
                        )
                        HorizontalDivider(color = colors.divider)
                    }
                }
            }
        }
    }

    // ── Clear history dialog ──
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(Strings["history_clear"], color = colors.textPrimary) },
            text = { Text(Strings["history_clear_confirm"], color = colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        database.history.deleteAll()
                        allHistoryItems = emptyList()
                    }
                    showClearDialog = false
                }) {
                    Text(Strings["history_clear_action"], color = colors.danger)
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

// ── History Item ──
@Composable
private fun HistoryItem(
    history: PodcastHistory,
    episode: PodcastEpisode,
    podcast: Podcast?,
    contextItems: List<QueueItem>,
    colors: app.podara.theme.PodaraColors,
    scope: kotlinx.coroutines.CoroutineScope,
    database: AppDatabase,
    playerState: MediaPlayerState,
    isFavorite: Boolean,
    onFavoriteToggled: (Boolean) -> Unit,
    onHistoryChanged: (List<Pair<PodcastHistory, PodcastEpisode?>>) -> Unit,
    onShowPodcastDetail: (Podcast) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val historyItemBg by animateColorAsState(
        targetValue = if (isHovered) colors.elevated else Color.Transparent,
        animationSpec = tween(durationMillis = 150)
    )
    val er = DesignTokens.EpisodeRow

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .background(historyItemBg)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                scope.launch {
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
                }
            }
            .padding(start = 12.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Cover art (clickable → play)
        Box(
            modifier = Modifier
                .size(er.CoverSize)
                .clip(RoundedCornerShape(er.CoverRadius))
        ) {
            AsyncImage(
                model = episode.imageUrl ?: podcast?.imageUrl,
                contentDescription = episode.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Duration badge
            if (episode.duration > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = formatDurationCompact(episode.duration),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(DesignTokens.Spacing.md))

        // Info column
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = episode.title,
                color = colors.textPrimary,
                fontSize = er.TitleSize,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = episode.podcastTitle,
                    color = colors.accent,
                    fontSize = er.AuthorSize,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                        .clickable(enabled = podcast != null) {
                        if (podcast != null) onShowPodcastDetail(podcast)
                    }
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = formatRelativeTime(history.timestamp),
                    color = colors.textMuted,
                    fontSize = 11.sp,
                    maxLines = 1
                )
                if (episode.duration > 0) {
                    Text(
                        text = "·",
                        color = colors.textMuted,
                        fontSize = 11.sp
                    )
                    Text(
                        text = formatDurationCompact(episode.duration),
                        color = colors.textMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Action buttons
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            FavoriteEpisodeButton(isFavorite = isFavorite) {
                scope.launch {
                    database.episodes.insert(episode)
                    onFavoriteToggled(database.favorites.toggle(episode))
                }
            }

            // Add to queue
            val qInteractionSource = remember { MutableInteractionSource() }
            val isQHovered by qInteractionSource.collectIsHoveredAsState()
            val queueAnimatedBg by animateColorAsState(
                targetValue = if (isQHovered) colors.elevated else Color.Transparent,
                animationSpec = tween(durationMillis = 150)
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(queueAnimatedBg)
                    .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                    .clickable(interactionSource = qInteractionSource, indication = null) {
                        scope.launch {
                            playerState.addToQueue(
                                url = episode.audioUrl,
                                title = episode.title,
                                artworkUrl = episode.imageUrl,
                                podcastArtworkUrl = podcast?.imageUrl,
                                episodeId = episode.id
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlaylistAdd,
                    contentDescription = Strings["episode_add_to_queue"],
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Remove from history
            val rInteractionSource = remember { MutableInteractionSource() }
            val isRHovered by rInteractionSource.collectIsHoveredAsState()
            val removeAnimatedBg by animateColorAsState(
                targetValue = if (isRHovered) colors.elevated else Color.Transparent,
                animationSpec = tween(durationMillis = 150)
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(removeAnimatedBg)
                    .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                    .clickable(interactionSource = rInteractionSource, indication = null) {
                        scope.launch {
                            database.history.delete(episode.id)
                            onHistoryChanged(database.history.getAllWithEpisode())
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = Strings["player_remove"],
                    tint = colors.textMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ── Group history items by date ──
private fun groupByDate(items: List<Pair<PodcastHistory, PodcastEpisode>>): List<HistorySection> {
    val calendar = Calendar.getInstance()
    val todayDayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
    val currentYear = calendar.get(Calendar.YEAR)
    val yesterdayDayOfYear = todayDayOfYear - 1

    // Monday of this week (Calendar: SUNDAY=1, MONDAY=2, ..., SATURDAY=7)
    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val daysSinceMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
    calendar.add(Calendar.DAY_OF_YEAR, -daysSinceMonday)
    val weekStartDayOfYear = calendar.get(Calendar.DAY_OF_YEAR)

    val todayItems = mutableListOf<Pair<PodcastHistory, PodcastEpisode>>()
    val yesterdayItems = mutableListOf<Pair<PodcastHistory, PodcastEpisode>>()
    val weekItems = mutableListOf<Pair<PodcastHistory, PodcastEpisode>>()
    val earlierItems = mutableListOf<Pair<PodcastHistory, PodcastEpisode>>()

    for (item in items) {
        val cal = Calendar.getInstance().apply { time = Date(item.first.timestamp) }
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        val year = cal.get(Calendar.YEAR)

        when {
            year == currentYear && dayOfYear == todayDayOfYear -> todayItems.add(item)
            year == currentYear && dayOfYear == yesterdayDayOfYear -> yesterdayItems.add(item)
            year == currentYear && dayOfYear >= weekStartDayOfYear -> weekItems.add(item)
            else -> earlierItems.add(item)
        }
    }

    val sections = mutableListOf<HistorySection>()
    if (todayItems.isNotEmpty()) sections.add(HistorySection(Strings["history_today"], todayItems))
    if (yesterdayItems.isNotEmpty()) sections.add(HistorySection(Strings["history_yesterday"], yesterdayItems))
    if (weekItems.isNotEmpty()) sections.add(HistorySection(Strings["history_this_week"], weekItems))
    if (earlierItems.isNotEmpty()) sections.add(HistorySection(Strings["history_earlier"], earlierItems))
    return sections
}

// ── Relative time formatting ──
private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    if (diff < 0) return formatDateAbsolute(timestamp)

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
            if (today - tsDay == 1) "Yesterday"
            else formatDateAbsolute(timestamp)
        }
    }
}

private fun formatDateAbsolute(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
    return sdf.format(Date(timestamp))
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
