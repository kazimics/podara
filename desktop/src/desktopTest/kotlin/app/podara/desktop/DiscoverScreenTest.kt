package app.podara.desktop
import app.podara.screen.DiscoverScreen
import app.podara.screen.EpisodeRow
import app.podara.screen.SectionHeader

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import app.podara.api.model.PodcastPreviewModel
import app.podara.data.AppDatabase
import app.podara.manager.SubscriptionManager
import app.podara.theme.PodaraTheme
import app.podara.util.Strings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.*
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class DiscoverScreenTest {

    private lateinit var database: AppDatabase
    private lateinit var subscriptionManager: SubscriptionManager
    private lateinit var testDbFile: File
    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        testDbFile = File(System.getProperty("java.io.tmpdir"), "podium_discover_test_${System.currentTimeMillis()}.db")
        testDbFile.deleteOnExit()
        database = AppDatabase.build(testDbFile)
        subscriptionManager = SubscriptionManager(database)
    }

    @After
    fun teardown() {
        database.close()
        testDbFile.delete()
        Dispatchers.resetMain()
    }

    @Test
    fun testDiscoverScreenDisplaysTitle() {
        composeTestRule.setContent {
            PodaraTheme {
                DiscoverScreen(database = database, subscriptionManager = subscriptionManager, onSubscribed = {}, onBack = {}, onPlayLatestEpisode = {}, onShowDetail = {})
            }
        }
        composeTestRule.onNodeWithText(Strings["discover_title"]).assertIsDisplayed()
    }

    @Test
    fun testDiscoverScreenShowsSubtitle() {
        composeTestRule.setContent {
            PodaraTheme {
                DiscoverScreen(database = database, subscriptionManager = subscriptionManager, onSubscribed = {}, onBack = {}, onPlayLatestEpisode = {}, onShowDetail = {})
            }
        }
        composeTestRule.onNodeWithText(Strings["discover_subtitle"]).assertIsDisplayed()
    }

    @Test
    fun testDiscoverScreenHasSearchField() {
        composeTestRule.setContent {
            PodaraTheme {
                DiscoverScreen(database = database, subscriptionManager = subscriptionManager, onSubscribed = {}, onBack = {}, onPlayLatestEpisode = {}, onShowDetail = {})
            }
        }
        composeTestRule.onNodeWithText(Strings["discover_search_placeholder"]).assertIsDisplayed()
    }

    @Test
    fun testDiscoverScreenAcceptsRefreshKey() {
        composeTestRule.setContent {
            PodaraTheme {
                DiscoverScreen(database = database, subscriptionManager = subscriptionManager, discoverRefreshKey = 42, onSubscribed = {}, onBack = {}, onPlayLatestEpisode = {}, onShowDetail = {})
            }
        }
        composeTestRule.onNodeWithText(Strings["discover_title"]).assertIsDisplayed()
    }

    @Test
    fun testEpisodeRowFiresOnShowDetailWhenClicked() {
        val podcast = PodcastPreviewModel(
            fetchUrl = "https://example.com/feed.xml",
            link = "https://example.com",
            title = "Test Podcast",
            description = "A test podcast description",
            author = "Test Author",
            imageUrl = "https://example.com/image.jpg",
            languageCode = "en"
        )
        var detailClicked = false
        composeTestRule.setContent {
            PodaraTheme {
                EpisodeRow(
                    podcast = podcast,
                    isSubscribed = false,
                    isSubscribing = false,
                    onSubscribe = {},
                    onShowDetail = { detailClicked = true }
                )
            }
        }
        composeTestRule.onNodeWithText("Test Podcast").performClick()
        composeTestRule.waitForIdle()
        assert(detailClicked) { "onShowDetail should have been called when clicking the episode row" }
    }

    // ── SectionHeader ──

    @Test
    fun testSectionHeaderShowsShowAllByDefault() {
        composeTestRule.setContent {
            PodaraTheme {
                SectionHeader(title = "Test")
            }
        }
        composeTestRule.onNodeWithText(Strings["discover_show_all"]).assertIsDisplayed()
    }

    @Test
    fun testSectionHeaderHidesShowAllWhenFalse() {
        composeTestRule.setContent {
            PodaraTheme {
                SectionHeader(title = "Test", showAll = false)
            }
        }
        composeTestRule.onNodeWithText(Strings["discover_show_all"]).assertDoesNotExist()
    }

}
