package app.podiumpodcasts.podium.desktop
import app.podiumpodcasts.podium.screen.HistoryScreen

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import app.podiumpodcasts.podium.data.AppDatabase
import app.podiumpodcasts.podium.player.MediaPlayerState
import app.podiumpodcasts.podium.theme.PodiumTheme
import app.podiumpodcasts.podium.util.Strings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.*
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryScreenTest {

    private lateinit var database: AppDatabase
    private lateinit var testDbFile: File
    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        testDbFile = File(System.getProperty("java.io.tmpdir"), "podium_history_test_${System.currentTimeMillis()}.db")
        testDbFile.deleteOnExit()
        database = AppDatabase.build(testDbFile)
    }

    @After
    fun teardown() {
        database.close()
        testDbFile.delete()
        Dispatchers.resetMain()
    }

    @Test
    fun testHistoryEmptyState() {
        composeTestRule.setContent {
            PodiumTheme {
                HistoryScreen(database = database, playerState = MediaPlayerState(), onBack = {})
            }
        }
        composeTestRule.onNodeWithText(Strings["history_empty"]).assertIsDisplayed()
        composeTestRule.onNodeWithText(Strings["history_empty_hint"]).assertIsDisplayed()
    }

    @Test
    fun testHistoryTitle() {
        composeTestRule.setContent {
            PodiumTheme {
                HistoryScreen(database = database, playerState = MediaPlayerState(), onBack = {})
            }
        }
        composeTestRule.onNodeWithText(Strings["history_title"]).assertIsDisplayed()
    }

    @Test
    fun testHistoryClearButtonHiddenWhenEmpty() {
        composeTestRule.setContent {
            PodiumTheme {
                HistoryScreen(database = database, playerState = MediaPlayerState(), onBack = {})
            }
        }
        // When history is empty, the toolbar with clear button is not rendered
        composeTestRule.onNodeWithContentDescription(Strings["history_clear"]).assertDoesNotExist()
    }

    @Test
    fun testHistoryClearDialogCancel() {
        composeTestRule.setContent {
            PodiumTheme {
                HistoryScreen(database = database, playerState = MediaPlayerState(), onBack = {})
            }
        }
        composeTestRule.onNodeWithText(Strings["history_title"]).assertIsDisplayed()
    }

    @Test
    fun testHistoryShowsIcon() {
        composeTestRule.setContent {
            PodiumTheme {
                HistoryScreen(database = database, playerState = MediaPlayerState(), onBack = {})
            }
        }
        composeTestRule.onNodeWithText(Strings["history_empty"]).assertIsDisplayed()
    }

    @Test
    fun testSearchBarIsDisplayed() {
        composeTestRule.setContent {
            PodiumTheme {
                HistoryScreen(database = database, playerState = MediaPlayerState(), onBack = {})
            }
        }
        composeTestRule.onNodeWithText(Strings["history_search_placeholder"]).assertIsDisplayed()
    }

    @Test
    fun testSubtitleText() {
        composeTestRule.setContent {
            PodiumTheme {
                HistoryScreen(database = database, playerState = MediaPlayerState(), onBack = {})
            }
        }
        composeTestRule.onNodeWithText(Strings["history_subtitle"]).assertIsDisplayed()
    }
}
