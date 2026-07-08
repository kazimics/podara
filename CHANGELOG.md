# Changelog

All notable changes to podara will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [1.0.0-alpha4] - 2026-07-08

### Added
- **Real-time seek preview** — dragging the progress slider in MiniPlayer and FullPlayer now shows the scrub position live in the time display, instead of only revealing the position after releasing the drag

### Fixed
- **Auto-play-next not firing after track completion** — three-part fix:
  - `keep-open=yes` mpv option prevents mpv from unloading the file at EOF, ensuring `eof-reached=yes` is detectable on the next poll tick
  - Replaced `pendingPlay` flag with a timestamp-based guard (3s window) that reliably distinguishes false start-up transitions from real EOF, while also preventing the pause handler on the same poll tick from overriding the state just set by `playNext()`
  - Added PAUSED→PLAYING resume detection in the pause handler so that `isPlaying` is restored after the brief `pause=yes` during initial file loading, ensuring the EOF handler can fire later
- **Play/Pause button icon stuck after track ends** — caused by the same root issue: EOF was never detected so `isPlaying` never transitioned to `false`; fixed by the three-part fix above

### Changed
- `lastPlayStartMs` timestamp recorded in `MediaPlayerState.play()` for discriminating stale vs real state transitions

## [1.0.0-alpha3] - 2026-07-07

### Changed
- **Project renamed to Podara** — full rename across the codebase:
  - Package: `app.podiumpodcasts.podium` → `app.podara`
  - Theme: `PodiumTheme` → `PodaraTheme`, `PodiumColors` → `PodaraColors`
  - App display name: "Podium" → "Podara"
  - Data directory: `~/.podium/` → `~/.podara/`
  - Database file: `podium.db` → `podara.db`
  - GitHub URLs updated to `kazimics/podara`
  - Remote URL, CI test paths, AND documentation all synced
- README / README_zh: removed Custom UI, Playback controls, Design system from Notable Features
- README / README_zh: fixed EXE description (it is an installer, not a portable executable)

## [1.0.0-alpha2] - 2026-07-06

### Added
- System tray icon with app logo (`SystemTrayManager.kt`) — always present while the app is running
- Tray right-click menu: Show/Hide Podara, Play/Pause, Previous, Next, Quit (Swing JDialog with Podara dark theme styling)
- Close behavior dialog: on first close, asks whether to quit or minimize to tray; "Remember my choice" checkbox persists the decision
- Close behavior setting in Settings → Close Behavior: "Ask me every time" / "Quit the app" / "Minimize to tray"
- `close_action` / `close_action_remembered` settings keys in `Settings.kt`
- 15 new i18n string keys for tray menu, close dialog, and close behavior settings (EN/ZH)
- `logo-16.png` resource for high-quality tray icon rendering

### Changed
- Close button no longer immediately exits — checks remembered close action or shows the close behavior dialog
- Window can be hidden to system tray while audio continues playing (requires "Minimize to tray" setting)
- Tray left-click restores hidden window and brings it to front

### Added
- Release workflow (`.github/workflows/release.yml`): triggered by `v*` tags, builds MSI + EXE installers and uploads to GitHub Releases as draft
- Custom app logo and window icon: `desktop/src/desktopMain/resources/` — replaces default Material `GraphicEq` icons and Java coffee-cup taskbar icon

### Changed
- `packageVersion` bumped from `0.1.0` to `1.0.0-alpha`
- Sidebar logo: replaced `Icons.Default.GraphicEq` with `logo-64.png` loaded via `BitmapPainter` + `decodeToImageBitmap()`, enlarged from 32dp → 40dp
- Sidebar title: "Podify" → `Strings["app_name"]` ("Podara"), enlarged from 17sp → 20sp
- Custom title bar icon: replaced `Icons.Default.GraphicEq` with `logo-64.png` (16dp)
- Window/taskbar icon: `logo-256.png` set via `Window` composable `icon` parameter
- Updated README / README_zh for 1.0.0-alpha release: removed "WIP" warnings, added download table, updated badge URLs to `kazimics/podara`

### Removed
- Old `LaunchedEffect` + `ImageIO` window-icon fallback (replaced by `Window.icon` parameter)

## [0.1.0] - 2026-07-05

### Added
- Custom app logo and window icon: `desktop/src/desktopMain/resources/` — replaces default Material `GraphicEq` icons and Java coffee-cup taskbar icon

### Changed
- Sidebar logo: replaced `Icons.Default.GraphicEq` with `logo-64.png` loaded via `BitmapPainter` + `decodeToImageBitmap()`, enlarged from 32dp → 40dp
- Sidebar title: "Podify" → `Strings["app_name"]` ("Podara"), enlarged from 17sp → 20sp
- Custom title bar icon: replaced `Icons.Default.GraphicEq` with `logo-64.png` (16dp)
- Window/taskbar icon: `logo-256.png` set via `Window` composable `icon` parameter (`appIcon` file-level lazy `BitmapPainter`)
- Removed 5 empty `feature-*` modules (discover, home, library, player, settings)
- Reorganized source into feature-based sub-packages: `screen/`, `player/`, `data/`, `api/`, `manager/`, `theme/`, `util/`
- Moved `App.kt` + `Main.kt` to root package `app.podiumpodcasts.podium`
- `ui/theme/` → `theme/`, `utils/` → `util/`, `desktop/player/` → `player/`
- Updated 60+ import paths across all source and test files

### Removed
- `feature-discover/`, `feature-home/`, `feature-library/`, `feature-player/`, `feature-settings/` (empty shells)
- `shared/` module (KMP split no longer needed for Windows-only app)
- Dead Android legacy code: `EpisodeListItem.kt`, `PodcastListItem.kt`, `ListItem.kt`, `ListModel.kt`
- `designer/` directory and `AGENTS.md`

## [0.1.0] - 2026-07-04

### Added
- Download speed limit setting with per-task rate limiter (Settings → Downloads → Download Speed Limit)
- Custom numerical input dialog for speed limit with validation (non-negative integer, KB/s unit)
- `Settings.getDownloadSpeedLimitKbps()` / `setDownloadSpeedLimitKbps()` for persisting speed limit
- `RateLimiter` class with cumulative byte tracking for per-task download throttling
- `HistoryDao.getLatestTimestampPerOrigin()` — GROUP BY query for last-listen timestamps
- Subscription list sorting: Name A-Z, Name Z-A, Recent Update (RSS feed), Recently Listened
- Sort dropdown menu with accent highlight on active option
- Drag-to-reorder for queue items using `detectDragGestures` on DragHandle
- Real-time drag animation — shifted items part to show insertion gap during drag
- `MediaPlayerState.moveQueueItem()` for queue reordering
- Select/Cancel mode toggle in QueueDrawer header
- FeaturedCard full-card hover overlay (subtle White 4% brightening)

### Changed
- SettingsScreen fully redesigned: MD3 `Scaffold` + `TopAppBar` + `ListItem` replaced with custom `Column` + `SectionHeader` + `SettingsRow`, matching app design system
- All SettingsScreen dialogs now use `colors.surface` container, 0-radius corners, explicit text colors
- `TextButton` → `SettingsActionText` (plain text + clickable, no MD3 hover/ripple) on all settings rows
- HomeScreen edit mode toolbar redesigned: MD3 `IconButton` + `Checkbox` → custom 32dp buttons + `SettingsActionText` select-all
- Edit/manage button hidden during editing mode
- Batch unsubscribe dialog styled with `containerColor = colors.surface` + explicit colors
- QueueDrawer redesigned: MD3 `TextButton` → plain text, `Checkbox` → custom 20dp circle, hover effects, DesignTokens spacing/colors
- Queue row height 80dp → 72dp, cover 56dp → 48dp, active row accent tint
- `LazyColumn` → `Column` + `verticalScroll` in QueueDrawer for per-item offset control during drag
- FeaturedCard nav button padding 12dp → 20dp to stay within content boundaries
- `DownloadManager` speed limit constructor parameter added, `RateLimiter` throttle during download loop

### Fixed
- Download speed limit unit mismatch (KB/s treated as bytes/s — 128x slowdown)
- Pause unresponsive during `RateLimiter` delay (delay split into 200ms chunks with `shouldStop` check)
- `Icons.Default.Speed` → `Icons.Default.Star` (icon not in default Material set)
- Missing outer Row closing brace in HomeScreen toolbar after edit mode refactor

### Removed
- Back navigation button from SettingsScreen (sidebar provides navigation)
- `CircleShape` / `ArrowBack` / `clip` / `collectIsHoveredAsState` unused imports from SettingsScreen
- `testSettingsBackButton` test (back button removed)

### Tests
- SettingsScreenTest: removed `testSettingsBackButton`
- 187 tests pass (all existing + new structure compatible)

### Added
- Download Management page with three-section layout (Summary + In Progress + Completed)
- True pause/resume for downloads using HTTP Range headers and partial file persistence
- Single episode delete and batch delete by podcast on downloaded content
- Download task table (`downloadTask`) for persistent pause/resume state tracking
- Download directory path display and storage usage statistics in Summary card
- Download progress ring with click toggle (pause / resume) on PodcastDetailScreen episode rows
- `DownloadTaskDao` with CRUD operations for active download task management
- DB-backed active download check in PodcastDetailScreen for robust state across page navigation
- `cleanupPausedTask()` method for cleaning up paused/failed download partial files
- ConcurrentHashMap-based thread-safe pause/cancel tracking
- 20+ new i18n string keys: `downloads_subtitle`, `downloads_in_progress`, `downloads_completed`, `downloads_summary_downloads`, `downloads_summary_storage`, `downloads_summary_path`, `downloads_pause`, `downloads_resume`, `downloads_cancel`, `downloads_status_paused`, `downloads_status_failed`, `downloads_retry`, `downloads_delete`, `downloads_delete_confirm`, `downloads_delete_podcast_confirm`, `downloads_delete_all`, `downloads_file_missing`, `downloads_open_folder`

### Changed
- DownloadsScreen promoted from placeholder to full download management page
- DownloadManager rewritten with pause/resume/cancel/delete support
- `podcastDownload` data class extended with `episodeTitle` field
- `DownloadDao.getAllValid()` and related methods added for download list queries
- StartDownload guard prevents duplicate downloads for the same episode
- PodcastDetailScreen episode row cursor changed to default (removed row-wide hand cursor)

### Fixed
- Download state lost across page navigation (now checked from DB on re-mount)
- Paused downloads showing download button after page switch (now shows progress ring)
- Empty progress ring for paused tasks (now reads progress from DB)
- Cancel inactive for paused tasks (now cleans up DB task and partial file)
- Thread safety issue in pausedDownloads/cancelledDownloads MutableSet (now ConcurrentHashMap)

### Tests
- DownloadManagerTest: 19 tests (9 new: pause, cancel, cleanupPausedTask, delete single/by-origin, getAllValid filter, total bytes, task CRUD, missing file delete)
- DownloadsScreenTest: 12 tests (new file: empty state, title/subtitle, summary path, completed list, file size, delete callback, in-progress section, pause button, cancel button, mixed state, batch delete button)

### Added
- FullPlayer redesign: large cover (160dp), hero episode title (24sp), podcast name, description, metadata (date/duration), timeline slider, playback controls (speed/rewind-10s/play-pause-56dp/forward-10s/sleep-timer)
- Episode Notes section with HTML rendering (`<b>`, `<i>`, `<a>`, `<p>`, `<br>` support via `parseSimpleHtml()`)
- "You might also like" recommendations section (other episodes from same podcast)
- Download action button and More dropdown in FullPlayer
- MiniPlayer body click to toggle FullPlayer (with no hover/ripple effects)
- `currentEpisodeId` tracking in `MediaPlayerState` — FullPlayer queries DB for full episode info
- `playAndRecordHistory()` now accepts optional `podcastImageUrl` fallback for unsubscribed podcasts
- Episode is inserted into database on play to ensure FullPlayer can retrieve metadata
- `database` parameter in FullPlayer for episode/recommendation queries
- 8 new i18n string keys: `player_follow`, `player_following`, `player_episode_notes`, `player_show_more`, `player_show_less`, `player_you_might_also_like`, `player_rewind_15`, `player_forward_30`
- Subscriptions page redesign: custom header (title+count, subtitle, search bar, manage button), subscription cards (64dp cover, title, author, new-episode badge, episode count, more menu)
- New i18n keys: `home_subscriptions`, `home_subscriptions_desc`, `home_search_placeholder`, `home_manage`, `home_episode_count`, `home_new_badge`, `home_new_count`

### Changed
- FullPlayer controls size reduced to match DesignTokens (cover 160dp, play 56dp, circle buttons 40dp, speed 38×30, gaps 20dp)
- FullPlayer padding/spacing normalized (24dp between sections, 28dp horizontal padding)
- Forward/rewind buttons unified with MiniPlayer (10s, `Replay10`/`Forward10` icons)
- Speed selector uses `Popup` + custom `PopupPositionProvider` for reliable positioning below button
- MiniPlayer expand button disabled when no track is loaded
- HistoryScreen play calls now pass `episodeId` for FullPlayer lookup
- Removed FollowingButton from FullPlayer (action buttons now only Download + More)
- HomeScreen redesigned: custom header replaces TopAppBar, Material ListItems replaced with custom SubscriptionCards, added local search and manage mode

### Tests
- `testCurrentEpisodeIdIsNullByDefault` — verifies initial null state
- `testCurrentEpisodeIdSetWhenPlaying` — verifies episodeId tracking after play
- `testCurrentEpisodeIdClearedOnStop` — verifies cleanup on stop
- `testMiniPlayerSeekButtonsDoNotTriggerBodyClick` — body toggle not triggered by controls
- `testMiniPlayerPlayButtonDoesNotTriggerBodyClick` — body toggle not triggered by play/pause
- `testMiniPlayerExpandButtonDisabledWhenNoPlayback` — expand disabled with no URL
- `testFullPlayerShowsCloseButton`, `testFullPlayerShowsPodcastName` — basic rendering

## [0.3.0] - 2026-07-03

### Changed
- HistoryScreen redesigned: unified Column padding (28dp top, 32dp sides, 8dp bottom) matching Subscriptions page layout, serif PageHeader (32sp) + subtitle, date grouping (Today/Yesterday/This Week/Earlier), search bar (320dp), hover-highlighted cards with duration badges and action buttons, relative time display, clear-all dialog
- PodcastDetailScreen redesigned: removed Scaffold+TopAppBar in favor of Column layout with back button Row, consistent 32dp page margins, episode rows with hover effects (elevated background + hand cursor), unified 32dp padding before background for proper margin-aware hover, header cover aligned with episode row covers at 44dp (32dp margin + 12dp inset)
- Download completed CheckCircle icon unified to `colors.success` (green) across PodcastDetailScreen and FullPlayer
- Added `pointerHoverIcon(HAND_CURSOR)` to HistoryItem action buttons (queue, remove) and podcast name link

### Added
- History search field with text filtering by episode/podcast title
- Relative timestamps ("Just now", "Xm ago", "Xh ago", "Yesterday", "MMM dd") in history list
- Date-grouped sections in history (Today/Yesterday/This Week/Earlier)
- `history_subtitle`, `history_search_placeholder`, `history_today`, `history_yesterday`, `history_this_week`, `history_earlier`, `history_count` i18n keys (EN/ZH)

### Fixed
- HistoryScreen hover background spanning full width (padding moved before background, matching Subscriptions page pattern)
- HistoryScreen dividers lacking right margin (end padding added)
- HistoryScreen episode count toolbar position inconsistent with Subscriptions page (unified Column padding eliminates jitter when switching pages)
- PodcastDetailScreen episode row hover background spanning full width (32dp horizontal padding applied before background)
- PodcastDetailScreen downloading progress indicator color set to `colors.accent`
- Material theme primary color reference (`colorScheme.primary`) replaced with PodaraTheme semantic colors

### Removed
- `@OptIn(ExperimentalMaterial3Api)` from PodcastDetailScreen (no longer needed after Scaffold removal)
- Back navigation button from HistoryScreen (page is sidebar-navigated, no TopAppBar)

### Tests
- HistoryScreenTest: removed `testHistoryBackButton` (page no longer has back button), added `testSearchBarIsDisplayed`, `testSubtitleText`

### Added
- Downloads placeholder page with empty state (navigated from sidebar)
- List toolbar on Subscriptions page: subscription count, sort icon button, manage icon button
- Horizontal divider above subscription list
- `cursorBrush` on subscriptions search bar for visible blinking cursor
- New i18n keys: `nav_subscriptions`, `home_subscription_count`, `home_sort`, `downloads_empty`, `downloads_empty_hint`

### Changed
- HomeScreen header redesigned to match Discover page: Serif title font, accent search icon, Surface+border search bar, top padding 28dp, removed count badge from title row
- SubscriptionCard: removed fixed height constraint on description (was clipping text), added `lineHeight = 14.sp` to author text
- EpisodeRow in DiscoverScreen: added `lineHeight = 14.sp` to author text for tighter vertical rhythm
- Sidebar navigation: second item relabeled from "Shows" to "Subscriptions" (中文: 订阅), third item relabeled to "History" (中文: 历史), downloads now navigates to dedicated DownloadsScreen
- Search bar Manage button removed from header (moved to list toolbar as icon)
- Sort and Manage buttons changed from text to icon buttons (UnfoldMore / Edit)
- `testFullPlayerShowsEpisodeNotesWithDataInDb` — notes section with DB data
- `testFullPlayerShowsYouMightAlsoLikeWithRecommendations` — recs with DB data
- `testNewStringKeysResolveToNonEmptyValues` — 4 new string keys verified
- All FullPlayer tests updated to pass `database` parameter

### Added
- Podcast detail page header with large cover (180dp), title, author, description, and three action buttons (Play Latest, Subscribe, More with "Copy RSS URL")
- Improved episode list layout: publication date, description text, 72dp cover filling row height
- `formatDate()` utility for formatting episode publication dates
- `stripHtml()` utility for cleaning HTML tags from RSS descriptions
- `onSubscribed` callback for PodcastDetailScreen to sync subscription state across screens
- `discoverRefreshKey` mechanism to reload DiscoverScreen subscription status on return
- EpisodeRow click navigation to podcast detail page (Trending Podcasts + search results)
- TopAppBar `containerColor = Color.Transparent` for consistent background across all screens

### Tests
- `PodcastDetailUtilTest` — unit tests for `stripHtml()` (plain text, nested tags, whitespace, empty input) and `formatDate()` (zero/negative, format pattern)
- `DiscoverScreenTest.testDiscoverScreenAcceptsRefreshKey` — verifies DiscoverScreen renders with `discoverRefreshKey` parameter
- `DiscoverScreenTest.testEpisodeRowFiresOnShowDetailWhenClicked` — verifies clicking EpisodeRow triggers the detail navigation callback

### Added
- Tests for `SectionHeader` `showAll` parameter behavior
- Language-aware country code selection for Top Podcasts feed (CN for zh, US for en)
- New i18n string keys for sidebar, title bar, discover screen, player, settings, and error messages
- Chinese translations (zh) for all previously missing UI strings
- `Strings.get()` format-parameter usage for error messages in DiscoverScreen and AddPodcastDialog
- Lightweight subscribe via `PodcastManager.addPodcastFromPreview()` — podcast created from Apple Podcast preview data without downloading RSS feed (instant subscribe)
- `onSubscribed` callback in DiscoverScreen to refresh subscription list immediately
- Retry button on podcast detail screen when episode loading fails
- Automatic RSS refresh when entering podcast detail screen (deferred episode loading)
- Play state initialization in `SubscriptionManager.updatePodcast()` for newly fetched episodes
- Hover cursor pointer for MiniPlayer controls (speed selector, seek, play, volume, queue, expand)
- iTunes episode lookup API (`entity=podcastEpisode`) — lightweight episode metadata fetch without full RSS download
- `PodcastEpisodeLookupResult` model and `lookupLatestEpisodes()` method on `ApplePodcastClient.Lookup`
- `RssConverter.parseFetchResult()` — shared helper for parsing RSS fetch results without exposing rssparser types
- `podcastItunesLookup` database table for persistent `itunes-lookup:xxx → RSS URL` mapping across restarts
- `ItunesLookupDao` for CRUD on the iTunes URL mapping table
- `loading_episodes` i18n string (EN/ZH)
- "Play Latest Episode" button on FeaturedCard — plays via iTunes API, no subscription required
- Entire FeaturedCard cover + text area clickable → navigates to episode list
- Subscription state feedback on FeaturedCard: spinner during subscribing, check icon when subscribed
- In-memory + persistent cache for `itunes-lookup:xxx` subscription status check across sessions
- Podcast artwork fallback: MiniPlayer now shows podcast cover when the episode has no artwork

### Changed
- "New Episodes" section renamed to "Trending Podcasts" (EN) / "热门播客" (ZH) to reflect actual content (Apple Top Podcasts chart)
- FeaturedCard now cycles through first 5 podcasts instead of the entire list
- Trending Podcasts list starts from index 6 (podcasts.drop(5)) instead of index 2 (drop(1))
- `SectionHeader` visibility changed from `private` to `internal` for testability
- Migrated all hardcoded English UI strings to `Strings["key"]` localization system (App, Discover, Player, Settings, History screens)
- Sidebar nav items now use string keys instead of literals for full i18n support
- Error messages in DiscoverScreen and AddPodcastDialog use `Strings.get()` with format parameters
- Window title in Main.kt uses `Strings["app_name"]` instead of hardcoded "Podara"
- All Chinese code comments translated to English
- `FetchPodcastClient.fetch()` made `open` for testability
- `SubscriptionManager.unsubscribe()` now also cleans up the iTunes lookup mapping
- `PodcastDetailScreen` receives `FetchPodcastClient` for unsubscribed preview mode with built-in loading
- FeaturedCard subscribe button now has three visual states: Add (unsubscribed), spinner (subscribing), Check (subscribed)

### Removed
- "Show All" button from Trending Podcasts section header
- Ctrl+K search shortcut badge test (UI element was removed in previous refactor)
- Auto-subscribe side effect when navigating to podcast detail from FeaturedCard

### Fixed
- DiscoverScreenTest compilation failure: missing `onPlayLatestEpisode` and `onShowDetail` parameters from FeaturedCard buttons rewrite
- Tests now use `Strings[...]` instead of hardcoded English text, making them locale-independent (fixes failures when system language is set to Chinese)
- `AppGUITest.testSettingsScreenAppName` and `SettingsScreenTest.testSettingsAppName` now use `Strings["settings_about_desc"]` instead of the raw English string
- MiniPlayer now falls back to podcast cover art when the played episode has no image of its own
- Audio stays paused when playing a new episode after pausing the previous one (mpv retains pause state across `loadfile`; polling thread's transition false no longer triggers `playNext()`)
- Podcast detail page unsubscribe button now correctly removes the podcast from the subscription list (was refreshing the database but not the in-memory list)
- Navigate to other sidebar tabs no longer blocked when inside podcast episode list
- Infinite loading spinner when RSS fetch fails in PodcastDetailScreen
- iTunes lookup URL (`itunes-lookup:`) causing HTTP error during episode refresh — now resolved to real feed URL at subscribe time
- DiscoverScreen subscription status incorrect for iTunes-sourced podcasts after refresh
- FeaturedCard "Latest Episode" button incorrectly called subscribe instead of playing the latest episode
- FeaturedCard right/left navigation arrows blocked by content overlay (z-order issue)
- `onShowDetail` previously auto-subscribed the podcast — now fetches RSS in preview mode only
- Podcast detail screen now handles unsubscribed (preview) mode — fetches RSS directly, hides unsubscribe button
- Subscribed podcast status lost after re-entering DiscoverScreen — now also checks via RSS URL cache
- Subscribed podcast status lost after app restart — now persists iTunes URL → RSS URL mapping in database
- Subscription status not synced when subscribing from podcast detail page (missing `podcast` record in DB, missing itunes-lookup mapping, missing `onSubscribed` callback)
- Subscription status not refreshed when returning to DiscoverScreen from podcast detail page (`discoverRefreshKey` mechanism now triggers reload)

## [0.1.0] - 2026-07-01

### Fixed
- MiniPlayer skip step inconsistent with FullPlayer (MiniPlayer 15s/30s → unified to 10s)
- MiniPlayer play button clickable when no track loaded, now disabled
- FullPlayer background not filling content area, switched to PodaraTheme dark background
- FullPlayer sidebar navigation blocked while expanded, clicking nav items now closes FullPlayer
- FullPlayer text and icon colors not applying dark theme palette

### Changed
- Volume button changed from `Icon.clickable` to `IconButton`, hover shape matches queue/expand buttons (circle), icon size reduced from 22dp to 18dp
- MiniPlayer shows podcast name below episode title (added `currentSubtitle` field)
- `QueueItem` data class now includes `subtitle` field
- `MediaPlayerState.play()` accepts new `subtitle` parameter
- Search interaction redesigned: click search icon to trigger search, Enter key triggers search, removed redundant "Search" button and Ctrl+K badge
- Search results only shown after user triggers a search, keeps Top Podcasts view when idle

## [0.1.0] - 2026-06-30

### Added
- Design system (`DesignTokens.kt`) — centralized design tokens for spacing, colors, typography, and component dimensions
- Custom immersive title bar with undecorated window (minimize, maximize, close buttons with hover effects)
- Window dragging via `WindowDraggableArea`
- Premium gradient button system (`Button.Gradient`, `Button.InnerHighlight`)
- Card background gradient (`Card.Gradient` — #1C1C1E → #15171B)
- Sidebar navigation component with accent highlighting and hover cursor
- Featured card on Discover page with cover art, description, and action buttons
- Podcast card horizontal scroll section ("Trending This Week")
- Episode row component for podcast lists
- Queue panel as fixed right-side drawer with cover art per item
- MiniPlayer redesign: cover art, speed selector, 15s/30s skip, volume slider, progress slider

### Changed
- Theme system upgraded: custom `PodaraColors` data class with `LocalPodaraColors` CompositionLocal
- Default screen changed from Home to Discover
- Color palette updated to match design system (warm gold accent, dark surface tones)
- All hardcoded dp/sp values in Sidebar, DiscoverScreen, PlayerUI replaced with `DesignTokens` references
- MiniPlayer layout: single Row with three-section layout (left: cover+title, center: controls, right: time+slider+volume)
- QueueDrawer redesigned with 320px fixed panel, 80px row height, cover thumbnails

### Removed
- TopAppBar navigation from Discover screen (replaced by Sidebar)
- Material3 default color scheme overrides (replaced by design system tokens)

## [0.1.0] - 2026-06-29

### Added
- `AudioPlayerEngine` interface for abstracting audio playback implementations
- `MpvAudioPlayerEngine` — new audio engine using libmpv via JNA
- `MpvApi` — JNA interface for libmpv C API
- `MpvNativeLoader` — native library loader for mpv-1.dll
- `MpvAudioPlayerEngineTest` — unit tests for mpv engine
- `PlaybackState` enum and `PlayerMetadata` data class for engine state tracking

### Changed
- Audio playback engine switched from Java Sound + Rubberband to libmpv
- `MediaPlayerState` now uses `MpvAudioPlayerEngine` as default engine
- Default engine can be overridden via constructor parameter
- Removed JavaFX MediaPlayer dependency (was unused for audio)

### Removed
- `JavaAudioPlayerEngine` (legacy Java Sound + Rubberband implementation)
- `RubberbandApi`, `RubberbandStretcher`, `RubberbandNativeLoader`
- `AudioDecoder`, `Mp3Decoder`, `M4aDecoder`, `SpiDecoder`, `JAADecAudioDecoder`
- `PitchPlayer`, `WsolaTimeStretch`, `JfxMediaPlayer`, `RubberbandPlayer`
- `PlaybackTest` (legacy JavaFX media test)
- Native libraries: rubberband.dll, libfftw3-3.dll, libsamplerate-0.dll, libstdc++-6.dll, libgcc_s_seh-1.dll, libwinpthread-1.dll
- JARs: jlayer.jar, jaad-0.9.4.jar, mp3spi.jar, javafx-*.jar

## [0.1.0] - 2026-06-26

### Added
- Podcast cover images display on Home, Discover, History screens, and episode lists
- Coil 3 image loading library for network image fetching
- Multi-language support (English / Simplified Chinese)
- Language selector in Settings screen
- Unsubscribe functionality with confirmation dialog on podcast detail screen
- Edit mode on Home screen for batch unsubscribe operations
- Subscribe button on Discover page now shows loading spinner during subscription
- Subscribe success icon aligns with add button for consistent layout
- Discover page now correctly shows subscribed podcasts with check icon across sessions

### Fixed
- Download status showing completed when local file no longer exists
- History list now falls back to podcast cover when episode has no image
- All GUI tests now pass with Coil image loading integration
- Discover page subscribe button lacked loading state feedback
- Subscribe success icon misaligned with add button icon
- Already-subscribed podcasts still showing add button on Discover page
- Imported podcasts not recognized as subscribed on Discover page

## [0.1.0] - 2026-06-25

### Fixed
- Progress bar position calculation: `totalInputSamples` unit mismatch caused position to advance at half speed for stereo audio
- Progress bar not syncing to total duration when playback finishes naturally
- Volume control now functional (was a no-op in custom audio pipeline)
- M4aDecoder resume: `seekToMs` was a no-op, now restarts playback from correct position
- Queue always showing empty: episodes now auto-added to queue when played
- Auto-advance to next queue item when track ends naturally

### Added
- Mute toggle: clicking volume icon mutes/unmutes, restoring previous volume level
- Queue button on mini player for quick access to playlist
- QueueSheet with selection mode, batch delete, and per-item download
- "Add to Queue" button on episode lists (podcast detail + history)

### Changed
- Player thread safety: added `@Volatile` to `isStopped`, `currentVolume`, `isPlaying`, `currentPosition`
- Player performance: pre-allocated buffers in playback loop (planar, byteOut) to reduce GC pressure
- Position callback throttled to 300ms intervals to reduce UI refresh overhead

## [0.1.0] - 2026-06-19

### Added
- Custom download path setting in Settings screen
- Settings persistence via `~/.podara/settings.properties`
- Download progress indicator with determinate progress
- Download state persists across navigation
- Download completed icon shows CheckCircle

### Fixed
- WSOLA time-stretch crash: IndexOutOfBounds when frame size < 2048
- WsolaTimeStretch now uses actual frame size instead of fixed 2048
- Download records persisted in database — download status survives path changes
- isDownloaded checks completedDownloads set (no file.exists() fallback)
- Play button checks database for actual file path before playing
- Download progress shows real-time incremental progress
- Download state persists when navigating away and back

### Changed
- Replaced VLC (vlcj) with JavaFX MediaPlayer for audio playback
- Removed 87MB VLC plugin bundling requirement
- Users no longer need to install VLC separately

## [0.1.0] - 2026-06-18

### Added
- Initial release of podara
- Podcast subscription via RSS feed URL
- Discover page with Apple Podcasts search and top podcasts
- Podcast detail page with episode list
- Mini player and full player UI
- Playback queue management
- Sleep timer
- Listening history tracking
- OPML import/export
- Episode download for offline listening
- Debug logging module (`~/.podara/debug.log`) for troubleshooting
- CHANGELOG.md
- Comprehensive test suite (67+ tests across 8 test classes)

### Fixed
- Serialization plugin missing in shared module causing Discover page to fail
- VLC path resolution fails in packaged app (URI is not hierarchical)
- JavaFX Media local file path not converted to URI format
- JavaFX Toolkit not initialized before MediaPlayer creation
- JavaFX native library loading from JAR with multiple fallback paths

### Added
- Pitch-preserving speed control using WSOLA time-stretching algorithm
- When speed != 1.0x, switches to PitchPlayer (JLayer decode + WSOLA + javax.sound output)
- When speed == 1.0x, uses JavaFX MediaPlayer normally
- Updated CI workflow to run new test classes
- Removed unused Android deploy workflow (deploy.yaml)
- Removed build artifacts from git tracking
