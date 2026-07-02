package app.podiumpodcasts.podium.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.awt.Cursor
import app.podiumpodcasts.podium.api.apple.ApplePodcastClient
import app.podiumpodcasts.podium.api.model.PodcastPreviewModel
import app.podiumpodcasts.podium.data.AppDatabase
import app.podiumpodcasts.podium.manager.AddPodcastResult
import app.podiumpodcasts.podium.manager.PodcastManager
import app.podiumpodcasts.podium.manager.SubscriptionManager
import app.podiumpodcasts.podium.utils.Logger
import app.podiumpodcasts.podium.utils.Strings
import app.podiumpodcasts.podium.ui.theme.DesignTokens
import app.podiumpodcasts.podium.ui.theme.PodiumTheme
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch

// ── Design Tokens: button.primary ──
private val PrimaryButtonGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFC5976F),
        Color(0xFFBF936C),
        Color(0xFFB1845F)
    ),
    startY = 0f,
    endY = 48f
)
private val PrimaryButtonBorder = Color(0x14FFFFFF)
private val PrimaryButtonText = Color(0xFFFFF8F3)
private val PrimaryButtonIcon = Color.White

private const val TAG = "DiscoverScreen"

// Persists itunes-lookup:xxx → RSS feed URL mapping across composition boundaries
// so that subscription status can be checked correctly after re-entering DiscoverScreen.
private val itunesToRssCache = mutableMapOf<String, String>()

@Composable
fun DiscoverScreen(
    database: AppDatabase,
    subscriptionManager: SubscriptionManager,
    onSubscribed: () -> Unit,
    onBack: () -> Unit,
    onPlayLatestEpisode: (PodcastPreviewModel) -> Unit,
    onShowDetail: (PodcastPreviewModel) -> Unit
) {
    val colors = PodiumTheme.colors
    val header = DesignTokens.PageHeader
    val search = DesignTokens.SearchBar
    val scope = rememberCoroutineScope()
    val podcastManager = remember { PodcastManager(database) }
    val appleClient = remember { ApplePodcastClient() }

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(emptyList<PodcastPreviewModel>()) }
    var hasSearched by remember { mutableStateOf(false) }
    var topPodcasts by remember { mutableStateOf(emptyList<PodcastPreviewModel>()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var subscribedOrigins by remember { mutableStateOf(setOf<String>()) }
    var subscribingOrigins by remember { mutableStateOf(setOf<String>()) }

    DisposableEffect(Unit) {
        onDispose { appleClient.close() }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        Logger.i(TAG, "Loading top podcasts and subscriptions")
        try {
            val dbOrigins = database.podcasts.getAllOrigins()
            // Restore persisted itunes-lookup → RSS URL mappings from database
            itunesToRssCache.clear()
            itunesToRssCache.putAll(database.itunesLookup.getAll())
            topPodcasts = appleClient.topPodcasts.load()
            val itunesIds = topPodcasts
                .filter { it.fetchUrl.startsWith("itunes-lookup:") }
                .mapNotNull { it.fetchUrl.removePrefix("itunes-lookup:").toLongOrNull() }
            val resolvedMap = appleClient.lookup.batchLookupFeedUrls(itunesIds)
            val resolvedUrls = resolvedMap.values.toSet()
            // Cache itunes-lookup:xxx → RSS URL mapping for subscription status checks
            resolvedMap.forEach { (id, rssUrl) ->
                itunesToRssCache["itunes-lookup:$id"] = rssUrl
            }
            // Also track itunes-lookup:xxx mapping so subscription status check is correct
            val itunesLookupOrigins = resolvedMap.entries.map { "itunes-lookup:${it.key}" }.toSet()
            subscribedOrigins = dbOrigins + resolvedUrls + itunesLookupOrigins
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to load data", e)
            errorMessage = Strings.get("error_loading", e.message ?: "")
        }
        isLoading = false
    }

    val doSearch: () -> Unit = {
        if (searchQuery.isNotBlank()) {
            hasSearched = true
            scope.launch {
                isLoading = true
                errorMessage = null
                try {
                    searchResults = appleClient.search.search(searchQuery)
                } catch (e: Exception) {
                    Logger.e(TAG, "Search failed", e)
                    errorMessage = Strings.get("search_failed", e.message ?: "")
                }
                isLoading = false
            }
        }
    }

    val subscribe: (PodcastPreviewModel) -> Unit = { preview ->
        scope.launch {
            subscribingOrigins = subscribingOrigins + preview.fetchUrl
            try {
                val result = podcastManager.addPodcastFromPreview(preview, null)
                if (result is AddPodcastResult.Created || result is AddPodcastResult.Duplicate) {
                    // Get the podcast's actual origin (itunes-lookup: is resolved to real RSS URL)
                    val podcastOrigin = when (result) {
                        is AddPodcastResult.Created -> result.podcast.origin
                        is AddPodcastResult.Duplicate -> result.duplicate.origin
                    }
                    subscriptionManager.subscribe(podcastOrigin)
                    // Persist itunes-lookup → RSS URL mapping in database
                    database.itunesLookup.insert(preview.fetchUrl, podcastOrigin)
                    // Also cache in memory for the current session
                    itunesToRssCache[preview.fetchUrl] = podcastOrigin
                    subscribedOrigins = subscribedOrigins + preview.fetchUrl + podcastOrigin
                    onSubscribed()
                }
            } catch (e: Exception) {
                errorMessage = Strings.get("error_adding_podcast", e.message ?: "")
            } finally {
                subscribingOrigins = subscribingOrigins - preview.fetchUrl
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(colors.background)
    ) {
        // ── Header: Title + Search ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = header.PaddingHorizontal, end = header.PaddingHorizontal, top = header.PaddingTop, bottom = DesignTokens.Spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = Strings["nav_discover"],
                    color = colors.textPrimary,
                    fontSize = header.TitleSize,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                )
                Spacer(modifier = Modifier.height(header.Gap))
                Text(
                    text = Strings["discover_subtitle"],
                    color = colors.textMuted,
                    fontSize = header.SubtitleSize
                )
            }

            // Search bar
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
                        contentDescription = Strings["discover_search"],
                        tint = colors.accent,
                        modifier = Modifier
                            .size(search.IconSize)
                            .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                            .clickable { doSearch() }
                    )
                    Spacer(modifier = Modifier.width(search.Gap))
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = Strings["discover_search_placeholder"],
                                color = colors.textDisabled,
                                fontSize = search.TextSize
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onKeyEvent { event ->
                                    if (event.key == Key.Enter && event.type == KeyEventType.KeyUp && searchQuery.isNotBlank()) {
                                        doSearch()
                                        true
                                    } else false
                                },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = colors.textPrimary,
                                fontSize = search.TextSize
                            ),
                            singleLine = true,
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
                                .clickable {
                                    searchQuery = ""
                                    searchResults = emptyList()
                                    hasSearched = false
                                }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Content ──
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.accent)
                }
            }
            errorMessage != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = errorMessage!!, color = colors.danger)
                }
            }
            else -> {
                val podcasts = if (hasSearched) searchResults else topPodcasts
                if (podcasts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(Strings["discover_search_hint"], color = colors.textMuted)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        // ── Featured card (first podcast) ──
                        if (!hasSearched && podcasts.isNotEmpty()) {
                            item {
                                var featuredIndex by remember { mutableIntStateOf(0) }
                                val featured = podcasts[featuredIndex % podcasts.size]
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // arrows are inside FeaturedCard
                                }
                                FeaturedCard(
                                    podcast = featured,
                                    isSubscribed = featured.fetchUrl in subscribedOrigins
                                        || itunesToRssCache[featured.fetchUrl] in subscribedOrigins,
                                    isSubscribing = featured.fetchUrl in subscribingOrigins,
                                    onSubscribe = { subscribe(featured) },
                                    onPlayLatestEpisode = { onPlayLatestEpisode(featured) },
                                    onShowDetail = { onShowDetail(featured) },
                                    onPrevious = { featuredIndex = if (featuredIndex > 0) featuredIndex - 1 else podcasts.size - 1 },
                                    onNext = { featuredIndex = (featuredIndex + 1) % podcasts.size }
                                )
                            }
                        }

                        // ── Trending This Week ──
                        if (!hasSearched && podcasts.size > 1) {
                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                                SectionHeader(title = Strings["discover_trending"])
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = DesignTokens.SectionHeader.PaddingHorizontal)
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.PodcastCard.Gap)
                                ) {
                                    podcasts.drop(1).take(10).forEach { podcast ->
                                        PodcastCard(
                                            podcast = podcast,
                                            onClick = { /* TODO */ }
                                        )
                                    }
                                }
                            }
                        }

                        // ── New Episodes / All results ──
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            SectionHeader(
                                title = if (hasSearched) Strings["discover_results"] else Strings["discover_new_episodes"]
                            )
        Spacer(modifier = Modifier.height(DesignTokens.Spacing.sm))
                        }

                        val listItems = if (hasSearched) podcasts else podcasts.drop(1)
                        items(listItems) { podcast ->
                            Box(modifier = Modifier.padding(horizontal = DesignTokens.SectionHeader.PaddingHorizontal, vertical = 5.dp)) {
                                EpisodeRow(
                                    podcast = podcast,
                                    isSubscribed = podcast.fetchUrl in subscribedOrigins
                                        || itunesToRssCache[podcast.fetchUrl] in subscribedOrigins,
                                    isSubscribing = podcast.fetchUrl in subscribingOrigins,
                                    onSubscribe = { subscribe(podcast) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Featured Card ──
@Composable
private fun FeaturedCard(
    podcast: PodcastPreviewModel,
    isSubscribed: Boolean,
    isSubscribing: Boolean,
    onSubscribe: () -> Unit,
    onPlayLatestEpisode: () -> Unit,
    onShowDetail: () -> Unit,
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {}
) {
    val colors = PodiumTheme.colors
    val card = DesignTokens.FeaturedCard
    val btn = DesignTokens.Button
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DesignTokens.SectionHeader.PaddingHorizontal, vertical = 4.dp)
            .height(card.Height),
        shape = RoundedCornerShape(card.Radius),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(8.dp, RoundedCornerShape(card.Radius), ambientColor = Color.Black.copy(alpha = 0.4f), spotColor = Color.Black.copy(alpha = 0.4f))
                .clip(RoundedCornerShape(card.Radius))
                .background(DesignTokens.Card.Gradient)
        ) {
            // ── Navigation arrows in the top-right area, BEFORE content so they get correct layout
            //     but they render on TOP because they use TopEnd alignment and we layer them last ──

            // Content: Cover + Text (whole area clickable → navigate to detail)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(card.Padding)
                    .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                    .clickable { onShowDetail() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cover
                AsyncImage(
                    model = podcast.imageUrl,
                    contentDescription = podcast.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(card.CoverRadius))
                )

                Spacer(modifier = Modifier.width(card.ContentGap))

                // Text content
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = Strings["discover_featured"],
                        color = colors.accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(card.TextGap))
                    Box(modifier = Modifier.height(36.dp)) {
                        Text(
                            text = podcast.title,
                            color = colors.textPrimary,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.align(Alignment.CenterStart)
                        )
                    }
                    Spacer(modifier = Modifier.height(card.TextGap))
                    if (podcast.description.isNotEmpty()) {
                        Box(modifier = Modifier.height(40.dp)) {
                            Text(
                                text = podcast.description,
                                color = colors.textSecondary,
                                fontSize = 15.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 20.sp,
                                modifier = Modifier.align(Alignment.CenterStart)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Spacer(modifier = Modifier.height(card.ButtonGap))

                    // Three action buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(card.ButtonGap)) {
                        // Latest Episode — design token button
                        Box(
                            modifier = Modifier
                                .height(btn.Height)
                                .clip(RoundedCornerShape(btn.Radius))
                                .shadow(btn.ShadowElevation, RoundedCornerShape(btn.Radius), ambientColor = btn.ShadowColor, spotColor = btn.ShadowColor)
                                .border(DesignTokens.Border.Width, DesignTokens.Border.SecondaryColor, RoundedCornerShape(btn.Radius))
                                .background(btn.Gradient)
                                .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                                .clickable { onPlayLatestEpisode() },
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

                        // Add to playlist / Subscribe
                        val addInteractionSource = remember { MutableInteractionSource() }
                        val isAddHovered by addInteractionSource.collectIsHoveredAsState()
                        Box(
                            modifier = Modifier
                                .size(DesignTokens.IconButton.Size)
                                .clip(CircleShape)
                                .border(DesignTokens.Border.Width, DesignTokens.Border.SecondaryColor, CircleShape)
                                .background(
                                    when {
                                        isSubscribed -> colors.accent.copy(alpha = 0.15f)
                                        isAddHovered -> colors.elevated
                                        else -> colors.surface
                                    }
                                )
                                .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                                .clickable(interactionSource = addInteractionSource, indication = null) {
                                    if (!isSubscribed) onSubscribe()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                isSubscribing -> CircularProgressIndicator(
                                    modifier = Modifier.size(DesignTokens.IconButton.IconSize),
                                    strokeWidth = 2.dp,
                                    color = colors.accent
                                )
                                isSubscribed -> Icon(
                                    Icons.Default.Check,
                                    contentDescription = Strings["discover_added"],
                                    tint = colors.accent,
                                    modifier = Modifier.size(DesignTokens.IconButton.IconSize)
                                )
                                else -> Icon(
                                    Icons.Default.Add,
                                    contentDescription = Strings["discover_add"],
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(DesignTokens.IconButton.IconSize)
                                )
                            }
                        }

                        // More / Detail
                        val moreInteractionSource = remember { MutableInteractionSource() }
                        val isMoreHovered by moreInteractionSource.collectIsHoveredAsState()
                        Box(
                            modifier = Modifier
                                .size(DesignTokens.IconButton.Size)
                                .clip(CircleShape)
                                .border(DesignTokens.Border.Width, DesignTokens.Border.SecondaryColor, CircleShape)
                                .background(if (isMoreHovered) colors.elevated else colors.surface)
                                .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                                .clickable(interactionSource = moreInteractionSource, indication = null) { onShowDetail() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MoreHoriz, contentDescription = Strings["discover_more"], tint = colors.textSecondary, modifier = Modifier.size(DesignTokens.IconButton.IconSize))
                        }
                    }
                }
            }

            // Navigation arrows — top right, layered ON TOP of the content
            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(card.NavPadding),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val prevInteractionSource = remember { MutableInteractionSource() }
                val isPrevHovered by prevInteractionSource.collectIsHoveredAsState()
                Box(
                    modifier = Modifier
                        .size(card.NavButtonSize)
                        .clip(CircleShape)
                        .background(if (isPrevHovered) colors.elevated else colors.surface.copy(alpha = 0.6f))
                        .border(1.dp, colors.border, CircleShape)
                        .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                        .clickable(interactionSource = prevInteractionSource, indication = null) { onPrevious() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ChevronLeft,
                        contentDescription = Strings["discover_previous"],
                        tint = colors.textPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                val nextInteractionSource = remember { MutableInteractionSource() }
                val isNextHovered by nextInteractionSource.collectIsHoveredAsState()
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isNextHovered) colors.elevated else colors.surface.copy(alpha = 0.6f))
                        .border(1.dp, colors.border, CircleShape)
                        .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                        .clickable(interactionSource = nextInteractionSource, indication = null) { onNext() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = Strings["discover_next"],
                        tint = colors.textPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ── Section Header ──
@Composable
private fun SectionHeader(title: String) {
    val colors = PodiumTheme.colors
    val sh = DesignTokens.SectionHeader
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = sh.PaddingHorizontal),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = colors.textPrimary,
            fontSize = sh.TitleSize,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Serif
        )
        Text(
            text = Strings["discover_show_all"],
            color = colors.accent,
            fontSize = sh.LinkSize,
            modifier = Modifier.pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
        )
    }
}

// ── Podcast Card (horizontal scroll) ──
@Composable
private fun PodcastCard(
    podcast: PodcastPreviewModel,
    onClick: () -> Unit
) {
    val colors = PodiumTheme.colors
    val pc = DesignTokens.PodcastCard
    Surface(
        modifier = Modifier
            .width(pc.Width)
            .shadow(8.dp, RoundedCornerShape(pc.ImageRadius), ambientColor = Color.Black.copy(alpha = 0.4f), spotColor = Color.Black.copy(alpha = 0.4f))
            .clip(RoundedCornerShape(pc.ImageRadius))
            .background(DesignTokens.Card.Gradient)
            .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(pc.ImageRadius),
        color = Color.Transparent
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            AsyncImage(
                model = podcast.imageUrl,
                contentDescription = podcast.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(pc.ImageSize)
                    .clip(RoundedCornerShape(pc.ImageRadius))
            )
            Spacer(modifier = Modifier.height(pc.Spacing))
            Text(
                text = podcast.title,
                color = colors.textPrimary,
                fontSize = pc.TitleSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = podcast.author,
                color = colors.textMuted,
                fontSize = pc.AuthorSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ── Episode Row ──
@Composable
private fun EpisodeRow(
    podcast: PodcastPreviewModel,
    isSubscribed: Boolean,
    isSubscribing: Boolean,
    onSubscribe: () -> Unit
) {
    val colors = PodiumTheme.colors
    val er = DesignTokens.EpisodeRow
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(er.CoverRadius), ambientColor = Color.Black.copy(alpha = 0.4f), spotColor = Color.Black.copy(alpha = 0.4f))
            .clip(RoundedCornerShape(er.CoverRadius))
            .background(DesignTokens.Card.Gradient),
        shape = RoundedCornerShape(er.CoverRadius),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(er.Height)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(er.Spacing)
        ) {
            AsyncImage(
                model = podcast.imageUrl,
                contentDescription = podcast.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(er.CoverSize)
                    .clip(RoundedCornerShape(er.CoverRadius))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = podcast.title,
                    color = colors.textPrimary,
                    fontSize = er.TitleSize,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = podcast.author,
                    color = colors.textSecondary,
                    fontSize = er.AuthorSize,
                    maxLines = 1
                )
                if (podcast.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = podcast.description,
                        color = colors.textMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            when {
                isSubscribed -> {
                    Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = Strings["discover_added"],
                            tint = colors.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                isSubscribing -> {
                    Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = colors.accent
                        )
                    }
                }
                else -> {
                    IconButton(onClick = onSubscribe, modifier = Modifier.size(32.dp).pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = Strings["discover_add"],
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
