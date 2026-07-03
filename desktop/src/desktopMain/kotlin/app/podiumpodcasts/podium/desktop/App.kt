package app.podiumpodcasts.podium.desktop

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
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowScope
import java.awt.Cursor
import coil3.compose.AsyncImage
import app.podiumpodcasts.podium.api.apple.ApplePodcastClient
import app.podiumpodcasts.podium.api.model.PodcastPreviewModel
import app.podiumpodcasts.podium.api.rss.FetchPodcastClient
import app.podiumpodcasts.podium.api.rss.FetchPodcastClientResult
import app.podiumpodcasts.podium.data.AppDatabase
import app.podiumpodcasts.podium.data.model.Podcast
import app.podiumpodcasts.podium.data.model.PodcastEpisode
import app.podiumpodcasts.podium.desktop.player.FullPlayer
import app.podiumpodcasts.podium.desktop.player.MediaPlayerState
import app.podiumpodcasts.podium.desktop.player.MiniPlayer
import app.podiumpodcasts.podium.desktop.player.QueueDrawer
import app.podiumpodcasts.podium.manager.AddPodcastResult
import app.podiumpodcasts.podium.manager.DownloadManager
import app.podiumpodcasts.podium.manager.PodcastManager
import app.podiumpodcasts.podium.manager.SubscriptionManager
import app.podiumpodcasts.podium.manager.UpdatePodcastResult
import app.podiumpodcasts.podium.ui.theme.DesignTokens
import app.podiumpodcasts.podium.ui.theme.PodiumTheme
import app.podiumpodcasts.podium.utils.Logger
import app.podiumpodcasts.podium.utils.RssConverter
import app.podiumpodcasts.podium.utils.Settings
import app.podiumpodcasts.podium.utils.Strings
import kotlinx.coroutines.launch
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
    onHistory: () -> Unit,
    onSettings: () -> Unit
) {
    data class NavItem(val icon: androidx.compose.ui.graphics.vector.ImageVector, val labelKey: String, val screen: String)

    val navItems = listOf(
        NavItem(Icons.Default.Explore, "nav_discover", "discover"),
        NavItem(Icons.Default.LibraryMusic, "nav_shows", "home"),
        NavItem(Icons.Default.QueueMusic, "nav_episodes", "history"),
        NavItem(Icons.Default.Folder, "nav_downloads", "home")
    )

    val colors = PodiumTheme.colors
    val sidebar = DesignTokens.Sidebar

    Surface(
        modifier = Modifier.width(sidebar.Width).fillMaxHeight(),
        color = colors.surface
    ) {
        Column(
            modifier = Modifier.fillMaxHeight().padding(top = sidebar.PaddingVertical, bottom = 0.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = sidebar.PaddingHorizontal, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier.size(sidebar.LogoSize).background(colors.accent, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = colors.surface,
                        modifier = Modifier.size(sidebar.LogoIconSize)
                    )
                }
                Text(
                    text = "Podify",
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
                val showBg = isActive || isHovered
                val bgColor = if (showBg) SidebarActiveBg else Color.Transparent
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
                            .background(bgColor, RoundedCornerShape(8.dp))
                            .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                            .clickable(interactionSource = interactionSource, indication = null) {
                                when (item.screen) {
                                    "discover" -> onDiscover()
                                    "home" -> onShows()
                                    "history" -> onHistory()
                                    "settings" -> onSettings()
                                }
                            }
                            .padding(horizontal = 8.dp),
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
            val settingsShowBg = settingsActive || settingsIsHovered
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(sidebar.NavItemHeight)
                    .padding(horizontal = sidebar.NavItemPadding)
                    .background(if (settingsShowBg) SidebarActiveBg else Color.Transparent, RoundedCornerShape(8.dp))
                    .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                    .clickable(interactionSource = settingsInteractionSource, indication = null) { onSettings() }
                    .padding(horizontal = 8.dp),
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
        }
    }
}

private const val TAG = "App"

private fun logError(e: Throwable) {
    Logger.e(TAG, "Uncaught error: ${e.message}", e)
    try {
        val logFile = File(System.getProperty("user.home"), ".podium/crash.log")
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
    val bgColor = when {
        isClose && isHovered -> Color(0xFFE81123)
        isHovered -> Color(0x18FFFFFF)
        else -> Color.Transparent
    }
    val iconTint = when {
        isClose && isHovered -> Color.White
        isHovered -> Color.White
        else -> Color(0xFF999999)
    }

    Box(
        modifier = Modifier
            .size(36.dp)
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        icon(iconTint)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WindowScope.App(windowState: androidx.compose.ui.window.WindowState, awtWindow: java.awt.Window) {
    val database = remember {
        try {
            val userHome = System.getProperty("user.home")
            val dbDir = File(userHome, ".podium")
            dbDir.mkdirs()
            Logger.i(TAG, "Initializing database at ${dbDir.absolutePath}")
            AppDatabase.build(File(dbDir, "podium.db"))
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
    val downloadManager = remember(downloadPath) {
        val downloadsDir = File(downloadPath)
        downloadsDir.mkdirs()
        DownloadManager(database, downloadsDir)
    }
    val playerState = remember { MediaPlayerState() }
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
    var addError by remember { mutableStateOf<String?>(null) }
    var downloadProgress by remember { mutableStateOf(mapOf<String, Pair<Long, Long>>()) }
    var downloadingEpisodes by remember { mutableStateOf(setOf<String>()) }
    var downloadVersion by remember { mutableIntStateOf(0) }
    var completedDownloads by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(Unit) {
        Logger.d(TAG, "Loading podcasts from database")
        podcasts = database.podcasts.getAllSync()
        completedDownloads = database.downloads.getAllValidDownloadedIds()
        Logger.d(TAG, "Loaded ${podcasts.size} podcasts, ${completedDownloads.size} downloads")
    }

    val startDownload = { episode: PodcastEpisode, podcastTitle: String ->
        downloadingEpisodes = downloadingEpisodes + episode.id
        completedDownloads = completedDownloads - episode.id
        scope.launch {
            try {
                val result = downloadManager.downloadEpisode(
                    episodeId = episode.id,
                    audioUrl = episode.audioUrl,
                    origin = episode.origin,
                    episodeTitle = episode.title,
                    podcastTitle = podcastTitle
                ) { current, total ->
                    downloadProgress = downloadProgress + (episode.id to Pair(current, total))
                }
                if (result.isSuccess) {
                    completedDownloads = completedDownloads + episode.id
                }
            } finally {
                downloadingEpisodes = downloadingEpisodes - episode.id
                downloadProgress = downloadProgress - episode.id
                downloadVersion++
            }
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

    PodiumTheme(darkTheme = true) {
        val titleBarColors = PodiumTheme.colors
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
                    Icon(
                        Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = titleBarColors.accent,
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
                        onClick = { awtWindow.dispose(); System.exit(0) },
                        icon = { tint -> Icon(Icons.Default.Close, contentDescription = Strings["titlebar_close"], tint = tint, modifier = Modifier.size(14.dp)) },
                        isClose = true
                    )
                }
            }

            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Sidebar(
                    currentScreen = currentScreen,
                    onDiscover = { currentScreen = "discover"; showFullPlayer = false; selectedPodcast = null },
                    onShows = { currentScreen = "home"; showFullPlayer = false; selectedPodcast = null },
                    onHistory = { currentScreen = "history"; showFullPlayer = false; selectedPodcast = null },
                    onSettings = { currentScreen = "settings"; showFullPlayer = false; selectedPodcast = null }
                )

                Box(modifier = Modifier.weight(1f)) {
                    when {
                        showFullPlayer -> FullPlayer(
                            state = playerState,
                            database = database,
                            onClose = { showFullPlayer = false }
                        )
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
                            onStartDownload = startDownload,
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
                            onLanguageChanged = { /* Language change is handled by Settings */ }
                        )
                        currentScreen == "history" -> HistoryScreen(
                            database = database,
                            playerState = playerState,
                            onBack = { currentScreen = "home" }
                        )
                    }
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

    if (showQueueFromMini) {
        QueueDrawer(
            state = playerState,
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
}

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
    val colors = PodiumTheme.colors
    var isEditing by remember { mutableStateOf(false) }
    var selectedPodcasts by remember { mutableStateOf(setOf<String>()) }
    var showBatchUnsubscribeDialog by remember { mutableStateOf(false) }
    var showUnsubscribeDialog by remember { mutableStateOf(false) }
    var podcastToUnsubscribe by remember { mutableStateOf<Podcast?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var subscriptionMap by remember { mutableStateOf(mapOf<String, app.podiumpodcasts.podium.data.model.PodcastSubscription>()) }
    var episodeCountMap by remember { mutableStateOf(mapOf<String, Int>()) }

    LaunchedEffect(Unit) {
        val subs = database.subscriptions.getAllSync()
        subscriptionMap = subs.associateBy { it.origin }
        // Episode counts for display
        val counts = mutableMapOf<String, Int>()
        podcasts.forEach { p ->
            counts[p.origin] = database.episodes.getEpisodeIds(p.origin).size
        }
        episodeCountMap = counts
    }

    val filteredPodcasts = remember(podcasts, searchQuery) {
        if (searchQuery.isBlank()) podcasts
        else podcasts.filter { it.title.contains(searchQuery, ignoreCase = true) || it.author.contains(searchQuery, ignoreCase = true) }
    }

    // ── Empty state ──
    if (podcasts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.RssFeed, null, Modifier.size(64.dp), tint = colors.textMuted)
                Spacer(modifier = Modifier.height(16.dp))
                Text(Strings["home_empty"], style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(Strings["home_empty_hint"], style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                Spacer(modifier = Modifier.height(16.dp))
                FilledTonalButton(onClick = onAddPodcast) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Strings["home_add_podcast"])
                }
            }
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(top = 24.dp, start = 28.dp, end = 28.dp, bottom = 8.dp)
    ) {
        // ── Header (hidden in edit mode, replaced by selection bar) ──
        if (isEditing) {
            // Selection header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = { isEditing = false; selectedPodcasts = emptySet() }) {
                    Icon(Icons.Default.Close, contentDescription = Strings["home_cancel"], tint = colors.textPrimary)
                }
                Text(Strings.get("home_selected_count", selectedPodcasts.size), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = {
                    selectedPodcasts = if (selectedPodcasts.size == filteredPodcasts.size) emptySet()
                    else filteredPodcasts.map { it.origin }.toSet()
                }) {
                    Icon(Icons.Default.SelectAll, contentDescription = Strings["home_select_all"], tint = colors.textSecondary)
                }
                IconButton(onClick = { showBatchUnsubscribeDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = Strings["home_delete_selected"], tint = colors.danger)
                }
            }
        } else {
            // Title row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(Strings["home_subscriptions"], fontSize = 32.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Text(
                    text = "(${podcasts.size})",
                    fontSize = 16.sp,
                    color = colors.textMuted,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(Strings["home_subscriptions_desc"], fontSize = 14.sp, color = colors.textSecondary)

            Spacer(modifier = Modifier.height(16.dp))

            // Toolbar: Search + Manage
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Search bar
                val search = DesignTokens.SearchBar
                Box(
                    modifier = Modifier
                        .width(search.Width)
                        .height(search.Height)
                        .clip(RoundedCornerShape(search.Radius))
                        .background(colors.elevated)
                        .padding(horizontal = search.PaddingHorizontal),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(search.Gap)) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(search.IconSize))
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isEmpty()) {
                                Text(Strings["home_search_placeholder"], color = colors.textDisabled, fontSize = search.TextSize)
                            }
                            androidx.compose.foundation.text.BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = TextStyle(color = colors.textPrimary, fontSize = search.TextSize)
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            Icon(Icons.Default.Clear, contentDescription = Strings["discover_search_clear"], tint = colors.textMuted,
                                modifier = Modifier.size(search.ClearIconSize).pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR))).clickable { searchQuery = "" })
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                // Manage button
                Box(
                    modifier = Modifier
                        .width(96.dp).height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                        .background(colors.surface)
                        .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                        .clickable { isEditing = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(Strings["home_manage"], fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Subscriptions list ──
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filteredPodcasts) { podcast ->
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
                HorizontalDivider(color = colors.divider, modifier = Modifier.padding(start = 80.dp))
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
            title = { Text(Strings["batch_unsubscribe"]) },
            text = { Text(Strings.get("batch_unsubscribe_confirm", selectedPodcasts.size)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        selectedPodcasts.forEach { subscriptionManager.unsubscribe(it) }
                        onPodcastsChanged(database.podcasts.getAllSync())
                        selectedPodcasts = emptySet(); isEditing = false; showBatchUnsubscribeDialog = false
                    }
                }) { Text(Strings["unsubscribe"]) }
            },
            dismissButton = { TextButton(onClick = { showBatchUnsubscribeDialog = false }) { Text(Strings["dialog_cancel"]) } }
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
    val colors = PodiumTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .background(if (isHovered && !isEditing) colors.elevated else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (isEditing) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onToggleSelect,
                modifier = Modifier.size(24.dp)
            )
        }

        // Cover
        Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(colors.elevated)) {
            AsyncImage(model = podcast.imageUrl, contentDescription = podcast.title,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(text = podcast.fetchTitle(), color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = podcast.author, color = colors.textMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (episodeCount > 0) {
                    Text(text = Strings.get("home_episode_count", episodeCount), color = colors.textDisabled, fontSize = 11.sp)
                }
                if (newCount > 0) {
                    Box(
                        modifier = Modifier
                            .height(20.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.accent.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (newCount == 1) Strings["home_new_badge"] else Strings.get("home_new_count", newCount),
                            color = colors.accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // More button
        if (!isEditing) {
            val moreInteractionSource = remember { MutableInteractionSource() }
            val isMoreHovered by moreInteractionSource.collectIsHoveredAsState()
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isMoreHovered) colors.elevated else Color.Transparent)
                    .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                    .clickable(interactionSource = moreInteractionSource, indication = null) { onMore() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MoreHoriz, contentDescription = Strings["discover_more"], tint = colors.textSecondary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    onStartDownload: (PodcastEpisode, String) -> Unit,
    onBack: () -> Unit,
    onUnsubscribed: suspend () -> Unit = { },
    onSubscribed: suspend () -> Unit = { }
) {
    var episodes by remember { mutableStateOf(emptyList<PodcastEpisode>()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSubscribed by remember { mutableStateOf(false) }
    var showUnsubscribeDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = podcast.fetchTitle(),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = Strings["nav_back"])
                    }
                },
                actions = { },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            episodes.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = Strings["podcast_no_episodes"],
                            color = PodiumTheme.colors.textMuted
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
                val colors = PodiumTheme.colors
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                // ── Header: Cover + Title + Author + Description + Action Buttons ──
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Cover — left side, square with rounded corners (like FeaturedCard)
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
                                    // 1. Play Latest (reuse DesignTokens.Button from FeaturedCard)
                                    val btn = DesignTokens.Button
                                    Box(
                                        modifier = Modifier
                                            .height(btn.Height)
                                            .clip(RoundedCornerShape(btn.Radius))
                                            .shadow(btn.ShadowElevation, RoundedCornerShape(btn.Radius), ambientColor = btn.ShadowColor, spotColor = btn.ShadowColor)
                                            .border(DesignTokens.Border.Width, DesignTokens.Border.SecondaryColor, RoundedCornerShape(btn.Radius))
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

                                    // 2. Subscribe / Unsubscribe (reuse FeaturedCard circular icon button)
                                    val subInteractionSource = remember { MutableInteractionSource() }
                                    val isSubHovered by subInteractionSource.collectIsHoveredAsState()
                                    Box(
                                        modifier = Modifier
                                            .size(DesignTokens.IconButton.Size)
                                            .clip(CircleShape)
                                            .border(DesignTokens.Border.Width, DesignTokens.Border.SecondaryColor, CircleShape)
                                            .background(
                                                when {
                                                    isSubscribed -> colors.accent.copy(alpha = 0.15f)
                                                    isSubHovered -> colors.elevated
                                                    else -> colors.surface
                                                }
                                            )
                                            .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                                            .clickable(interactionSource = subInteractionSource, indication = null) {
                                                if (isSubscribed) {
                                                    showUnsubscribeDialog = true
                                                } else {
                                                    scope.launch {
                                                        // Save podcast record in DB first, then subscribe
                                                        database.podcasts.insert(podcast)
                                                        subscriptionManager.subscribe(podcast.origin)
                                                        isSubscribed = true
                                                        onSubscribed()
                                                        // Refresh episodes via RSS
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

                                    // 3. More options — copy RSS URL (reuse FeaturedCard circular icon button)
                                    val moreInteractionSource = remember { MutableInteractionSource() }
                                    val isMoreHovered by moreInteractionSource.collectIsHoveredAsState()
                                    var showPopup by remember { mutableStateOf(false) }
                                    Box(
                                        modifier = Modifier
                                            .size(DesignTokens.IconButton.Size)
                                            .clip(CircleShape)
                                            .border(DesignTokens.Border.Width, DesignTokens.Border.SecondaryColor, CircleShape)
                                            .background(if (isMoreHovered) colors.elevated else colors.surface)
                                            .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                                            .clickable(interactionSource = moreInteractionSource, indication = null) { showPopup = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.MoreHoriz, contentDescription = Strings["discover_more"], tint = colors.textSecondary, modifier = Modifier.size(DesignTokens.IconButton.IconSize))

                                        DropdownMenu(
                                            expanded = showPopup,
                                            onDismissRequest = { showPopup = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text(Strings["dialog_copy_to_clipboard"]) },
                                                onClick = {
                                                    showPopup = false
                                                    val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
                                                    val selection = java.awt.datatransfer.StringSelection(podcast.origin)
                                                    clipboard.setContents(selection, null)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Divider after header ──
                    item { HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp)) }

                    // ── Episode list ──
                    items(episodes) { episode ->
                        val isDownloading = episode.id in downloadingEpisodes
                        val progress = downloadProgress[episode.id]

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                                .clickable {
                                    scope.launch {
                                        val downloadRecord = database.downloads.getByEpisodeId(episode.id)
                                        val url = if (downloadRecord != null && File(downloadRecord.filePath).exists()) {
                                            downloadRecord.filePath
                                        } else {
                                            episode.audioUrl
                                        }
                                        val epWithUrl = episode.copy(audioUrl = url)
                                        playAndRecordHistory(database, playerState, epWithUrl, podcast.imageUrl)
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Cover — match text content height
                            AsyncImage(
                                model = episode.imageUrl ?: podcast.imageUrl,
                                contentDescription = episode.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))

                            // Content
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = episode.title,
                                    color = colors.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val dateStr = formatDate(episode.pubDate)
                                    if (dateStr.isNotEmpty()) {
                                        Text(
                                            text = dateStr,
                                            color = colors.textMuted,
                                            fontSize = 12.sp
                                        )
                                        Text(text = " · ", color = colors.textMuted, fontSize = 12.sp)
                                    }
                                    Text(
                                        text = formatDuration(episode.duration),
                                        color = colors.textMuted,
                                        fontSize = 12.sp
                                    )
                                }
                                if (episode.description.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = stripHtml(episode.description),
                                        color = colors.textMuted,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Action buttons
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Add to queue
                                IconButton(onClick = {
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
                                }) {
                                    Icon(Icons.Default.PlaylistAdd, contentDescription = Strings["episode_add_to_queue"])
                                }

                                // Download / Downloaded / Downloading
                                Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                                    if (episode.id in completedDownloads) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = Strings["episode_downloaded"],
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    } else if (isDownloading) {
                                        val fraction = if (progress != null && progress.second > 0) {
                                            progress.first.toFloat() / progress.second
                                        } else 0f
                                        CircularProgressIndicator(
                                            progress = { fraction },
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        IconButton(onClick = { onStartDownload(episode, podcast.title) }) {
                                            Icon(Icons.Default.Download, contentDescription = Strings["episode_download"])
                                        }
                                    }
                                }
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
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

private fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, secs)
    } else {
        "%d:%02d".format(minutes, secs)
    }
}
