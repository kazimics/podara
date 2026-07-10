package app.podara.desktop
import app.podara.screen.DownloadsScreen

import androidx.compose.ui.test.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import app.podara.data.AppDatabase
import app.podara.data.DownloadTask
import app.podara.manager.DownloadManager
import app.podara.theme.PodaraTheme
import app.podara.util.Strings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.*
import org.junit.Assert.*
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadsScreenTest {

    private lateinit var database: AppDatabase
    private lateinit var downloadManager: DownloadManager
    private lateinit var testDbFile: File
    private lateinit var testDownloadsDir: File
    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        testDbFile = File(System.getProperty("java.io.tmpdir"), "podium_downloads_test_${System.currentTimeMillis()}.db")
        testDbFile.deleteOnExit()
        testDownloadsDir = File(System.getProperty("java.io.tmpdir"), "podium_downloads_test_dir_${System.currentTimeMillis()}")
        testDownloadsDir.mkdirs()
        testDownloadsDir.deleteOnExit()
        database = AppDatabase.build(testDbFile)
        downloadManager = DownloadManager(database, testDownloadsDir)
    }

    @After
    fun teardown() {
        database.close()
        testDbFile.delete()
        testDownloadsDir.deleteRecursively()
        Dispatchers.resetMain()
    }

    private fun insertDownload(episodeId: String, origin: String, filePath: String, podcastTitle: String, episodeTitle: String) {
        runBlocking {
            database.downloads.insert(episodeId, origin, filePath, podcastTitle, episodeTitle)
        }
    }

    private fun createScreen(
        downloadingEpisodes: Set<String> = emptySet(),
        downloadProgress: Map<String, Pair<Long, Long>> = emptyMap(),
        downloadVersion: Int = 0,
        completedDownloads: Set<String> = emptySet(),
        activeDownloadMeta: Map<String, Pair<String, String>> = emptyMap(),
        onPauseDownload: ((String) -> Unit) = {},
        onResumeDownload: ((String) -> Unit) = {},
        onCancelDownload: ((String) -> Unit) = {},
        onDeleteDownloaded: ((String) -> Unit) = {},
        onDeleteDownloadedByOrigin: ((String) -> Unit) = {}
    ) {
        composeTestRule.setContent {
            PodaraTheme {
                DownloadsScreen(
                    database = database,
                    downloadManager = downloadManager,
                    downloadPath = testDownloadsDir.absolutePath,
                    downloadingEpisodes = downloadingEpisodes,
                    downloadProgress = downloadProgress,
                    downloadVersion = downloadVersion,
                    completedDownloads = completedDownloads,
                    activeDownloadMeta = activeDownloadMeta,
                    favoriteVersion = 0,
                    onPauseDownload = onPauseDownload,
                    onResumeDownload = onResumeDownload,
                    onCancelDownload = onCancelDownload,
                    onDeleteDownloaded = onDeleteDownloaded,
                    onDeleteDownloadedByOrigin = onDeleteDownloadedByOrigin,
                    onBack = {},
                    onOpenSettings = {}
                )
            }
        }
    }

    // ── Empty state ──

    @Test
    fun testEmptyState() {
        createScreen()
        composeTestRule.onNodeWithText(Strings["downloads_empty"]).assertIsDisplayed()
        composeTestRule.onNodeWithText(Strings["downloads_empty_hint"]).assertIsDisplayed()
    }

    @Test
    fun testTitleAndSubtitle() {
        createScreen()
        composeTestRule.onNodeWithText(Strings["nav_downloads"]).assertIsDisplayed()
        composeTestRule.onNodeWithText(Strings["downloads_subtitle"]).assertIsDisplayed()
    }

    @Test
    fun testEmptyStateHasNoSections() {
        createScreen()
        composeTestRule.onNodeWithText(Strings["downloads_in_progress"]).assertDoesNotExist()
        composeTestRule.onNodeWithText(Strings["downloads_completed"]).assertDoesNotExist()
    }

    // ── Summary card ──

    @Test
    fun testSummaryShowsPath() {
        createScreen()
        composeTestRule.onNodeWithText(Strings["downloads_summary_path"]).assertIsDisplayed()
    }

    // ── With completed downloads ──

    @Test
    fun testCompletedDownloadsSection() {
        val testFile = File(testDownloadsDir, "completed-test.mp3").apply { writeText("test data") }
        testFile.deleteOnExit()
        val episodeId = "completed-ep-1"
        insertDownload(episodeId, "https://example.com/test.xml", testFile.absolutePath, "Test Podcast", "Test Episode Title")

        createScreen(
            completedDownloads = setOf(episodeId),
            downloadVersion = 1
        )

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(Strings["downloads_completed"]).assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Podcast").assertIsDisplayed()
        composeTestRule.onNodeWithText(Strings["downloads_empty"]).assertDoesNotExist()
    }

    @Test
    fun testCompletedEpisodeShowsFileSize() {
        val testFile = File(testDownloadsDir, "size-test.mp3").apply { writeText("Hello World") }
        testFile.deleteOnExit()
        val episodeId = "size-ep"
        insertDownload(episodeId, "https://example.com/size.xml", testFile.absolutePath, "Size Pod", "Size Ep")

        createScreen(
            completedDownloads = setOf(episodeId),
            downloadVersion = 1
        )

        composeTestRule.waitForIdle()
        // "Hello World" is 11 bytes
        composeTestRule.onNodeWithText("11 B").assertIsDisplayed()
    }

    @Test
    fun testDeleteButtonTriggersCallback() {
        val testFile = File(testDownloadsDir, "delete-cb-test.mp3").apply { writeText("data") }
        testFile.deleteOnExit()
        val episodeId = "delete-cb-ep"
        insertDownload(episodeId, "https://example.com/delete-cb.xml", testFile.absolutePath, "Del Pod", "Del Ep")

        var deleteClicked = false
        createScreen(
            completedDownloads = setOf(episodeId),
            downloadVersion = 1,
            onDeleteDownloaded = { deleteClicked = true }
        )

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription(Strings["downloads_delete"]).performClick()
        composeTestRule.waitForIdle()

        assertTrue("Delete callback should have been triggered", deleteClicked)
    }

    @Test
    fun testPausedTaskTransitionsToSingleInProgressRow() {
        val episodeId = "transition-ep"
        runBlocking {
            database.downloadTasks.insert(DownloadTask(
                episodeId = episodeId, origin = "origin", audioUrl = "https://example.com/episode.mp3",
                podcastTitle = "Transition Podcast", episodeTitle = "Transition Episode",
                downloadedBytes = 100L, totalBytes = 200L, state = "PAUSED",
                createdAt = 1L, updatedAt = 1L
            ))
        }
        val downloadingEpisodes = mutableStateOf(emptySet<String>())

        composeTestRule.setContent {
            PodaraTheme {
                DownloadsScreen(
                    database = database,
                    downloadManager = downloadManager,
                    downloadPath = testDownloadsDir.absolutePath,
                    downloadingEpisodes = downloadingEpisodes.value,
                    downloadProgress = mapOf(episodeId to Pair(100L, 200L)),
                    downloadVersion = 1,
                    completedDownloads = emptySet(),
                    activeDownloadMeta = mapOf(episodeId to ("Transition Podcast" to "Transition Episode")),
                    favoriteVersion = 0,
                    onPauseDownload = {},
                    onResumeDownload = {},
                    onCancelDownload = {},
                    onDeleteDownloaded = {},
                    onDeleteDownloadedByOrigin = {},
                    onBack = {},
                    onOpenSettings = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(Strings["downloads_status_paused"]).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(Strings["downloads_resume"]).assertIsDisplayed()

        composeTestRule.runOnUiThread { downloadingEpisodes.value = setOf(episodeId) }

        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("Transition Episode").assertCountEquals(1)
        composeTestRule.onNodeWithText("(50%)").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(Strings["downloads_pause"]).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(Strings["downloads_resume"]).assertDoesNotExist()
        composeTestRule.onNodeWithText(Strings["downloads_empty"]).assertDoesNotExist()

        composeTestRule.runOnUiThread { downloadingEpisodes.value = emptySet() }

        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("Transition Episode").assertCountEquals(1)
        composeTestRule.onNodeWithText(Strings["downloads_status_paused"]).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(Strings["downloads_resume"]).assertIsDisplayed()
    }

    // ── In-progress downloads ──

    @Test
    fun testInProgressSectionShowsProgress() {
        val episodeId = "in-progress-ep"
        createScreen(
            downloadingEpisodes = setOf(episodeId),
            downloadProgress = mapOf(episodeId to Pair(500L, 1000L)),
            downloadVersion = 1,
            activeDownloadMeta = mapOf(episodeId to ("Active Podcast" to "Active Episode"))
        )

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(Strings["downloads_in_progress"]).assertIsDisplayed()
        composeTestRule.onNodeWithText("Active Episode").assertIsDisplayed()
        composeTestRule.onNodeWithText("Active Podcast").assertIsDisplayed()
        composeTestRule.onNodeWithText("(50%)").assertIsDisplayed()
    }

    @Test
    fun testPauseButtonIsDisplayedForInProgress() {
        val episodeId = "pause-btn-ep"
        createScreen(
            downloadingEpisodes = setOf(episodeId),
            downloadProgress = mapOf(episodeId to Pair(100L, 200L)),
            downloadVersion = 1,
            activeDownloadMeta = mapOf(episodeId to ("P" to "E"))
        )

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription(Strings["downloads_pause"]).assertIsDisplayed()
    }

    @Test
    fun testCancelButtonIsDisplayedForInProgress() {
        val episodeId = "cancel-btn-ep"
        createScreen(
            downloadingEpisodes = setOf(episodeId),
            downloadProgress = mapOf(episodeId to Pair(100L, 200L)),
            downloadVersion = 1,
            activeDownloadMeta = mapOf(episodeId to ("P" to "E"))
        )

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription(Strings["downloads_cancel"]).assertIsDisplayed()
    }

    @Test
    fun testInProgressWithCompletedNoEmptyState() {
        val testFile = File(testDownloadsDir, "mixed-test.mp3").apply { writeText("data") }
        testFile.deleteOnExit()
        val completedEp = "mixed-completed"
        insertDownload(completedEp, "https://example.com/mixed.xml", testFile.absolutePath, "Mixed Pod", "Mixed Ep")

        createScreen(
            downloadingEpisodes = setOf("mixed-active"),
            downloadProgress = mapOf("mixed-active" to Pair(50L, 100L)),
            downloadVersion = 1,
            completedDownloads = setOf(completedEp),
            activeDownloadMeta = mapOf("mixed-active" to ("Active" to "Active Ep"))
        )

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(Strings["downloads_in_progress"]).assertIsDisplayed()
        composeTestRule.onNodeWithText(Strings["downloads_completed"]).assertIsDisplayed()
        composeTestRule.onNodeWithText(Strings["downloads_empty"]).assertDoesNotExist()
    }

    @Test
    fun testBatchDeleteButton() {
        val testFile = File(testDownloadsDir, "batch-group-test.mp3").apply { writeText("data") }
        testFile.deleteOnExit()
        val episodeId = "batch-group-ep"
        insertDownload(episodeId, "https://example.com/batch-group.xml", testFile.absolutePath, "Batch Pod", "Batch Ep")

        createScreen(
            completedDownloads = setOf(episodeId),
            downloadVersion = 1
        )

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(Strings["downloads_delete_all"]).assertIsDisplayed()
    }
}
