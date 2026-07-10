package app.podara

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowScope
import java.awt.Cursor
import coil3.compose.AsyncImage
import androidx.compose.ui.graphics.painter.BitmapPainter
import app.podara.api.apple.ApplePodcastClient
import app.podara.api.model.PodcastPreviewModel
import app.podara.api.rss.FetchPodcastClient
import app.podara.api.rss.FetchPodcastClientResult
import app.podara.component.AddToQueueButton
import app.podara.component.EpisodeActionIconButton
import app.podara.component.EpisodeListItem
import app.podara.component.EpisodeListItemSecondaryTextRole
import app.podara.component.FavoriteEpisodeButton
import app.podara.component.formatEpisodeMetadata
import app.podara.component.PodaraDropdownMenu
import app.podara.component.PodaraDropdownMenuItem
import app.podara.component.PodaraEmptyState
import app.podara.data.AppDatabase
import app.podara.data.model.Podcast
import app.podara.data.model.PodcastEpisode
import app.podara.player.FullPlayer
import app.podara.player.MediaPlayerState
import app.podara.player.MiniPlayer
import app.podara.player.QueueDrawer
import app.podara.player.QueueItem
import app.podara.screen.DiscoverScreen
import app.podara.screen.DownloadsScreen
import app.podara.screen.FavoritesScreen
import app.podara.screen.HistoryScreen
import app.podara.screen.SettingsScreen
import app.podara.manager.AddPodcastResult
import app.podara.manager.DownloadManager
import app.podara.manager.PodcastManager
import app.podara.manager.SubscriptionManager
import app.podara.manager.UpdatePodcastResult
import app.podara.theme.DesignTokens
import app.podara.theme.PodaraTheme
import app.podara.util.Logger
import app.podara.util.RssConverter
import app.podara.util.Settings
import app.podara.util.Strings
import app.podara.util.SystemTrayManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*

private val SidebarActiveBg = Color(0x14FFFFFF)

@Composable
private fun Sidebar(
    currentScreen: String,
    onDiscover: () -> Unit,
    onShows: () -> Unit,
    onFavorites: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onDownloads: () -> Unit = {}
) {
    data class NavItem(val icon: androidx.compose.ui.graphics.vector.ImageVector, val labelKey: String, val screen: String)

    val navItems = listOf(
        NavItem(Icons.Default.Explore, "nav_discover", "discover"),
        NavItem(Icons.Default.LibraryMusic, "nav_subscriptions", "home"),
        NavItem(Icons.Default.Favorite, "nav_favorites", "favorites"),
        NavItem(Icons.Default.QueueMusic, "nav_history", "history"),
        NavItem(Icons.Default.Folder, "nav_downloads", "downloads")
    )

    val colors = PodaraTheme.colors
    val sidebar = DesignTokens.Sidebar

    Surface(
        modifier = Modifier.width(sidebar.Width).fillMaxHeight(),
        color = colors.surface
    ) {
        Column(
            modifier = Modifier.fillMaxHeight().padding(top = sidebar.PaddingVertical, bottom = 0.dp)
        ) {
            Row(
                modifier = Modifier.padding(start = sidebar.PaddingHorizontal + 6.dp, end = sidebar.PaddingHorizontal, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val logoPainter = remember {
                    val loader = object {}::class.java.classLoader
                    val bytes = loader.getResourceAsStream("logo-64.png")!!.readBytes()
                    BitmapPainter(bytes.decodeToImageBitmap())
                }
                Image(
                    painter = logoPainter,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(sidebar.LogoSize)
                        .clip(RoundedCornerShape(8.dp))
                )
                Text(
                    text = Strings["app_name"],
                    color = colors.textPrimary,
                    fontSize = sidebar.LogoTextSize,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            navItems.forEach { item ->
                val isActive = currentScreen == item.screen
                val interactionSource = remember(item.screen) { MutableInteractionSource() }
                val isHovered by interactionSource.collectIsHoveredAsState()
                val animatedBg by animateColorAsState(if (isActive || isHovered) SidebarActiveBg else Color.Transparent, tween(150))
                val activeGlass = DesignTokens.Navigation.ActiveGlass
                val itemShape = RoundedCornerShape(activeGlass.Radius)
                val hoverShape = itemShape
                val iconTint = if (isActive) colors.accent else colors.textSecondary

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(sidebar.NavItemHeight)
                            .padding(horizontal = sidebar.NavItemPadding)
                            .let { mod ->
                                if (isActive) {
                                    mod.shadow(activeGlass.ShadowElevation, itemShape, ambientColor = activeGlass.ShadowColor, spotColor = activeGlass.ShadowColor)
                                        .border(activeGlass.BorderWidth, activeGlass.Border, itemShape)
                                        .clip(itemShape)
                                        .background(activeGlass.BaseColor)
                                        .background(activeGlass.LeftGlow)
                                        .background(activeGlass.TopGlow)
                                        .background(activeGlass.RightGlow)
                                } else {
                                    mod.background(animatedBg, hoverShape)
                                }
                            }
                            .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                            .clickable(interactionSource = interactionSource, indication = null) {
                                when (item.screen) {
                                    "discover" -> onDiscover()
                                    "home" -> onShows()
                                    "favorites" -> onFavorites()
                                    "history" -> onHistory()
                                    "settings" -> onSettings()
                                    "downloads" -> onDownloads()
                                }
                            }
                            .padding(horizontal = activeGlass.InnerPaddingHorizontal),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(item.icon, contentDescription = Strings[item.labelKey], tint = iconTint, modifier = Modifier.size(sidebar.NavIconSize))
                            Text(
                                text = Strings[item.labelKey],
                                color = if (isActive) colors.textPrimary else colors.textSecondary,
                                fontSize = sidebar.NavTextSize
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            HorizontalDivider(color = colors.divider, modifier = Modifier.padding(horizontal = sidebar.DividerPadding))

            Spacer(modifier = Modifier.height(8.dp))

            val settingsActive = currentScreen == "settings"
            val settingsInteractionSource = remember { MutableInteractionSource() }
            val settingsIsHovered by settingsInteractionSource.collectIsHoveredAsState()
            val settingsAnimatedBg by animateColorAsState(if (settingsActive || settingsIsHovered) SidebarActiveBg else Color.Transparent, tween(150))
            val settingsActiveGlass = DesignTokens.Navigation.ActiveGlass
            val settingsShape = RoundedCornerShape(settingsActiveGlass.Radius)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(sidebar.NavItemHeight)
                    .padding(horizontal = sidebar.NavItemPadding)
                    .let { mod ->
                        if (settingsActive) {
                            mod.shadow(settingsActiveGlass.ShadowElevation, settingsShape, ambientColor = settingsActiveGlass.ShadowColor, spotColor = settingsActiveGlass.ShadowColor)
                                .border(settingsActiveGlass.BorderWidth, settingsActiveGlass.Border, settingsShape)
                                .clip(settingsShape)
                                .background(settingsActiveGlass.BaseColor)
                                .background(settingsActiveGlass.LeftGlow)
                                .background(settingsActiveGlass.TopGlow)
                                .background(settingsActiveGlass.RightGlow)
                        } else {
                            mod.background(settingsAnimatedBg, settingsShape)
                        }
                    }
                    .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                    .clickable(interactionSource = settingsInteractionSource, indication = null) { onSettings() }
                    .padding(horizontal = settingsActiveGlass.InnerPaddingHorizontal),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Settings, contentDescription = Strings["nav_settings"], tint = if (settingsActive) colors.accent else colors.textSecondary, modifier = Modifier.size(sidebar.NavIconSize))
                    Text(
                        text = Strings["nav_settings"],
                        color = if (settingsActive) colors.textPrimary else colors.textSecondary,
                        fontSize = sidebar.NavTextSize
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

private const val TAG = "App"

private fun logError(e: Throwable) {
    Logger.e(TAG, "Uncaught error: ${e.message}", e)
    try {
        val logFile = File(System.getProperty("user.home"), ".podara/crash.log")
        logFile.parentFile?.mkdirs()
        logFile.appendText(
            "[${SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())}] ${e.message}\n${e.stackTraceToString()}\n\n"
        )
    } catch (_: Exception) {}
}

private suspend fun playAndRecordHistory(
    database: AppDatabase,
    playerState: MediaPlayerState,
    episode: PodcastEpisode,
    podcastImageUrl: String? = null
) {
    Logger.i(TAG, "playAndRecordHistory: title=${episode.title}, url=${episode.audioUrl}")
    try {
        val podcast = database.podcasts.getByOrigin(episode.origin)
        database.episodes.insert(episode)
        playerState.play(
            url = episode.audioUrl,
            title = episode.title,
            subtitle = episode.podcastTitle,
            artworkUrl = episode.imageUrl,
            podcastArtworkUrl = podcast?.imageUrl ?: podcastImageUrl,
            durationMs = episode.duration * 1000L,
            episodeId = episode.id
        )
        database.history.insert(episode.origin, episode.id)
        Logger.d(TAG, "History recorded for episode: ${episode.id}")
    } catch (e: Exception) {
        Logger.e(TAG, "Failed to play episode: ${episode.title}", e)
        throw e
    }
}

@Composable
private fun WindowControlButton(
    onClick: () -> Unit,
    icon: @Composable (tint: Color) -> Unit,
    isClose: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val animatedBg by animateColorAsState(
        when {
            isClose && isHovered -> Color(0xFFE81123)
            isHovered -> Color(0x18FFFFFF)
            else -> Color.Transparent
        },
        tween(150)
    )
    val iconTint = when {
        isClose && isHovered -> Color.White
        isHovered -> Color.White
        else -> Color(0xFF999999)
    }

    Box(
        modifier = Modifier
            .size(36.dp)
            .background(animatedBg)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        icon(iconTint)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WindowScope.App(
    windowState: androidx.compose.ui.window.WindowState,
    awtWindow: java.awt.Window,
    closeRequestCount: Int = 0
) {
    val database = remember {
        try {
            val userHome = System.getProperty("user.home")
            val dbDir = File(userHome, ".podara")
            dbDir.mkdirs()
            Logger.i(TAG, "Initializing database at ${dbDir.absolutePath}")
            AppDatabase.build(File(dbDir, "podara.db"))
        } catch (e: Exception) {
            logError(e)
            throw e
        }
    }

    val podcastManager = remember { PodcastManager(database) }
    val subscriptionManager = remember { SubscriptionManager(database) }
    val appleClient = remember { ApplePodcastClient() }
    DisposableEffect(Unit) {
        onDispose { appleClient.close() }
    }
    var downloadPath by remember { mutableStateOf(Settings.getDownloadPath()) }
    var downloadSpeedLimitKbps by remember { mutableStateOf(Settings.getDownloadSpeedLimitKbps()) }
    val downloadManager = remember(downloadPath, downloadSpeedLimitKbps) {
        val downloadsDir = File(downloadPath)
        downloadsDir.mkdirs()
        DownloadManager(database, downloadsDir, downloadSpeedLimitKbps)
    }
    val playerState = remember { MediaPlayerState() }

    // Restore saved queue and playback state on startup
    LaunchedEffect(Unit) {
        playerState.restoreSession(database)
    }

    val trayManager = remember { SystemTrayManager(awtWindow, playerState) }
    // Save session before quitting from tray
    trayManager.onBeforeQuit = {
        runBlocking { playerState.saveSession(database) }
    }

    // Periodic heartbeat: save playback position every 30 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            if (playerState.currentUrl != null) {
                playerState.savePosition(database)
            }
        }
    }

    DisposableEffect(Unit) {
        trayManager.setup()
        onDispose { trayManager.remove() }
    }

    LaunchedEffect(playerState.isPlaying) {
        trayManager.updatePlayPauseLabel(playerState.isPlaying)
    }
    val fetchPodcastClient = remember { FetchPodcastClient() }
    DisposableEffect(Unit) {
        onDispose { playerState.release() }
    }
    val scope = rememberCoroutineScope()

    var podcasts by remember { mutableStateOf(emptyList<Podcast>()) }
    var currentScreen by remember { mutableStateOf("discover") }
    var selectedPodcast by remember { mutableStateOf<Podcast?>(null) }
    var discoverRefreshKey by remember { mutableStateOf(0) }
    var showFullPlayer by remember { mutableStateOf(false) }
    var showQueueFromMini by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showCloseDialog by remember { mutableStateOf(false) }
    var addError by remember { mutableStateOf<String?>(null) }
    var downloadProgress by remember { mutableStateOf(mapOf<String, Pair<Long, Long>>()) }
    var downloadingEpisodes by remember { mutableStateOf(setOf<String>()) }
    var downloadVersion by remember { mutableIntStateOf(0) }
    var favoritesVersion by remember { mutableIntStateOf(0) }
    var completedDownloads by remember { mutableStateOf(setOf<String>()) }
    var downloadJobs by remember { mutableStateOf(mapOf<String, Job>()) }
    var activeDownloadMeta by remember { mutableStateOf(mapOf<String, Pair<String, String>>()) } // episodeId -> (podcastTitle, episodeTitle)

    val handleCloseRequest = {
        val action = Settings.getCloseAction()
        if (Settings.isCloseActionRemembered()) {
            when (action) {
                "quit" -> {
                    runBlocking { playerState.saveSession(database) }
                    awtWindow.dispose(); System.exit(0)
                }
                "minimize_to_tray" -> {
                    awtWindow.isVisible = false
                    trayManager.updateShowHideLabel(false)
                }
                else -> { showCloseDialog = true }
            }
        } else {
            showCloseDialog = true
        }
    }

    LaunchedEffect(closeRequestCount) {
        if (closeRequestCount > 0) handleCloseRequest()
    }

    LaunchedEffect(Unit) {
        Logger.d(TAG, "Loading podcasts from database")
        podcasts = database.podcasts.getAllSync()
        completedDownloads = database.downloads.getAllValidDownloadedIds()
        Logger.d(TAG, "Loaded ${podcasts.size} podcasts, ${completedDownloads.size} downloads")
    }

    val startDownload: (PodcastEpisode, String) -> Unit = { episode, podcastTitle ->
        // Guard: don't start a new download if already downloading
        if (episode.id !in downloadingEpisodes) {
            downloadingEpisodes = downloadingEpisodes + episode.id
            completedDownloads = completedDownloads - episode.id
            activeDownloadMeta = activeDownloadMeta + (episode.id to (podcastTitle to episode.title))
            val job = scope.launch {
                try {
                    val result = downloadManager.downloadEpisode(
                        episodeId = episode.id,
                        audioUrl = episode.audioUrl,
                        origin = episode.origin,
                        episodeTitle = episode.title,
                        podcastTitle = podcastTitle,
                        onProgress = { current, total ->
                            downloadProgress = downloadProgress + (episode.id to Pair(current, total))
                        }
                    )
                    if (result.isSuccess) {
                        completedDownloads = completedDownloads + episode.id
                    }
                } finally {
                    downloadingEpisodes = downloadingEpisodes - episode.id
                    downloadProgress = downloadProgress - episode.id
                    activeDownloadMeta = activeDownloadMeta - episode.id
                    downloadJobs = downloadJobs - episode.id
                    downloadVersion++
                }
            }
            downloadJobs = downloadJobs + (episode.id to job)
        }
        Unit
    }

    // ── Download management callbacks ──
    val pauseDownload: (String) -> Unit = { episodeId ->
        downloadManager.pauseDownload(episodeId)
    }

    val resumeDownload: (String) -> Unit = { episodeId ->
        // Guard: don't resume if already downloading
        if (episodeId !in downloadingEpisodes) {
            scope.launch {
                // Track in downloadingEpisodes so DownloadsScreen shows it as in-progress
                downloadingEpisodes = downloadingEpisodes + episodeId
                try {
                    val result = downloadManager.resumeDownload(episodeId) { current, total ->
                        downloadProgress = downloadProgress + (episodeId to Pair(current, total))
                    }
                    if (result.isSuccess) {
                        completedDownloads = completedDownloads + episodeId
                    }
                } finally {
                    downloadingEpisodes = downloadingEpisodes - episodeId
                    downloadProgress = downloadProgress - episodeId
                    downloadVersion++ // trigger UI refresh to show completed / removed
                }
            }
        }
        Unit
    }

    val cancelDownload: (String) -> Unit = { episodeId ->
        downloadManager.cancelDownload(episodeId)
        downloadJobs[episodeId]?.cancel()
        downloadingEpisodes = downloadingEpisodes - episodeId
        downloadProgress = downloadProgress - episodeId
        activeDownloadMeta = activeDownloadMeta - episodeId
        downloadJobs = downloadJobs - episodeId
        downloadVersion++
        // Also clean up any paused/failed task in DB and partial files
        scope.launch {
            downloadManager.cleanupPausedTask(episodeId)
        }
        Unit
    }

    val deleteDownloaded: (String) -> Unit = { episodeId ->
        scope.launch {
            downloadManager.deleteDownloadedEpisode(episodeId)
            completedDownloads = completedDownloads - episodeId
            downloadVersion++
        }
        Unit
    }

    val deleteDownloadedByOrigin: (String) -> Unit = { origin ->
        scope.launch {
            downloadManager.deleteDownloadedByOrigin(origin)
            completedDownloads = database.downloads.getAllValidDownloadedIds()
            downloadVersion++
        }
        Unit
    }

    // ── Play latest episode from FeaturedCard without subscribing ──
    // Uses iTunes Lookup API (entity=podcastEpisode) for fast response.
    val onPlayLatestEpisode: (PodcastPreviewModel) -> Unit = { preview ->
        scope.launch {
            try {
                val collectionId = when {
                    preview.fetchUrl.startsWith("itunes-lookup:") ->
                        preview.fetchUrl.removePrefix("itunes-lookup:").toLongOrNull()
                    else -> null
                } ?: return@launch

                val episodes = appleClient.lookup.lookupLatestEpisodes(collectionId, 1)
                val episode = episodes.firstOrNull() ?: return@launch
                val audioUrl = episode.episodeUrl ?: episode.previewUrl ?: return@launch

                Logger.i(TAG, "onPlayLatestEpisode: playing ${episode.trackName} from ${episode.collectionName}")
                playerState.play(
                    url = audioUrl,
                    title = episode.trackName ?: "",
                    subtitle = episode.collectionName,
                    artworkUrl = episode.artworkUrl600,
                    podcastArtworkUrl = preview.imageUrl,
                    durationMs = episode.trackTimeMillis ?: 0L,
                    episodeId = episode.episodeGuid ?: episode.trackId?.toString() ?: ""
                )
                database.history.insert(
                    origin = episode.feedUrl ?: preview.fetchUrl,
                    episodeId = episode.episodeGuid ?: episode.trackId?.toString() ?: ""
                )
            } catch (e: Exception) {
                Logger.e(TAG, "onPlayLatestEpisode failed", e)
            }
        }
    }

    // ── Navigate to podcast detail (RSS fetch only, no subscribe) ──
    val onShowDetail: (PodcastPreviewModel) -> Unit = { preview ->
        scope.launch {
            try {
                // 1. Resolve the RSS feed URL and persist itunes-lookup mapping
                val feedUrl = if (preview.fetchUrl.startsWith("itunes-lookup:")) {
                    val id = preview.fetchUrl.removePrefix("itunes-lookup:").toLongOrNull()
                        ?: return@launch
                    val lookupResult = appleClient.lookup.lookupById(id)
                    val resolvedUrl = lookupResult?.fetchUrl ?: return@launch
                    // Persist mapping so DiscoverScreen can check subscription status later
                    database.itunesLookup.insert(preview.fetchUrl, resolvedUrl)
                    resolvedUrl
                } else {
                    preview.fetchUrl
                }

                // 2. Navigate immediately with a minimal Podcast
                //    PodcastDetailScreen will fetch episodes from RSS on its own
                selectedPodcast = Podcast(
                    origin = feedUrl,
                    link = preview.link,
                    title = preview.title,
                    description = preview.description,
                    author = preview.author,
                    imageUrl = preview.imageUrl,
                    imageSeedColor = 0,
                    languageCode = preview.languageCode,
                    fileSize = 0
                )
            } catch (e: Exception) {
                Logger.e(TAG, "onShowDetail failed", e)
            }
        }
    }

    PodaraTheme(darkTheme = true) {
        val titleBarColors = PodaraTheme.colors
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Custom Title Bar ──
            WindowDraggableArea {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .background(titleBarColors.background),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(12.dp))
                    val titleLogoPainter = remember {
                        val loader = object {}::class.java.classLoader
                        val bytes = loader.getResourceAsStream("logo-64.png")!!.readBytes()
                        BitmapPainter(bytes.decodeToImageBitmap())
                    }
                    Image(
                        painter = titleLogoPainter,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Strings["app_name"],
                        color = titleBarColors.textMuted,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    // Minimize
                    WindowControlButton(
                        onClick = { windowState.isMinimized = true },
                        icon = { tint -> Icon(Icons.Default.Remove, contentDescription = Strings["titlebar_minimize"], tint = tint, modifier = Modifier.size(14.dp)) }
                    )
                    // Maximize
                    WindowControlButton(
                        onClick = {
                            windowState.placement = if (windowState.placement == WindowPlacement.Maximized)
                                WindowPlacement.Floating else WindowPlacement.Maximized
                        },
                        icon = { tint ->
                            Icon(
                                if (windowState.placement == WindowPlacement.Maximized) Icons.Default.FilterNone else Icons.Default.CropSquare,
                                contentDescription = Strings["titlebar_maximize"],
                                tint = tint,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                    // Close
                    WindowControlButton(
                        onClick = { handleCloseRequest() },
                        icon = { tint -> Icon(Icons.Default.Close, contentDescription = Strings["titlebar_close"], tint = tint, modifier = Modifier.size(14.dp)) },
                        isClose = true
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Sidebar(
                        currentScreen = currentScreen,
                        onDiscover = { currentScreen = "discover"; showFullPlayer = false; selectedPodcast = null },
                        onShows = { currentScreen = "home"; showFullPlayer = false; selectedPodcast = null },
                        onFavorites = { currentScreen = "favorites"; showFullPlayer = false; selectedPodcast = null },
                        onHistory = { currentScreen = "history"; showFullPlayer = false; selectedPodcast = null },
                        onSettings = { currentScreen = "settings"; showFullPlayer = false; selectedPodcast = null },
                        onDownloads = { currentScreen = "downloads"; showFullPlayer = false; selectedPodcast = null }
                    )

                    // Main content (hidden when FullPlayer is showing)
                    Box(modifier = Modifier.weight(1f)) {
                        if (!showFullPlayer) {
                        when {
                            selectedPodcast != null -> PodcastDetailScreen(
                            podcast = selectedPodcast!!,
                            database = database,
                            subscriptionManager = subscriptionManager,
                            fetchPodcastClient = fetchPodcastClient,
                            playerState = playerState,
                            downloadManager = downloadManager,
                            downloadingEpisodes = downloadingEpisodes,
                            downloadProgress = downloadProgress,
                            downloadVersion = downloadVersion,
                            completedDownloads = completedDownloads,
                            favoriteVersion = favoritesVersion,
                            onStartDownload = startDownload,
                            onPauseDownload = pauseDownload,
                            onResumeDownload = resumeDownload,
                            onFavoriteChanged = { favoritesVersion++ },
                            onBack = {
                                selectedPodcast = null
                                discoverRefreshKey++
                            },
                            onUnsubscribed = {
                                podcasts = database.podcasts.getAllSync()
                            },
                            onSubscribed = {
                                podcasts = database.podcasts.getAllSync()
                                discoverRefreshKey++
                            }
                        )
                        currentScreen == "home" -> HomeScreen(
                            podcasts = podcasts,
                            database = database,
                            subscriptionManager = subscriptionManager,
                            scope = scope,
                            onPodcastClick = { podcast -> selectedPodcast = podcast },
                            onAddPodcast = { showAddDialog = true },
                            onDiscover = { currentScreen = "discover" },
                            onHistory = { currentScreen = "history" },
                            onSettings = { currentScreen = "settings" },
                            onPodcastsChanged = { newPodcasts -> podcasts = newPodcasts }
                        )
                        currentScreen == "discover" -> DiscoverScreen(
                            database = database,
                            subscriptionManager = subscriptionManager,
                            discoverRefreshKey = discoverRefreshKey,
                            onSubscribed = {
                                scope.launch { podcasts = database.podcasts.getAllSync() }
                            },
                            onBack = {
                                currentScreen = "home"
                                scope.launch { podcasts = database.podcasts.getAllSync() }
                            },
                            onPlayLatestEpisode = onPlayLatestEpisode,
                            onShowDetail = onShowDetail
                        )
                        currentScreen == "settings" -> SettingsScreen(
                            database = database,
                            onBack = { currentScreen = "home" },
                            onDownloadPathChanged = { newPath -> downloadPath = newPath },
                            onLanguageChanged = { /* Language change is handled by Settings */ },
                            downloadSpeedLimitKbps = downloadSpeedLimitKbps,
                            onDownloadSpeedLimitChanged = { limit -> downloadSpeedLimitKbps = limit }
                        )
                        currentScreen == "history" -> HistoryScreen(
                            database = database,
                            playerState = playerState,
                            favoriteVersion = favoritesVersion,
                            onBack = { currentScreen = "home" },
                            onFavoriteChanged = { favoritesVersion++ },
                            onShowPodcastDetail = { podcast -> selectedPodcast = podcast }
                        )
                        currentScreen == "favorites" -> FavoritesScreen(
                            database = database,
                            playerState = playerState,
                            favoriteVersion = favoritesVersion,
                            onBack = { currentScreen = "home" },
                            onFavoriteChanged = { favoritesVersion++ },
                            onShowPodcastDetail = { podcast -> selectedPodcast = podcast }
                        )
                        currentScreen == "downloads" -> DownloadsScreen(
                            database = database,
                            downloadManager = downloadManager,
                            downloadPath = downloadPath,
                            downloadingEpisodes = downloadingEpisodes,
                            downloadProgress = downloadProgress,
                            downloadVersion = downloadVersion,
                            completedDownloads = completedDownloads,
                            activeDownloadMeta = activeDownloadMeta,
                            playerState = playerState,
                            favoriteVersion = favoritesVersion,
                            onPauseDownload = pauseDownload,
                            onResumeDownload = resumeDownload,
                            onCancelDownload = cancelDownload,
                            onDeleteDownloaded = deleteDownloaded,
                            onDeleteDownloadedByOrigin = deleteDownloadedByOrigin,
                            onFavoriteChanged = { favoritesVersion++ },
                            onBack = { currentScreen = "home" },
                            onOpenSettings = { currentScreen = "settings" }
                        )   // DownloadsScreen
                        }   // when
                        }   // if
                    }   // content Box

            }   // Row close

                Box(Modifier.matchParentSize()) {
                    @Composable
                    fun FullPlayerOverlay() {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = showFullPlayer,
                            enter = slideInVertically(animationSpec = tween(400)) { it } + fadeIn(animationSpec = tween(300)),
                            exit = slideOutVertically(animationSpec = tween(300)) { it } + fadeOut(animationSpec = tween(200))
                        ) {
                            Box(Modifier.fillMaxSize()) {
                                FullPlayer(
                                    state = playerState,
                                    database = database,
                                    favoriteVersion = favoritesVersion,
                                    onFavoriteChanged = { favoritesVersion++ },
                                    onShowQueue = { showQueueFromMini = true },
                                    onClose = { showFullPlayer = false }
                                )
                            }
                        }
                    }
                    FullPlayerOverlay()
                }
            }

            MiniPlayer(
                state = playerState,
                onExpand = { showFullPlayer = true },
                onBodyClick = { showFullPlayer = !showFullPlayer },
                onShowQueue = { showQueueFromMini = true }
            )
        }
    }

    // Scrim — appears instantly, separate from panel animation
    if (showQueueFromMini) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable { showQueueFromMini = false }
        )
    }

    // Panel — slides in from right
    AnimatedVisibility(
        visible = showQueueFromMini,
        enter = slideInHorizontally(animationSpec = tween(300)) { it },
        exit = slideOutHorizontally(animationSpec = tween(250)) { it }
    ) {
        QueueDrawer(
            state = playerState,
            database = database,
            favoriteVersion = favoritesVersion,
            onFavoriteChanged = { favoritesVersion++ },
            onDismiss = { showQueueFromMini = false }
        )
    }

    if (showAddDialog) {
        AddPodcastDialog(
            onDismiss = {
                showAddDialog = false
                addError = null
            },
            onConfirm = { url ->
                showAddDialog = false
                scope.launch {
                    try {
                        when (val result = podcastManager.addPodcast(url, null)) {
                            is AddPodcastResult.Created -> {
                                podcasts = database.podcasts.getAllSync()
                            }
                            is AddPodcastResult.Duplicate -> {
                                addError = Strings.get("podcast_already_exists", result.duplicate.title)
                                showAddDialog = true
                            }
                        }
                    } catch (e: Exception) {
                        addError = Strings.get("error_adding_podcast", e.message ?: "")
                        showAddDialog = true
                    }
                }
            },
            error = addError
        )
    }

    // ── Close Behavior Dialog ──
    if (showCloseDialog) {
        val dialogColors = PodaraTheme.colors
        var chosenAction by remember { mutableStateOf(Settings.getCloseAction()) }
        var rememberChoice by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showCloseDialog = false },
            shape = RoundedCornerShape(0),
            modifier = Modifier.widthIn(max = 380.dp),
            containerColor = dialogColors.surface,
            title = {
                Text(
                    text = Strings["close_dialog_title"],
                    color = dialogColors.textPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = Strings["close_dialog_message"],
                        fontSize = 14.sp,
                        color = dialogColors.textSecondary
                    )
                    Spacer(Modifier.height(12.dp))

                    // Quit option
                    val quitInteractionSource = remember { MutableInteractionSource() }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .pointerHoverIcon(PointerIcon(java.awt.Cursor(java.awt.Cursor.HAND_CURSOR)))
                            .clickable(interactionSource = quitInteractionSource, indication = null) {
                                chosenAction = "quit"
                            }
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = chosenAction == "quit",
                            onClick = { chosenAction = "quit" },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = dialogColors.accent,
                                unselectedColor = dialogColors.textSecondary
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = Strings["close_action_quit"],
                                fontSize = 14.sp,
                                color = dialogColors.textPrimary
                            )
                        }
                    }

                    // Minimize to tray option
                    val minimizeInteractionSource = remember { MutableInteractionSource() }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .pointerHoverIcon(PointerIcon(java.awt.Cursor(java.awt.Cursor.HAND_CURSOR)))
                            .clickable(interactionSource = minimizeInteractionSource, indication = null) {
                                chosenAction = "minimize_to_tray"
                            }
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = chosenAction == "minimize_to_tray",
                            onClick = { chosenAction = "minimize_to_tray" },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = dialogColors.accent,
                                unselectedColor = dialogColors.textSecondary
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = Strings["close_action_minimize"],
                                fontSize = 14.sp,
                                color = dialogColors.textPrimary
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Remember choice checkbox
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .pointerHoverIcon(PointerIcon(java.awt.Cursor(java.awt.Cursor.HAND_CURSOR)))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { rememberChoice = !rememberChoice }
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = rememberChoice,
                            onCheckedChange = { rememberChoice = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = dialogColors.accent,
                                uncheckedColor = dialogColors.textSecondary,
                                checkmarkColor = dialogColors.surface
                            )
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = Strings["close_remember"],
                            fontSize = 13.sp,
                            color = dialogColors.textSecondary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (rememberChoice) {
                        Settings.setCloseAction(chosenAction)
                        Settings.setCloseActionRemembered(true)
                    }
                    when (chosenAction) {
                        "quit" -> {
                            runBlocking { playerState.saveSession(database) }
                            awtWindow.dispose(); System.exit(0)
                        }
                        "minimize_to_tray" -> {
                            awtWindow.isVisible = false
                            trayManager.updateShowHideLabel(false)
                        }
                    }
                    showCloseDialog = false
                }) {
                    Text(Strings["dialog_ok"], color = dialogColors.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloseDialog = false }) {
                    Text(Strings["dialog_cancel"], color = dialogColors.textSecondary)
                }
            }
        )
    }
}

internal fun sortPodcastsByLatestEpisodeDate(
    podcasts: List<Podcast>,
    latestEpisodePubDateMap: Map<String, Long>
): List<Podcast> = podcasts.sortedWith(
    compareByDescending<Podcast> { latestEpisodePubDateMap[it.origin] ?: 0L }
        .thenBy { it.fetchTitle() }
        .thenBy { it.origin }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    podcasts: List<Podcast>,
    database: AppDatabase,
    subscriptionManager: SubscriptionManager,
    scope: kotlinx.coroutines.CoroutineScope,
    onPodcastClick: (Podcast) -> Unit,
    onAddPodcast: () -> Unit,
    onDiscover: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onPodcastsChanged: (List<Podcast>) -> Unit
) {
    val colors = PodaraTheme.colors
    var isEditing by remember { mutableStateOf(false) }
    var selectedPodcasts by remember { mutableStateOf(setOf<String>()) }
    var showBatchUnsubscribeDialog by remember { mutableStateOf(false) }
    var showUnsubscribeDialog by remember { mutableStateOf(false) }
    var podcastToUnsubscribe by remember { mutableStateOf<Podcast?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var subscriptionMap by remember { mutableStateOf(mapOf<String, app.podara.data.model.PodcastSubscription>()) }
    var episodeCountMap by remember { mutableStateOf(mapOf<String, Int>()) }
    var latestEpisodePubDateMap by remember { mutableStateOf(mapOf<String, Long>()) }
    var lastListenedMap by remember { mutableStateOf(mapOf<String, Long>()) }
    var sortOption by remember { mutableStateOf("name_asc") }
    var showSortMenu by remember { mutableStateOf(false) }

    // Load subscription data and episode counts
    LaunchedEffect(podcasts) {
        if (podcasts.isEmpty()) return@LaunchedEffect
        val subs = database.subscriptions.getAllSync()
        subscriptionMap = subs.associateBy { it.origin }
        val counts = mutableMapOf<String, Int>()
        val latestPubDates = mutableMapOf<String, Long>()
        podcasts.forEach { p ->
            val episodes = database.episodes.getAllByOrigin(p.origin)
            counts[p.origin] = episodes.size
            latestPubDates[p.origin] = episodes.firstOrNull()?.pubDate ?: 0L
        }
        episodeCountMap = counts
        latestEpisodePubDateMap = latestPubDates
        lastListenedMap = database.history.getLatestTimestampPerOrigin()
    }

    val filteredPodcasts = remember(podcasts, searchQuery) {
        if (searchQuery.isBlank()) podcasts
        else podcasts.filter { it.title.contains(searchQuery, ignoreCase = true) || it.author.contains(searchQuery, ignoreCase = true) }
    }

    val sortedPodcasts = remember(filteredPodcasts, sortOption, latestEpisodePubDateMap, lastListenedMap) {
        when (sortOption) {
            "name_asc" -> filteredPodcasts.sortedBy { it.fetchTitle() }
            "name_desc" -> filteredPodcasts.sortedByDescending { it.fetchTitle() }
            "recent_update" -> sortPodcastsByLatestEpisodeDate(filteredPodcasts, latestEpisodePubDateMap)
            "recent_listen" -> filteredPodcasts.sortedByDescending { lastListenedMap[it.origin] ?: 0L }
            else -> filteredPodcasts
        }
    }

    // ── Empty state ──
    if (podcasts.isEmpty()) {
        PodaraEmptyState(
            icon = Icons.Default.RssFeed,
            title = Strings["home_empty"],
            subtitle = Strings["home_empty_hint"],
            modifier = Modifier.fillMaxSize().background(colors.background),
            iconTint = colors.accent,
            actionText = Strings["home_add_podcast"],
            actionIcon = Icons.Default.Add,
            onActionClick = onAddPodcast
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(
                top = DesignTokens.PageHeader.PaddingTop,
                start = DesignTokens.PageHeader.PaddingHorizontal,
                end = DesignTokens.PageHeader.PaddingHorizontal,
                bottom = DesignTokens.Spacing.sm
            )
    ) {
        // ── Header (hidden in edit mode, replaced by selection bar) ──
        if (isEditing) {
            val toolbarButton = DesignTokens.ToolbarButton
            // Selection header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Close button (32dp square matching Sort/Manage style)
                val closeInteractionSource = remember { MutableInteractionSource() }
                val isCloseHovered by closeInteractionSource.collectIsHoveredAsState()
                Box(
                    modifier = Modifier
                        .size(toolbarButton.Size)
                        .clip(RoundedCornerShape(toolbarButton.Radius))
                        .background(if (isCloseHovered) toolbarButton.HoverBackgroundColor else toolbarButton.BackgroundColor)
                        .border(toolbarButton.BorderWidth, toolbarButton.BorderColor, RoundedCornerShape(toolbarButton.Radius))
                        .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                        .clickable(interactionSource = closeInteractionSource, indication = null) {
                            isEditing = false; selectedPodcasts = emptySet()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, contentDescription = Strings["home_cancel"], tint = colors.textPrimary, modifier = Modifier.size(toolbarButton.IconSize))
                }

                Spacer(Modifier.width(DesignTokens.Spacing.md))

                Text(
                    text = Strings.get("home_selected_count", selectedPodcasts.size),
                    fontSize = toolbarButton.StrongTextSize,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )

                Spacer(Modifier.weight(1f))

                // Select all — clean text button
                Text(
                    text = Strings["home_select_all"],
                    color = colors.accent,
                    fontSize = toolbarButton.TextSize,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            selectedPodcasts = if (selectedPodcasts.size == filteredPodcasts.size) emptySet()
                            else filteredPodcasts.map { it.origin }.toSet()
                        }
                )

                Spacer(Modifier.width(DesignTokens.Spacing.md))

                // Delete button (32dp square with danger color)
                val deleteInteractionSource = remember { MutableInteractionSource() }
                val isDeleteHovered by deleteInteractionSource.collectIsHoveredAsState()
                Box(
                    modifier = Modifier
                        .size(toolbarButton.Size)
                        .clip(RoundedCornerShape(toolbarButton.Radius))
                        .background(if (isDeleteHovered) toolbarButton.DangerHoverBackgroundColor else toolbarButton.BackgroundColor)
                        .border(toolbarButton.BorderWidth, toolbarButton.BorderColor, RoundedCornerShape(toolbarButton.Radius))
                        .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                        .clickable(interactionSource = deleteInteractionSource, indication = null) { showBatchUnsubscribeDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Delete, contentDescription = Strings["home_delete_selected"], tint = colors.danger, modifier = Modifier.size(toolbarButton.IconSize))
                }
            }
        } else {
            val header = DesignTokens.PageHeader
            val search = DesignTokens.SearchBar
            // ── Header: Title + Subtitle + Toolbar (right-aligned) ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left: Title + Subtitle
                Column {
                    Text(
                        text = Strings["home_subscriptions"],
                        fontSize = header.TitleSize,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(header.Gap))
                    Text(Strings["home_subscriptions_desc"], fontSize = header.SubtitleSize, color = colors.textMuted)
                }

                // Right: Search bar
                Surface(
                    modifier = Modifier
                        .width(search.Width)
                        .height(search.Height)
                        .border(DesignTokens.Border.Width, DesignTokens.Border.SecondaryColor, RoundedCornerShape(search.Radius)),
                    shape = RoundedCornerShape(search.Radius),
                    color = colors.surface
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = search.PaddingHorizontal),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(search.IconSize)
                        )
                        Spacer(modifier = Modifier.width(search.Gap))
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isEmpty()) {
                                Text(Strings["home_search_placeholder"], color = colors.textDisabled, fontSize = search.TextSize)
                            }
                            androidx.compose.foundation.text.BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = TextStyle(color = colors.textPrimary, fontSize = search.TextSize),
                                cursorBrush = SolidColor(colors.accent)
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = Strings["discover_search_clear"],
                                tint = colors.textMuted,
                                modifier = Modifier
                                    .size(search.ClearIconSize)
                                    .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                                    .clickable { searchQuery = "" }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val toolbarButton = DesignTokens.ToolbarButton
        // ── List toolbar: count + actions ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = Strings.get("home_subscription_count", podcasts.size),
                color = colors.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(toolbarButton.Gap)) {
                if (!isEditing) {
                    val currentSortLabel = when (sortOption) {
                        "name_asc" -> Strings["home_sort_name_asc"]
                        "name_desc" -> Strings["home_sort_name_desc"]
                        "recent_update" -> Strings["home_sort_recent_update"]
                        "recent_listen" -> Strings["home_sort_recent_listen"]
                        else -> Strings["home_sort"]
                    }

                    // Manage button
                    val manageInteractionSource = remember { MutableInteractionSource() }
                    val isManageHovered by manageInteractionSource.collectIsHoveredAsState()
                    val isManagePressed by manageInteractionSource.collectIsPressedAsState()
                    val manageShape = RoundedCornerShape(toolbarButton.PillRadius)
                    val manageTextColor = if (isManageHovered || isManagePressed) toolbarButton.PillHoverTextColor else toolbarButton.PillTextColor
                    val manageIconColor = if (isManageHovered || isManagePressed) toolbarButton.PillHoverIconColor else toolbarButton.PillIconColor
                    Box(
                        modifier = Modifier
                            .height(toolbarButton.PillHeight)
                            .widthIn(min = toolbarButton.ManageMinWidth)
                            .shadow(
                                if (isManageHovered) toolbarButton.PillHoverShadowElevation else 0.dp,
                                manageShape,
                                ambientColor = toolbarButton.PillHoverShadowColor,
                                spotColor = toolbarButton.PillHoverShadowColor
                            )
                            .clip(manageShape)
                            .background(
                                when {
                                    isManagePressed -> toolbarButton.PillPressedBackgroundColor
                                    isManageHovered -> toolbarButton.PillHoverBackgroundColor
                                    else -> toolbarButton.PillDefaultBackgroundColor
                                }
                            )
                            .border(
                                toolbarButton.BorderWidth,
                                if (isManageHovered || isManagePressed) toolbarButton.PillHoverBorderColor else toolbarButton.PillDefaultBorderColor,
                                manageShape
                            )
                            .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                            .clickable(interactionSource = manageInteractionSource, indication = null) { isEditing = true }
                            .padding(horizontal = toolbarButton.PillPaddingHorizontal),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(toolbarButton.PillIconTextGap)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = Strings["home_manage"],
                                tint = manageIconColor,
                                modifier = Modifier.size(toolbarButton.PillIconSize)
                            )
                            Text(
                                text = Strings["home_manage"],
                                color = manageTextColor,
                                fontSize = toolbarButton.PillTextSize,
                                lineHeight = toolbarButton.PillLineHeight,
                                fontWeight = toolbarButton.PillTextWeight
                            )
                        }
                    }

                    Box {
                        // Sort button
                        val sortInteractionSource = remember { MutableInteractionSource() }
                        val isSortHovered by sortInteractionSource.collectIsHoveredAsState()
                        val isSortPressed by sortInteractionSource.collectIsPressedAsState()
                        val sortShape = RoundedCornerShape(toolbarButton.PillRadius)
                        val sortActive = showSortMenu
                        val sortTextColor = when {
                            sortActive -> toolbarButton.PillSelectedTextColor
                            isSortHovered || isSortPressed -> toolbarButton.PillHoverTextColor
                            else -> toolbarButton.PillTextColor
                        }
                        val sortIconColor = when {
                            sortActive -> toolbarButton.PillSelectedIconColor
                            isSortHovered || isSortPressed -> toolbarButton.PillHoverIconColor
                            else -> toolbarButton.PillIconColor
                        }
                        Box(
                            modifier = Modifier
                                .height(toolbarButton.PillHeight)
                                .widthIn(min = toolbarButton.SortMinWidth)
                                .shadow(
                                    if (isSortHovered) toolbarButton.PillHoverShadowElevation else 0.dp,
                                    sortShape,
                                    ambientColor = toolbarButton.PillHoverShadowColor,
                                    spotColor = toolbarButton.PillHoverShadowColor
                                )
                                .clip(sortShape)
                                .background(
                                    when {
                                        sortActive -> toolbarButton.PillSelectedBackgroundColor
                                        isSortPressed -> toolbarButton.PillPressedBackgroundColor
                                        isSortHovered -> toolbarButton.PillHoverBackgroundColor
                                        else -> toolbarButton.PillSortBackgroundColor
                                    }
                                )
                                .border(
                                    toolbarButton.BorderWidth,
                                    when {
                                        sortActive -> toolbarButton.PillSelectedBorderColor
                                        isSortHovered || isSortPressed -> toolbarButton.PillHoverBorderColor
                                        else -> toolbarButton.PillSortBorderColor
                                    },
                                    sortShape
                                )
                                .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                                .clickable(interactionSource = sortInteractionSource, indication = null) { showSortMenu = true }
                                .padding(horizontal = toolbarButton.PillPaddingHorizontal),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(toolbarButton.PillIconTextGap)
                            ) {
                                Icon(
                                    Icons.Default.UnfoldMore,
                                    contentDescription = null,
                                    tint = sortIconColor,
                                    modifier = Modifier.size(toolbarButton.PillIconSize)
                                )
                                Text(
                                    text = currentSortLabel,
                                    color = sortTextColor,
                                    fontSize = toolbarButton.PillTextSize,
                                    lineHeight = toolbarButton.PillLineHeight,
                                    fontWeight = if (sortActive) toolbarButton.PillActiveTextWeight else toolbarButton.PillTextWeight
                                )
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = Strings["home_sort"],
                                    tint = sortIconColor,
                                    modifier = Modifier.size(toolbarButton.PillTrailingIconSize)
                                )
                            }
                        }

                        PodaraDropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            items = listOf(
                                PodaraDropdownMenuItem(
                                    label = Strings["home_sort_name_asc"],
                                    icon = Icons.Default.SortByAlpha,
                                    isSelected = sortOption == "name_asc",
                                    onClick = { sortOption = "name_asc"; showSortMenu = false }
                                ),
                                PodaraDropdownMenuItem(
                                    label = Strings["home_sort_name_desc"],
                                    icon = Icons.Default.SortByAlpha,
                                    isSelected = sortOption == "name_desc",
                                    onClick = { sortOption = "name_desc"; showSortMenu = false }
                                ),
                                PodaraDropdownMenuItem(
                                    label = Strings["home_sort_recent_update"],
                                    icon = Icons.Default.Update,
                                    isSelected = sortOption == "recent_update",
                                    onClick = { sortOption = "recent_update"; showSortMenu = false }
                                ),
                                PodaraDropdownMenuItem(
                                    label = Strings["home_sort_recent_listen"],
                                    icon = Icons.Default.History,
                                    isSelected = sortOption == "recent_listen",
                                    onClick = { sortOption = "recent_listen"; showSortMenu = false }
                                )
                            )
                        )
                    }
                }
            }  // closes inner Row
        }  // closes outer Row

        Spacer(modifier = Modifier.height(DesignTokens.Spacing.sm))

        HorizontalDivider(color = colors.divider)

        Spacer(modifier = Modifier.height(DesignTokens.Spacing.sm))

        // ── Subscriptions list ──
        if (sortedPodcasts.isEmpty()) {
            val glass = DesignTokens.Glass
            Box(
                modifier = Modifier.fillMaxSize(),
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
                            .shadow(glass.CompactShadowElevation, RoundedCornerShape(DesignTokens.EmptyState.PanelRadius), ambientColor = glass.CompactShadowColor, spotColor = glass.CompactShadowColor)
                            .clip(RoundedCornerShape(DesignTokens.EmptyState.PanelRadius))
                            .background(glass.CompactGradient)
                            .border(glass.CompactBorderWidth, glass.CompactBorderColor, RoundedCornerShape(DesignTokens.EmptyState.PanelRadius))
                            .padding(DesignTokens.EmptyState.PanelPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = Strings["home_search_empty"],
                                color = colors.textPrimary,
                                fontSize = DesignTokens.EmptyState.TitleSize,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(DesignTokens.Spacing.xs))
                            Text(
                                text = Strings["home_search_empty_hint"],
                                color = colors.textSecondary,
                                fontSize = DesignTokens.EmptyState.SubtitleSize
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.FavoriteEpisodeList.CardGap),
                contentPadding = PaddingValues(
                    top = DesignTokens.FavoriteEpisodeList.ListPaddingTop,
                    bottom = DesignTokens.FavoriteEpisodeList.ListPaddingBottom
                )
            ) {
                items(sortedPodcasts) { podcast ->
                    val sub = subscriptionMap[podcast.origin]
                    val newCount = sub?.newEpisodes ?: 0
                    val epCount = episodeCountMap[podcast.origin] ?: 0

                    SubscriptionCard(
                        podcast = podcast,
                        newCount = newCount,
                        episodeCount = epCount,
                        isEditing = isEditing,
                        isSelected = podcast.origin in selectedPodcasts,
                        onToggleSelect = { checked ->
                            selectedPodcasts = if (checked) selectedPodcasts + podcast.origin
                            else selectedPodcasts - podcast.origin
                        },
                        onClick = { if (!isEditing) onPodcastClick(podcast) },
                        onMore = {
                            podcastToUnsubscribe = podcast
                            showUnsubscribeDialog = true
                        }
                    )
                }
            }
        }
    }

    // ── Dialogs ──
    if (showUnsubscribeDialog && podcastToUnsubscribe != null) {
        AlertDialog(
            onDismissRequest = { showUnsubscribeDialog = false; podcastToUnsubscribe = null },
            title = { Text(Strings["unsubscribe"]) },
            text = { Text(Strings.get("unsubscribe_confirm", podcastToUnsubscribe!!.title)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        subscriptionManager.unsubscribe(podcastToUnsubscribe!!.origin)
                        onPodcastsChanged(database.podcasts.getAllSync())
                        showUnsubscribeDialog = false; podcastToUnsubscribe = null
                    }
                }) { Text(Strings["unsubscribe"]) }
            },
            dismissButton = {
                TextButton(onClick = { showUnsubscribeDialog = false; podcastToUnsubscribe = null }) { Text(Strings["dialog_cancel"]) }
            }
        )
    }

    if (showBatchUnsubscribeDialog) {
        AlertDialog(
            onDismissRequest = { showBatchUnsubscribeDialog = false },
            containerColor = colors.surface,
            title = { Text(Strings["batch_unsubscribe"], color = colors.textPrimary) },
            text = { Text(Strings.get("batch_unsubscribe_confirm", selectedPodcasts.size), color = colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        selectedPodcasts.forEach { subscriptionManager.unsubscribe(it) }
                        onPodcastsChanged(database.podcasts.getAllSync())
                        selectedPodcasts = emptySet(); isEditing = false; showBatchUnsubscribeDialog = false
                    }
                }) { Text(Strings["unsubscribe"], color = colors.danger) }
            },
            dismissButton = { TextButton(onClick = { showBatchUnsubscribeDialog = false }) { Text(Strings["dialog_cancel"], color = colors.textSecondary) } }
        )
    }
}

@Composable
private fun SubscriptionCard(
    podcast: Podcast,
    newCount: Int,
    episodeCount: Int,
    isEditing: Boolean,
    isSelected: Boolean,
    onToggleSelect: (Boolean) -> Unit,
    onClick: () -> Unit,
    onMore: () -> Unit
) {
    val colors = PodaraTheme.colors
    val card = DesignTokens.FavoriteEpisodeList
    val row = DesignTokens.SubscriptionRow
    val badge = DesignTokens.Badge
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(card.CardRadius)
    val animatedBg by animateColorAsState(
        when {
            isSelected -> card.PlayingBackgroundColor
            isPressed && !isEditing -> card.PressedBackgroundColor
            isHovered && !isEditing -> card.HoverBackgroundColor
            else -> card.BackgroundColor
        },
        tween(DesignTokens.Animation.HoverMs)
    )
    val borderColor by animateColorAsState(
        when {
            isSelected -> card.PlayingBorderColor
            isHovered && !isEditing -> card.HoverBorderColor
            else -> card.BorderColor
        },
        tween(DesignTokens.Animation.HoverMs)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(card.CardHeight)
            .clip(shape)
            .background(animatedBg)
            .border(card.BorderWidth, borderColor, shape)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = card.CardPaddingHorizontal, vertical = card.CardPaddingVertical),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isEditing) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onToggleSelect,
                modifier = Modifier.size(row.CheckboxSize)
            )
            Spacer(modifier = Modifier.width(card.CoverContentGap))
        }

        // Cover
        Box(
            modifier = Modifier
                .size(card.CoverSize)
                .shadow(
                    card.CoverShadowElevation,
                    RoundedCornerShape(card.CoverRadius),
                    ambientColor = card.CoverShadowColor,
                    spotColor = card.CoverShadowColor
                )
                .clip(RoundedCornerShape(card.CoverRadius))
                .background(colors.elevated)
        ) {
            AsyncImage(model = podcast.imageUrl, contentDescription = podcast.title,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }

        Spacer(modifier = Modifier.width(card.CoverContentGap))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = podcast.fetchTitle(),
                color = card.TitleColor,
                fontSize = card.TitleSize,
                lineHeight = card.TitleLineHeight,
                fontWeight = card.TitleWeight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(card.PodcastNameMarginTop))
            Text(
                text = podcast.author,
                color = card.PodcastNameColor,
                fontSize = card.PodcastNameSize,
                lineHeight = card.PodcastNameLineHeight,
                fontWeight = card.PodcastNameWeight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (podcast.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(card.MetadataMarginTop))
                Text(
                    text = stripHtml(podcast.description),
                    color = card.MetadataColor,
                    fontSize = card.MetadataSize,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = row.DescriptionMaxWidth)
                )
            }
        }

        Spacer(modifier = Modifier.width(card.ContentActionsGap))

        // Meta: episode count + New badge (horizontal row)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm),
            modifier = Modifier.padding(end = row.MetaEndPadding)
        ) {
            if (episodeCount > 0) {
                Text(text = Strings.get("home_episode_count", episodeCount), color = card.MetadataColor, fontSize = card.MetadataSize)
            }
            if (newCount > 0) {
                Box(
                    modifier = Modifier
                        .wrapContentSize()
                        .clip(RoundedCornerShape(badge.Radius))
                        .background(badge.AccentBackgroundColor)
                        .padding(horizontal = badge.PaddingHorizontal, vertical = badge.PaddingVertical),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (newCount == 1) Strings["home_new_badge"] else Strings.get("home_new_count", newCount),
                        color = badge.AccentTextColor, fontSize = badge.TextSize, fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(card.ActionsGap))

        // More button
        if (!isEditing) {
            EpisodeActionIconButton(
                icon = Icons.Default.Delete,
                contentDescription = Strings["unsubscribe"],
                size = card.ActionButtonSize,
                radius = card.ActionButtonRadius,
                iconSize = card.ActionIconSize,
                hoverBackgroundColor = card.ActionButtonHoverBackgroundColor,
                defaultIconColor = card.ActionIconColor,
                hoverIconColor = card.ActionIconHoverColor,
                onClick = onMore
            )
        }
    }
}

@Composable
private fun PodcastDetailScreen(
    podcast: Podcast,
    database: AppDatabase,
    subscriptionManager: SubscriptionManager,
    fetchPodcastClient: FetchPodcastClient,
    playerState: MediaPlayerState,
    downloadManager: DownloadManager,
    downloadingEpisodes: Set<String>,
    downloadProgress: Map<String, Pair<Long, Long>>,
    downloadVersion: Int,
    completedDownloads: Set<String>,
    favoriteVersion: Int,
    onStartDownload: (PodcastEpisode, String) -> Unit,
    onPauseDownload: (String) -> Unit = {},
    onResumeDownload: (String) -> Unit = {},
    onFavoriteChanged: () -> Unit = {},
    onBack: () -> Unit,
    onUnsubscribed: suspend () -> Unit = { },
    onSubscribed: suspend () -> Unit = { }
) {
    val colors = PodaraTheme.colors
    var episodes by remember { mutableStateOf(emptyList<PodcastEpisode>()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSubscribed by remember { mutableStateOf(false) }
    var showUnsubscribeDialog by remember { mutableStateOf(false) }
    var favoriteIds by remember { mutableStateOf(setOf<String>()) }
    val scope = rememberCoroutineScope()

    // Build context queue items for playWithContext
    val episodeContextItems: List<QueueItem> = remember(episodes) {
        episodes.map { ep ->
            QueueItem(url = ep.audioUrl, title = ep.title, subtitle = ep.podcastTitle, artworkUrl = ep.imageUrl, episodeId = ep.id)
        }
    }

    // ── Active download progress from DB (for robustness across page navigation) ──
    var activeTaskProgress by remember { mutableStateOf(mapOf<String, Pair<Long, Long>>()) }
    LaunchedEffect(downloadVersion) {
        try {
            val activeTasks = database.downloadTasks.getAllActive()
            // Include all non-completed tasks (DOWNLOADING, PAUSED, FAILED)
            // so the detail page never shows a download button for a task that exists
            activeTaskProgress = activeTasks.associate { t ->
                t.episodeId to (t.downloadedBytes to t.totalBytes)
            }
        } catch (_: Exception) { }
    }

    LaunchedEffect(favoriteVersion) {
        favoriteIds = database.favorites.getAllEpisodeIds()
    }

    // Combined check: memory state OR DB task.
    // Keyed on downloadVersion so DB changes force recalculation.
    val allDownloading = remember(downloadingEpisodes, activeTaskProgress, downloadVersion) {
        downloadingEpisodes + activeTaskProgress.keys
    }

    LaunchedEffect(podcast.origin) {
        val subscription = database.subscriptions.getByOriginSync(podcast.origin)
        isSubscribed = subscription != null

        if (subscription != null) {
            // Subscribed: load from DB, then refresh via RSS
            episodes = database.episodes.getAllByOrigin(podcast.origin)
            try {
                when (val result = subscriptionManager.updatePodcast(podcast.origin, podcast.imageSeedColor)) {
                    is UpdatePodcastResult.Updated -> {
                        episodes = database.episodes.getAllByOrigin(podcast.origin)
                    }
                    else -> { }
                }
            } catch (_: Exception) { }
            isLoading = false
        } else {
            // Not subscribed: preview mode — fetch RSS directly, no DB writes
            try {
                val result = fetchPodcastClient.fetchNoCache(podcast.origin)
                if (result is FetchPodcastClientResult.Success) {
                    val (_, parsedEpisodes) = RssConverter.parseFetchResult(result, podcast.origin)
                    episodes = parsedEpisodes
                }
            } catch (_: Exception) { }
            isLoading = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(colors.background)
    ) {
        // ── Top navigation bar ──
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val backInteractionSource = remember { MutableInteractionSource() }
            val isBackHovered by backInteractionSource.collectIsHoveredAsState()
            val backAnimatedBg by animateColorAsState(if (isBackHovered) colors.elevated else Color.Transparent, tween(150))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(backAnimatedBg)
                    .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                    .clickable(interactionSource = backInteractionSource, indication = null) { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = Strings["nav_back"], tint = colors.textPrimary)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = podcast.fetchTitle(),
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        // ── Content ──
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colors.accent)
                }
            }
            episodes.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = Strings["podcast_no_episodes"],
                            color = PodaraTheme.colors.textMuted
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = {
                            scope.launch {
                                isLoading = true
                                try {
                                    val result = subscriptionManager.updatePodcast(podcast.origin, podcast.imageSeedColor)
                                    if (result is UpdatePodcastResult.Updated) {
                                        episodes = database.episodes.getAllByOrigin(podcast.origin)
                                    }
                                } catch (_: Exception) { }
                                isLoading = false
                            }
                        }) {
                            Text(Strings["podcast_retry"])
                        }
                    }
                }
            }
            else -> {
                // ── Header: Cover + Title + Author + Description + Action Buttons ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 44.dp, top = 16.dp, end = 32.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cover — left side, square with rounded corners
                    AsyncImage(
                        model = podcast.imageUrl,
                        contentDescription = podcast.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )

                    Spacer(modifier = Modifier.width(20.dp))

                    // Content — right side
                    Column(modifier = Modifier.weight(1f)) {
                        // Title
                        Text(
                            text = podcast.fetchTitle(),
                            color = colors.textPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Author
                        if (podcast.author.isNotEmpty()) {
                            Text(
                                text = podcast.author,
                                color = colors.textSecondary,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // Description (max 2 lines)
                        if (podcast.description.isNotEmpty()) {
                            Text(
                                text = stripHtml(podcast.description),
                                color = colors.textMuted,
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // ── Three action buttons ──
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // 1. Play Latest
                            val btn = DesignTokens.Button
                            Box(
                                modifier = Modifier
                                    .height(btn.Height)
                                    .shadow(btn.ShadowElevation, RoundedCornerShape(btn.Radius), ambientColor = btn.ShadowColor, spotColor = btn.ShadowColor)
                                    .clip(RoundedCornerShape(btn.Radius))
                                    .border(DesignTokens.Border.Width, btn.BorderColor, RoundedCornerShape(btn.Radius))
                                    .background(btn.Gradient)
                                    .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                                    .clickable {
                                        scope.launch {
                                            val latest = episodes.maxByOrNull { it.pubDate }
                                            if (latest != null) {
                                                playAndRecordHistory(database, playerState, latest, podcast.imageUrl)
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(modifier = Modifier.matchParentSize().background(btn.InnerHighlight))
                                Box(modifier = Modifier.matchParentSize().background(btn.SpecularSheen))
                                Row(
                                    modifier = Modifier.padding(horizontal = btn.PaddingHorizontal),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = btn.IconColor, modifier = Modifier.size(btn.IconSize))
                                    Spacer(Modifier.width(DesignTokens.Spacing.sm))
                                    Text(
                                        text = Strings["discover_latest_episode"],
                                        color = btn.TextColor,
                                        fontSize = btn.TextSize,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // 2. Subscribe / Unsubscribe
                            val subInteractionSource = remember { MutableInteractionSource() }
                            val isSubHovered by subInteractionSource.collectIsHoveredAsState()
                            val subAnimatedBg by animateColorAsState(
                                when {
                                    isSubscribed -> colors.accent.copy(alpha = 0.15f)
                                    isSubHovered -> colors.elevated
                                    else -> colors.surface
                                },
                                tween(150)
                            )
                            Box(
                                modifier = Modifier
                                    .size(DesignTokens.IconButton.Size)
                                    .clip(CircleShape)
                                    .border(DesignTokens.Border.Width, DesignTokens.Border.SecondaryColor, CircleShape)
                                    .background(subAnimatedBg)
                                    .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                                    .clickable(interactionSource = subInteractionSource, indication = null) {
                                        if (isSubscribed) {
                                            showUnsubscribeDialog = true
                                        } else {
                                            scope.launch {
                                                database.podcasts.insert(podcast)
                                                subscriptionManager.subscribe(podcast.origin)
                                                isSubscribed = true
                                                onSubscribed()
                                                try {
                                                    when (val result = subscriptionManager.updatePodcast(podcast.origin, podcast.imageSeedColor)) {
                                                        is UpdatePodcastResult.Updated -> {
                                                            episodes = database.episodes.getAllByOrigin(podcast.origin)
                                                        }
                                                        else -> { }
                                                    }
                                                } catch (_: Exception) { }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isSubscribed) Icons.Default.Check else Icons.Default.Add,
                                    contentDescription = if (isSubscribed) Strings["discover_added"] else Strings["discover_add"],
                                    tint = if (isSubscribed) colors.accent else colors.textSecondary,
                                    modifier = Modifier.size(DesignTokens.IconButton.IconSize)
                                )
                            }

                            // 3. More options — copy RSS URL
                            val moreInteractionSource = remember { MutableInteractionSource() }
                            val isMoreHovered by moreInteractionSource.collectIsHoveredAsState()
                            val moreAnimatedBg by animateColorAsState(if (isMoreHovered) colors.elevated else colors.surface, tween(150))
                            var showPopup by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .size(DesignTokens.IconButton.Size)
                                    .clip(CircleShape)
                                    .border(DesignTokens.Border.Width, DesignTokens.Border.SecondaryColor, CircleShape)
                                    .background(moreAnimatedBg)
                                    .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                                    .clickable(interactionSource = moreInteractionSource, indication = null) { showPopup = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.MoreHoriz, contentDescription = Strings["discover_more"], tint = colors.textSecondary, modifier = Modifier.size(DesignTokens.IconButton.IconSize))

                                PodaraDropdownMenu(
                                    expanded = showPopup,
                                    onDismissRequest = { showPopup = false },
                                    items = listOf(
                                        PodaraDropdownMenuItem(
                                            label = Strings["dialog_copy_to_clipboard"],
                                            onClick = {
                                                showPopup = false
                                                val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
                                                val selection = java.awt.datatransfer.StringSelection(podcast.origin)
                                                clipboard.setContents(selection, null)
                                            }
                                        )
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = colors.divider, modifier = Modifier.padding(horizontal = 32.dp))
                Spacer(modifier = Modifier.height(4.dp))

                // ── Episode list ──
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(
                        top = DesignTokens.FavoriteEpisodeList.ListPaddingTop,
                        bottom = DesignTokens.FavoriteEpisodeList.ListPaddingBottom
                    ),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.FavoriteEpisodeList.CardGap)
                ) {
                    items(episodes) { episode ->
                        val isDownloading = episode.id in allDownloading
                        val progress = downloadProgress[episode.id] ?: activeTaskProgress[episode.id]
                        val isDbTask = episode.id in activeTaskProgress && episode.id !in downloadProgress

                        EpisodeListItem(
                            episode = episode,
                            podcast = podcast,
                            isPlaying = playerState.currentEpisodeId == episode.id || playerState.currentUrl == episode.audioUrl,
                            secondaryText = formatEpisodeMetadata(formatDate(episode.pubDate), episode.duration),
                            tertiaryText = stripHtml(episode.description),
                            secondaryTextRole = EpisodeListItemSecondaryTextRole.Metadata,
                            modifier = Modifier.padding(horizontal = 32.dp),
                            onPlay = {
                                scope.launch {
                                    val downloadRecord = database.downloads.getByEpisodeId(episode.id)
                                    val url = if (downloadRecord != null && File(downloadRecord.filePath).exists()) {
                                        downloadRecord.filePath
                                    } else {
                                        episode.audioUrl
                                    }
                                    val epWithUrl = episode.copy(audioUrl = url)
                                    playerState.playWithContext(
                                        context = episodeContextItems.map { item ->
                                            if (item.url == epWithUrl.audioUrl) item.copy(url = url) else item
                                        },
                                        targetUrl = url,
                                        title = epWithUrl.title,
                                        subtitle = epWithUrl.podcastTitle,
                                        artworkUrl = epWithUrl.imageUrl,
                                        podcastArtworkUrl = podcast.imageUrl,
                                        durationMs = epWithUrl.duration * 1000L,
                                        episodeId = epWithUrl.id
                                    )
                                    database.episodes.insert(epWithUrl)
                                    database.history.insert(epWithUrl.origin, epWithUrl.id)
                                }
                            },
                        ) {
                            FavoriteEpisodeButton(isFavorite = episode.id in favoriteIds) {
                                scope.launch {
                                    database.episodes.insert(episode)
                                    val isFavorite = database.favorites.toggle(episode)
                                    favoriteIds = if (isFavorite) favoriteIds + episode.id else favoriteIds - episode.id
                                    onFavoriteChanged()
                                }
                            }

                            AddToQueueButton {
                                scope.launch {
                                    val downloadRecord = database.downloads.getByEpisodeId(episode.id)
                                    val url = if (downloadRecord != null && File(downloadRecord.filePath).exists()) {
                                        downloadRecord.filePath
                                    } else {
                                        episode.audioUrl
                                    }
                                    playerState.addToQueue(
                                        url = url,
                                        title = episode.title,
                                        artworkUrl = episode.imageUrl,
                                        podcastArtworkUrl = podcast.imageUrl,
                                        episodeId = episode.id,
                                        isDownloaded = episode.id in completedDownloads
                                    )
                                }
                            }

                            Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                                if (episode.id in completedDownloads) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = Strings["episode_downloaded"],
                                        tint = colors.success
                                    )
                                } else if (isDownloading) {
                                    val fraction = if (progress != null && progress.second > 0) {
                                        progress.first.toFloat() / progress.second
                                    } else 0f
                                    val ringInteractionSource = remember { MutableInteractionSource() }
                                    val isRingHovered by ringInteractionSource.collectIsHoveredAsState()
                                    val ringAnimatedBg by animateColorAsState(if (isRingHovered) colors.elevated else Color.Transparent, tween(150))
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(ringAnimatedBg)
                                            .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                                            .clickable(interactionSource = ringInteractionSource, indication = null) {
                                                if (isDbTask) {
                                                    onResumeDownload(episode.id)
                                                } else {
                                                    onPauseDownload(episode.id)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            progress = { fraction },
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = colors.accent
                                        )
                                    }
                                } else {
                                    val dInteractionSource = remember { MutableInteractionSource() }
                                    val isDHovered by dInteractionSource.collectIsHoveredAsState()
                                    val dAnimatedBg by animateColorAsState(if (isDHovered) colors.elevated else Color.Transparent, tween(150))
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(dAnimatedBg)
                                            .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                                            .clickable(interactionSource = dInteractionSource, indication = null) {
                                                onStartDownload(episode, podcast.title)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = Strings["episode_download"], tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showUnsubscribeDialog) {
        AlertDialog(
            onDismissRequest = { showUnsubscribeDialog = false },
            title = { Text(Strings["unsubscribe"]) },
            text = { Text(Strings.get("unsubscribe_confirm", podcast.title)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        subscriptionManager.unsubscribe(podcast.origin)
                        onUnsubscribed()
                        onBack()
                        showUnsubscribeDialog = false
                    }
                }) {
                    Text(Strings["unsubscribe"])
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnsubscribeDialog = false }) {
                    Text(Strings["dialog_cancel"])
                }
            }
        )
    }
}

@Composable
private fun AddPodcastDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    error: String? = null
) {
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings["home_add_podcast"]) },
        text = {
            Column {
                Text(Strings["add_podcast_hint"])
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(Strings["add_podcast_rss_label"]) },
                    placeholder = { Text(Strings["add_podcast_rss_placeholder"]) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (url.isNotBlank()) {
                        onConfirm(url)
                    }
                }
            ) {
                Text(Strings["dialog_ok"])
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings["dialog_cancel"])
            }
        }
    )
}

// ── DownloadsScreen is in DownloadsScreen.kt ──

internal fun stripHtml(html: String): String {
    return html.replace(Regex("<[^>]*>"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

internal fun formatDate(timestamp: Long): String {
    if (timestamp <= 0) return ""
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
