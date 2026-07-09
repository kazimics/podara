package app.podara.desktop

import app.podara.data.AppDatabase
import app.podara.manager.SubscriptionManager
import app.podara.manager.UpdatePodcastResult
import kotlinx.coroutines.runBlocking
import java.io.File
import java.sql.DriverManager
import kotlin.test.*

class UpdatePodcastTest {

    private lateinit var database: AppDatabase
    private lateinit var subscriptionManager: SubscriptionManager
    private lateinit var testDbFile: File

    private val origin = "https://example.com/feed.xml"

    @BeforeTest
    fun setup() {
        testDbFile = File(System.getProperty("java.io.tmpdir"), "podium_update_test_${System.currentTimeMillis()}.db")
        testDbFile.deleteOnExit()
        database = AppDatabase.build(testDbFile)
        subscriptionManager = SubscriptionManager(database, fetchPodcastClient = FakeFetchPodcastClient())
    }

    @AfterTest
    fun teardown() {
        database.close()
        testDbFile.delete()
    }

    @Test
    fun testUpdatePodcastReturnsNotSubscribedWhenNoSubscription() = runBlocking {
        val result = subscriptionManager.updatePodcast(origin, null)

        assertTrue(result is UpdatePodcastResult.NotSubscribed)
    }

    @Test
    fun testUpdatePodcastFetchesAndInsertsEpisodes() = runBlocking {
        subscriptionManager.subscribe(origin)

        val result = subscriptionManager.updatePodcast(origin, null)

        assertTrue(result is UpdatePodcastResult.Updated)
        assertEquals(2, (result as UpdatePodcastResult.Updated).newEpisodesCount)

        val episodes = database.episodes.getAllByOrigin(origin)
        assertEquals(2, episodes.size)
    }

    @Test
    fun testUpdatePodcastInitializesPlayStates() = runBlocking {
        subscriptionManager.subscribe(origin)
        subscriptionManager.updatePodcast(origin, null)

        val episodes = database.episodes.getAllByOrigin(origin)
        assertTrue(episodes.isNotEmpty(), "Episodes should exist")

        // Directly query the play state table to verify each episode has a play state record
        val stateCount = countPlayStates()
        assertEquals(episodes.size, stateCount,
            "Each episode should have a play state entry after updatePodcast")
    }

    @Test
    fun testUpdatePodcastHandlesDuplicateEpisodes() = runBlocking {
        subscriptionManager.subscribe(origin)

        // First call inserts episodes
        val firstResult = subscriptionManager.updatePodcast(origin, null)
        assertTrue(firstResult is UpdatePodcastResult.Updated)
        assertEquals(2, (firstResult as UpdatePodcastResult.Updated).newEpisodesCount)

        // Second call should not insert duplicates
        val secondResult = subscriptionManager.updatePodcast(origin, null)
        assertTrue(secondResult is UpdatePodcastResult.Updated)
        assertEquals(0, (secondResult as UpdatePodcastResult.Updated).newEpisodesCount,
            "Repeated update should not insert duplicate episodes")

        val episodes = database.episodes.getAllByOrigin(origin)
        assertEquals(2, episodes.size, "Total episodes should still be 2")
    }

    @Test
    fun testUpdatePodcastPreservesPlayStatesOnRepeat() = runBlocking {
        subscriptionManager.subscribe(origin)
        subscriptionManager.updatePodcast(origin, null)

        // Repeated refresh
        subscriptionManager.updatePodcast(origin, null)

        val episodes = database.episodes.getAllByOrigin(origin)
        val stateCount = countPlayStates()
        assertEquals(episodes.size, stateCount,
            "Play states should be preserved after repeated updatePodcast")
    }

    @Test
    fun testUpdatePodcastUpdatesExistingPodcastAndEpisodeMetadata() = runBlocking {
        subscriptionManager.subscribe(origin)
        subscriptionManager.updatePodcast(origin, null)

        subscriptionManager = SubscriptionManager(
            database,
            fetchPodcastClient = FakeFetchPodcastClient(
                podcastTitle = "Updated Fake Podcast",
                episode1Title = "Updated Episode 1",
                episode1AudioUrl = "https://example.com/audio1-updated.mp3"
            )
        )

        val result = subscriptionManager.updatePodcast(origin, null)

        assertTrue(result is UpdatePodcastResult.Updated)
        assertEquals(0, result.newEpisodesCount)
        assertEquals("Updated Fake Podcast", database.podcasts.getByOrigin(origin)?.title)

        val episode = database.episodes.getById("$origin:ep-1")
        assertNotNull(episode)
        assertEquals("Updated Episode 1", episode.title)
        assertEquals("https://example.com/audio1-updated.mp3", episode.audioUrl)
        assertEquals("Updated Fake Podcast", episode.podcastTitle)
    }

    private fun countPlayStates(): Int {
        var count = 0
        DriverManager.getConnection("jdbc:sqlite:${testDbFile.absolutePath}").use { conn ->
            val rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM podcastEpisodePlayState")
            if (rs.next()) count = rs.getInt(1)
        }
        return count
    }
}
