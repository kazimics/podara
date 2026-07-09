package app.podara.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.podara.component.FavoriteEpisodeButton
import app.podara.data.AppDatabase
import app.podara.data.PodcastDownload
import app.podara.data.model.PodcastFavorite
import app.podara.manager.DownloadManager
import app.podara.player.MediaPlayerState
import app.podara.player.QueueItem
import app.podara.util.Strings
import app.podara.theme.DesignTokens
import app.podara.theme.PodaraTheme
import kotlinx.coroutines.launch
import java.awt.Cursor
import java.awt.Desktop
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private data class PodcastGroup(
    val origin: String,
    val podcastTitle: String,
    val items: List<PodcastDownload>
)

@Composable
fun DownloadsScreen(
    database: AppDatabase,
    downloadManager: DownloadManager,
    downloadPath: String,
    downloadingEpisodes: Set<String>,
    downloadProgress: Map<String, Pair<Long, Long>>,
    downloadVersion: Int,
    completedDownloads: Set<String>,
    activeDownloadMeta: Map<String, Pair<String, String>>,
    playerState: MediaPlayerState? = null,
    favoriteVersion: Int,
    onPauseDownload: (String) -> Unit,
    onResumeDownload: (String) -> Unit,
    onCancelDownload: (String) -> Unit,
    onDeleteDownloaded: (String) -> Unit,
    onDeleteDownloadedByOrigin: (String) -> Unit,
    onFavoriteChanged: () -> Unit = {},
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val colors = PodaraTheme.colors
    val header = DesignTokens.PageHeader
    val spacing = DesignTokens.Spacing

    // ── State for completed downloads ──
    var completedList by remember { mutableStateOf(emptyList<PodcastDownload>()) }
    var totalBytes by remember { mutableStateOf(0L) }
    var groupedByPodcast by remember { mutableStateOf(emptyList<PodcastGroup>()) }
    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }
    var showBatchDeleteConfirm by remember { mutableStateOf<Pair<String, String>?>(null) }
    var favoriteIds by remember { mutableStateOf(setOf<String>()) }
    val scope = rememberCoroutineScope()

    // Reload completed list when downloadVersion changes
    LaunchedEffect(downloadVersion) {
        val all = database.downloads.getAllValid()
        completedList = all
        totalBytes = all.sumOf { File(it.filePath).length() }
        groupedByPodcast = all.groupBy { it.origin }.map { (origin, items) ->
            PodcastGroup(origin, items.first().podcastTitle, items)
        }
    }

    // ── Determine active download tasks from DB ──
    var activeTasks by remember { mutableStateOf(listOf<app.podara.data.DownloadTask>()) }
    LaunchedEffect(downloadVersion) {
        activeTasks = database.downloadTasks.getAllActive()
    }

    LaunchedEffect(favoriteVersion) {
        favoriteIds = database.favorites.getAllEpisodeIds()
    }

    val hasInProgress = downloadingEpisodes.isNotEmpty() || activeTasks.isNotEmpty()
    val hasCompleted = completedList.isNotEmpty()
    val showEmpty = !hasInProgress && !hasCompleted

    val downloadContextItems = remember(completedList) {
        completedList.filter { File(it.filePath).exists() }.map { dl ->
            QueueItem(url = dl.filePath, title = dl.episodeTitle.ifEmpty { dl.episodeId }, subtitle = dl.podcastTitle, episodeId = dl.episodeId, isDownloaded = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(top = 28.dp, start = 32.dp, end = 32.dp, bottom = 8.dp)
    ) {
        // ── Header ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = Strings["nav_downloads"],
                    fontSize = header.TitleSize,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(header.Gap))
                Text(
                    text = Strings["downloads_subtitle"],
                    fontSize = header.SubtitleSize,
                    color = colors.textMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.md))

        // ── Summary Card ──
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = colors.surface
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Location
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = Strings["downloads_summary_path"],
                            color = colors.textMuted,
                            fontSize = 11.sp
                        )
                        Text(
                            text = downloadPath,
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 280.dp)
                        )
                    }
                }

                // Divider
                Box(modifier = Modifier.width(1.dp).height(32.dp).background(colors.divider))

                // Storage
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        Icons.Default.Storage,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = Strings.get("downloads_summary_downloads", completedList.size),
                            color = colors.textPrimary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = Strings.get("downloads_summary_storage", formatFileSize(totalBytes)),
                            color = colors.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.sm))

        if (showEmpty) {
            // ── Empty state ──
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = colors.textMuted
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = Strings["downloads_empty"],
                        color = colors.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = Strings["downloads_empty_hint"],
                        color = colors.textMuted,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // ── In Progress Section ──
                if (hasInProgress) {
                    item(key = "section_in_progress") {
                        Spacer(modifier = Modifier.height(spacing.md))
                        Text(
                            text = Strings["downloads_in_progress"],
                            fontSize = DesignTokens.SectionHeader.TitleSize,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Serif,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(spacing.sm))
                    }

                    // Downloading episodes (from memory state)
                    items(downloadingEpisodes.toList(), key = { "dl_$it" }) { episodeId ->
                        val progress = downloadProgress[episodeId]
                        val meta = activeDownloadMeta[episodeId]
                        val task = activeTasks.find { it.episodeId == episodeId }
                        val current = progress?.first ?: task?.downloadedBytes ?: 0L
                        val total = progress?.second ?: task?.totalBytes ?: 0L

                        InProgressRow(
                            episodeId = episodeId,
                            podcastTitle = meta?.first ?: task?.podcastTitle ?: "",
                            episodeTitle = meta?.second ?: task?.episodeTitle ?: "",
                            currentBytes = current,
                            totalBytes = total,
                            isFavorite = episodeId in favoriteIds,
                            onToggleFavorite = {
                                val source = task ?: app.podara.data.DownloadTask(
                                    episodeId = episodeId,
                                    origin = "",
                                    audioUrl = "",
                                    podcastTitle = meta?.first ?: "",
                                    episodeTitle = meta?.second ?: episodeId,
                                    createdAt = 0L,
                                    updatedAt = 0L
                                )
                                scope.launch {
                                    val favorite = PodcastFavorite(
                                        episodeId = source.episodeId,
                                        origin = source.origin,
                                        timestamp = System.currentTimeMillis(),
                                        title = source.episodeTitle.ifEmpty { source.episodeId },
                                        podcastTitle = source.podcastTitle,
                                        audioUrl = source.audioUrl,
                                        imageUrl = null,
                                        duration = 0,
                                        pubDate = 0L
                                    )
                                    val nowFavorite = if (source.episodeId in favoriteIds) {
                                        database.favorites.delete(source.episodeId)
                                        false
                                    } else {
                                        database.favorites.insert(favorite)
                                        true
                                    }
                                    favoriteIds = if (nowFavorite) favoriteIds + source.episodeId else favoriteIds - source.episodeId
                                    onFavoriteChanged()
                                }
                            },
                            onPause = onPauseDownload,
                            onResume = onResumeDownload,
                            onCancel = onCancelDownload,
                            colors = colors
                        )
                        HorizontalDivider(color = colors.divider)
                    }

                    // Paused / Failed tasks from DB (not in downloadingEpisodes)
                    val pausedTasks = activeTasks.filter { t ->
                        t.state != "DOWNLOADING" && t.episodeId !in downloadingEpisodes
                    }
                    if (pausedTasks.isNotEmpty()) {
                        items(pausedTasks, key = { "paused_${it.episodeId}" }) { task ->
                            PausedTaskRow(
                                task = task,
                                isFavorite = task.episodeId in favoriteIds,
                                onToggleFavorite = {
                                    scope.launch {
                                        val favorite = PodcastFavorite(
                                            episodeId = task.episodeId,
                                            origin = task.origin,
                                            timestamp = System.currentTimeMillis(),
                                            title = task.episodeTitle.ifEmpty { task.episodeId },
                                            podcastTitle = task.podcastTitle,
                                            audioUrl = task.audioUrl,
                                            imageUrl = null,
                                            duration = 0,
                                            pubDate = 0L
                                        )
                                        val nowFavorite = if (task.episodeId in favoriteIds) {
                                            database.favorites.delete(task.episodeId)
                                            false
                                        } else {
                                            database.favorites.insert(favorite)
                                            true
                                        }
                                        favoriteIds = if (nowFavorite) favoriteIds + task.episodeId else favoriteIds - task.episodeId
                                        onFavoriteChanged()
                                    }
                                },
                                onResume = onResumeDownload,
                                onCancel = onCancelDownload,
                                colors = colors
                            )
                            HorizontalDivider(color = colors.divider)
                        }
                    }
                }

                // ── Completed Section ──
                if (hasCompleted) {
                    item(key = "section_completed") {
                        Spacer(modifier = Modifier.height(spacing.md))
                        Text(
                            text = Strings["downloads_completed"],
                            fontSize = DesignTokens.SectionHeader.TitleSize,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Serif,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(spacing.sm))
                    }

                    items(groupedByPodcast, key = { "group_${it.origin}" }) { group ->
                        PodcastDownloadGroup(
                            group = group,
                            playerState = playerState,
                            contextItems = downloadContextItems,
                            favoriteIds = favoriteIds,
                            onToggleFavorite = { download ->
                                scope.launch {
                                    val episode = database.episodes.getById(download.episodeId)
                                    val nowFavorite = if (download.episodeId in favoriteIds) {
                                        database.favorites.delete(download.episodeId)
                                        false
                                    } else if (episode != null) {
                                        database.favorites.insert(episode)
                                        true
                                    } else {
                                        database.favorites.insert(PodcastFavorite(
                                            episodeId = download.episodeId,
                                            origin = download.origin,
                                            timestamp = System.currentTimeMillis(),
                                            title = download.episodeTitle.ifEmpty { download.episodeId },
                                            podcastTitle = download.podcastTitle,
                                            imageUrl = null,
                                            audioUrl = download.filePath,
                                            duration = 0,
                                            pubDate = 0L
                                        ))
                                        true
                                    }
                                    favoriteIds = if (nowFavorite) favoriteIds + download.episodeId else favoriteIds - download.episodeId
                                    onFavoriteChanged()
                                }
                            },
                            onDeleteSingle = onDeleteDownloaded,
                            onDeleteAll = {
                                showBatchDeleteConfirm = group.origin to group.podcastTitle
                            },
                            colors = colors
                        )
                        HorizontalDivider(color = colors.divider)
                    }
                }
            }
        }
    }

    // ── Single delete confirm dialog ──
    showDeleteConfirm?.let { episodeId ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text(Strings["downloads_delete"], color = colors.textPrimary) },
            text = { Text(Strings["downloads_delete_confirm"], color = colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteDownloaded(episodeId)
                    showDeleteConfirm = null
                }) {
                    Text(Strings["downloads_delete"], color = colors.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text(Strings["dialog_cancel"], color = colors.textSecondary)
                }
            },
            containerColor = colors.surface
        )
    }

    // ── Batch delete confirm dialog ──
    showBatchDeleteConfirm?.let { (origin, title) ->
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirm = null },
            title = { Text(Strings["downloads_delete_all"], color = colors.textPrimary) },
            text = {
                Text(
                    Strings.get("downloads_delete_podcast_confirm", title),
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteDownloadedByOrigin(origin)
                    showBatchDeleteConfirm = null
                }) {
                    Text(Strings["downloads_delete"], color = colors.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteConfirm = null }) {
                    Text(Strings["dialog_cancel"], color = colors.textSecondary)
                }
            },
            containerColor = colors.surface
        )
    }
}

// ── In Progress Row ──
@Composable
private fun InProgressRow(
    episodeId: String,
    podcastTitle: String,
    episodeTitle: String,
    currentBytes: Long,
    totalBytes: Long,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onCancel: (String) -> Unit,
    colors: app.podara.theme.PodaraColors
) {
    val progress = if (totalBytes > 0) (currentBytes.toFloat() / totalBytes) else 0f
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val animatedBg by animateColorAsState(
        targetValue = if (isHovered) colors.elevated else Color.Transparent,
        animationSpec = tween(durationMillis = 150),
        label = "rowBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .background(animatedBg)
            .padding(start = 12.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Icon placeholder
        Box(
            modifier = Modifier
                .size(DesignTokens.EpisodeRow.CoverSize)
                .clip(RoundedCornerShape(DesignTokens.EpisodeRow.CoverRadius))
                .background(colors.elevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.FileDownload,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(28.dp)
            )
        }

        // Info + Progress
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = episodeTitle.ifEmpty { episodeId },
                color = colors.textPrimary,
                fontSize = DesignTokens.EpisodeRow.TitleSize,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = podcastTitle,
                color = colors.accent,
                fontSize = DesignTokens.EpisodeRow.AuthorSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Progress bar
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = colors.accent,
                trackColor = colors.elevated,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "${formatFileSize(currentBytes)} / ${formatFileSize(totalBytes)}",
                    color = colors.textMuted,
                    fontSize = 11.sp
                )
                if (totalBytes > 0) {
                    Text(
                        text = "(${(progress * 100).toInt()}%)",
                        color = colors.textMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Action buttons
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            FavoriteEpisodeButton(isFavorite = isFavorite, enabled = episodeTitle.isNotBlank() || podcastTitle.isNotBlank()) {
                onToggleFavorite()
            }
            // Pause / Resume toggle (resume only if paused state from DB — already handled elsewhere)
            if (currentBytes > 0) {
                ActionIconButton(
                    icon = Icons.Default.Pause,
                    description = Strings["downloads_pause"],
                    onClick = { onPause(episodeId) },
                    colors = colors
                )
            }
            // Cancel
            ActionIconButton(
                icon = Icons.Default.Close,
                description = Strings["downloads_cancel"],
                onClick = { onCancel(episodeId) },
                colors = colors
            )
        }
    }
}

// ── Paused / Failed Task Row ──
@Composable
private fun PausedTaskRow(
    task: app.podara.data.DownloadTask,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onResume: (String) -> Unit,
    onCancel: (String) -> Unit,
    colors: app.podara.theme.PodaraColors
) {
    val isPaused = task.state == "PAUSED"
    val isFailed = task.state == "FAILED"
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val animatedBg by animateColorAsState(
        targetValue = if (isHovered) colors.elevated else Color.Transparent,
        animationSpec = tween(durationMillis = 150),
        label = "rowBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(animatedBg)
            .padding(start = 12.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(DesignTokens.EpisodeRow.CoverSize)
                .clip(RoundedCornerShape(DesignTokens.EpisodeRow.CoverRadius))
                .background(if (isFailed) colors.danger.copy(alpha = 0.15f) else colors.elevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isFailed) Icons.Default.Error else Icons.Default.PauseCircle,
                contentDescription = null,
                tint = if (isFailed) colors.danger else colors.textSecondary,
                modifier = Modifier.size(28.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.episodeTitle.ifEmpty { task.episodeId },
                color = colors.textPrimary,
                fontSize = DesignTokens.EpisodeRow.TitleSize,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = task.podcastTitle,
                color = colors.accent,
                fontSize = DesignTokens.EpisodeRow.AuthorSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (isPaused) Strings["downloads_status_paused"]
                       else Strings["downloads_status_failed"],
                color = if (isFailed) colors.danger else colors.textMuted,
                fontSize = 11.sp
            )
            if (task.downloadedBytes > 0) {
                Text(
                    text = "${formatFileSize(task.downloadedBytes)} / ${formatFileSize(task.totalBytes)}",
                    color = colors.textMuted,
                    fontSize = 11.sp
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            FavoriteEpisodeButton(isFavorite = isFavorite) {
                onToggleFavorite()
            }
            if (isPaused || isFailed) {
                ActionIconButton(
                    icon = Icons.Default.PlayArrow,
                    description = Strings["downloads_resume"],
                    onClick = { onResume(task.episodeId) },
                    colors = colors
                )
            }
            ActionIconButton(
                icon = Icons.Default.Close,
                description = Strings["downloads_cancel"],
                onClick = { onCancel(task.episodeId) },
                colors = colors
            )
        }
    }
}

// ── Podcast download group (Completed) ──
@Composable
private fun PodcastDownloadGroup(
    group: PodcastGroup,
    playerState: MediaPlayerState?,
    contextItems: List<QueueItem>,
    favoriteIds: Set<String>,
    onToggleFavorite: (PodcastDownload) -> Unit,
    onDeleteSingle: (String) -> Unit,
    onDeleteAll: () -> Unit,
    colors: app.podara.theme.PodaraColors
) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        // Group header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Podcasts,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = group.podcastTitle,
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "(${group.items.size})",
                    color = colors.textMuted,
                    fontSize = 12.sp
                )
            }
            // Delete All button
            val daInteractionSource = remember { MutableInteractionSource() }
            val isDaHovered by daInteractionSource.collectIsHoveredAsState()
            val daAnimatedBg by animateColorAsState(
                targetValue = if (isDaHovered) colors.elevated else Color.Transparent,
                animationSpec = tween(durationMillis = 150),
                label = "deleteAllBg"
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(daAnimatedBg)
                    .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                    .clickable(interactionSource = daInteractionSource, indication = null) { onDeleteAll() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = Strings["downloads_delete_all"],
                    color = colors.danger,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Items
        group.items.forEachIndexed { index, download ->
            CompletedDownloadRow(
                download = download,
                playerState = playerState,
                contextItems = contextItems,
                isFavorite = download.episodeId in favoriteIds,
                onToggleFavorite = { onToggleFavorite(download) },
                onDelete = { onDeleteSingle(download.episodeId) },
                isLast = index == group.items.lastIndex,
                colors = colors
            )
        }
    }
}

// ── Completed download row ──
@Composable
private fun CompletedDownloadRow(
    download: PodcastDownload,
    playerState: MediaPlayerState?,
    contextItems: List<QueueItem>,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    isLast: Boolean,
    colors: app.podara.theme.PodaraColors
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val animatedBg by animateColorAsState(
        targetValue = if (isHovered) colors.elevated else Color.Transparent,
        animationSpec = tween(durationMillis = 150),
        label = "rowBg"
    )
    val file = remember(download) { File(download.filePath) }
    val fileExists = remember(download) { file.exists() }
    val fileSize = remember(download) { if (file.exists()) file.length() else 0L }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .background(animatedBg)
            .then(
                if (playerState != null && fileExists) {
                    Modifier
                        .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {
                            playerState.playWithContext(
                                context = contextItems,
                                targetUrl = download.filePath,
                                title = download.episodeTitle.ifEmpty { download.episodeId },
                                subtitle = download.podcastTitle,
                                episodeId = download.episodeId
                            )
                        }
                } else Modifier
            )
            .padding(start = 12.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(DesignTokens.EpisodeRow.CoverSize)
                .clip(RoundedCornerShape(DesignTokens.EpisodeRow.CoverRadius))
                .background(if (fileExists) colors.accent.copy(alpha = 0.1f) else colors.danger.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (fileExists) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = if (fileExists) colors.success else colors.danger,
                modifier = Modifier.size(28.dp)
            )
        }

        // Info column
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = download.episodeTitle.ifEmpty { download.episodeId },
                color = colors.textPrimary,
                fontSize = DesignTokens.EpisodeRow.TitleSize,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            if (!fileExists) {
                Text(
                    text = Strings["downloads_file_missing"],
                    color = colors.danger,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = formatFileSize(fileSize),
                        color = colors.textMuted,
                        fontSize = 11.sp
                    )
                    Text(
                        text = formatTimestamp(download.timestamp),
                        color = colors.textMuted,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = download.filePath,
                    color = colors.textDisabled,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Actions
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            FavoriteEpisodeButton(isFavorite = isFavorite) {
                onToggleFavorite()
            }
            // Open folder
            val ofInteractionSource = remember { MutableInteractionSource() }
            val isOfHovered by ofInteractionSource.collectIsHoveredAsState()
            val ofAnimatedBg by animateColorAsState(
                targetValue = if (isOfHovered) colors.elevated else Color.Transparent,
                animationSpec = tween(durationMillis = 150),
                label = "openFolderBg"
            )
            if (fileExists) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ofAnimatedBg)
                        .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                        .clickable(interactionSource = ofInteractionSource, indication = null) {
                            try {
                                Desktop.getDesktop().open(file.parentFile)
                            } catch (_: Exception) {}
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = Strings["downloads_open_folder"],
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Delete
            ActionIconButton(
                icon = Icons.Default.Delete,
                description = Strings["downloads_delete"],
                onClick = onDelete,
                colors = colors
            )
        }
    }
}

// ── Reusable action icon button ──
@Composable
private fun ActionIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    colors: app.podara.theme.PodaraColors
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val animatedBg by animateColorAsState(
        targetValue = if (isHovered) colors.elevated else Color.Transparent,
        animationSpec = tween(durationMillis = 150),
        label = "iconBg"
    )
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(animatedBg)
            .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = colors.textSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ── Helpers ──
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        else -> "%.1f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    }
}

private fun formatTimestamp(timestamp: Long): String {
    if (timestamp <= 0) return ""
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
