package app.podara.desktop

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import app.podara.data.AppDatabase
import app.podara.data.model.PodcastEpisode
import app.podara.player.MediaPlayerState
import app.podara.screen.FavoritesScreen
import app.podara.theme.PodaraTheme
import app.podara.util.Strings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesScreenTest {

    private lateinit var database: AppDatabase
    private lateinit var testDbFile: File
    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        testDbFile = File(System.getProperty("java.io.tmpdir"), "podium_favorites_screen_test_${System.currentTimeMillis()}.db")
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
    fun testFavoritesEmptyState() {
        composeTestRule.setContent {
            PodaraTheme {
                FavoritesScreen(database = database, playerState = MediaPlayerState(), favoriteVersion = 0, onBack = {})
            }
        }

        composeTestRule.onNodeWithText(Strings["favorites_empty"]).assertIsDisplayed()
        composeTestRule.onNodeWithText(Strings["favorites_empty_hint"]).assertIsDisplayed()
    }

    @Test
    fun testFavoritesTitleAndSearch() {
        composeTestRule.setContent {
            PodaraTheme {
                FavoritesScreen(database = database, playerState = MediaPlayerState(), favoriteVersion = 0, onBack = {})
            }
        }

        composeTestRule.onNodeWithText(Strings["favorites_title"]).assertIsDisplayed()
        composeTestRule.onNodeWithText(Strings["favorites_subtitle"]).assertIsDisplayed()
        composeTestRule.onNodeWithText(Strings["favorites_search_placeholder"]).assertIsDisplayed()
    }

    @Test
    fun testFavoritesClearButtonHiddenWhenEmpty() {
        composeTestRule.setContent {
            PodaraTheme {
                FavoritesScreen(database = database, playerState = MediaPlayerState(), favoriteVersion = 0, onBack = {})
            }
        }

        composeTestRule.onNodeWithContentDescription(Strings["favorites_clear"]).assertDoesNotExist()
    }

    @Test
    fun testPlayingFavoriteRecordsHistory() {
        val episode = PodcastEpisode(
            id = "ep-history",
            guid = "guid-history",
            origin = "https://example.com/feed.xml",
            link = "https://example.com/history",
            title = "History Favorite Episode",
            description = "Description",
            imageUrl = null,
            author = "Author",
            pubDate = 1000L,
            duration = 300,
            audioUrl = "https://example.com/history.mp3",
            podcastTitle = "Favorite Podcast"
        )
        runBlocking { database.favorites.insert(episode) }
        val playerState = MediaPlayerState()

        composeTestRule.setContent {
            PodaraTheme {
                FavoritesScreen(database = database, playerState = playerState, favoriteVersion = 0, onBack = {})
            }
        }

        composeTestRule.onNodeWithText(episode.title).performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking { database.history.getAllSync().any { it.episodeId == episode.id } }
        }

        val history = runBlocking { database.history.getAllSync() }
        assert(history.single().origin == episode.origin)
        assert(history.single().episodeId == episode.id)
        assert(playerState.currentUrl == episode.audioUrl)
        assert(playerState.currentEpisodeId == episode.id)
    }

    @Test
    fun testFavoritesShowsSavedEpisode() {
        runBlocking {
            database.favorites.insert(PodcastEpisode(
                id = "ep-1",
                guid = "guid-1",
                origin = "https://example.com/feed.xml",
                link = "https://example.com/ep1",
                title = "Favorite Episode",
                description = "Description",
                imageUrl = null,
                author = "Author",
                pubDate = 1000L,
                duration = 300,
                audioUrl = "https://example.com/audio.mp3",
                podcastTitle = "Favorite Podcast"
            ))
        }

        composeTestRule.setContent {
            PodaraTheme {
                FavoritesScreen(database = database, playerState = MediaPlayerState(), favoriteVersion = 0, onBack = {})
            }
        }

        composeTestRule.onNodeWithText("Favorite Episode").assertIsDisplayed()
        composeTestRule.onNodeWithText("Favorite Podcast").assertIsDisplayed()
    }
}
