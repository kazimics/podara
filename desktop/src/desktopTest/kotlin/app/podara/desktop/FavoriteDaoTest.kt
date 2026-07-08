package app.podara.desktop

import app.podara.data.AppDatabase
import app.podara.data.model.PodcastEpisode
import app.podara.data.model.PodcastFavorite
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.*

class FavoriteDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var testDbFile: File

    @BeforeTest
    fun setup() {
        testDbFile = File(System.getProperty("java.io.tmpdir"), "podium_favorite_test_${System.currentTimeMillis()}.db")
        testDbFile.deleteOnExit()
        database = AppDatabase.build(testDbFile)
    }

    @AfterTest
    fun teardown() {
        database.close()
        testDbFile.delete()
    }

    @Test
    fun testInsertAndGetAll() {
        runBlocking {
            val episode = createEpisode("ep-1", "Episode 1")
            database.favorites.insert(episode)

            val favorites = database.favorites.getAllSync()
            assertEquals(1, favorites.size)
            assertEquals("ep-1", favorites[0].episodeId)
            assertEquals("Episode 1", favorites[0].title)
        }
    }

    @Test
    fun testDuplicateInsertReplacesSnapshot() {
        runBlocking {
            database.favorites.insert(createEpisode("ep-1", "Old Title"))
            database.favorites.insert(createEpisode("ep-1", "New Title"))

            val favorites = database.favorites.getAllSync()
            assertEquals(1, favorites.size)
            assertEquals("New Title", favorites[0].title)
        }
    }

    @Test
    fun testDeleteByEpisodeId() {
        runBlocking {
            database.favorites.insert(createEpisode("ep-1", "Episode 1"))
            database.favorites.insert(createEpisode("ep-2", "Episode 2"))

            database.favorites.delete("ep-1")

            val ids = database.favorites.getAllEpisodeIds()
            assertEquals(setOf("ep-2"), ids)
        }
    }

    @Test
    fun testToggleFavorite() {
        runBlocking {
            val episode = createEpisode("ep-1", "Episode 1")

            assertTrue(database.favorites.toggle(episode))
            assertTrue(database.favorites.isFavorite("ep-1"))

            assertFalse(database.favorites.toggle(episode))
            assertFalse(database.favorites.isFavorite("ep-1"))
        }
    }

    @Test
    fun testDeleteAll() {
        runBlocking {
            database.favorites.insert(createEpisode("ep-1", "Episode 1"))
            database.favorites.insert(createEpisode("ep-2", "Episode 2"))

            database.favorites.deleteAll()

            assertTrue(database.favorites.getAllSync().isEmpty())
        }
    }

    @Test
    fun testGetAllWithEpisodeUsesSnapshotWhenEpisodeMissing() {
        runBlocking {
            database.favorites.insert(PodcastFavorite(
                episodeId = "missing-ep",
                origin = "https://example.com/feed.xml",
                timestamp = 123L,
                title = "Snapshot Title",
                podcastTitle = "Snapshot Podcast",
                imageUrl = "https://example.com/image.jpg",
                audioUrl = "https://example.com/audio.mp3",
                duration = 120,
                pubDate = 456L
            ))

            val result = database.favorites.getAllWithEpisode()
            assertEquals(1, result.size)
            assertEquals("Snapshot Title", result[0].second!!.title)
            assertEquals("Snapshot Podcast", result[0].second!!.podcastTitle)
            assertEquals("https://example.com/audio.mp3", result[0].second!!.audioUrl)
        }
    }

    @Test
    fun testPersistenceAcrossReopen() {
        runBlocking {
            database.favorites.insert(createEpisode("ep-1", "Episode 1"))
        }
        database.close()

        database = AppDatabase.build(testDbFile)
        runBlocking {
            assertTrue(database.favorites.isFavorite("ep-1"))
            assertEquals(1, database.favorites.getAllSync().size)
        }
    }

    private fun createEpisode(id: String, title: String) = PodcastEpisode(
        id = id,
        guid = "guid-$id",
        origin = "https://example.com/feed.xml",
        link = "https://example.com/$id",
        title = title,
        description = "Description",
        imageUrl = "https://example.com/image.jpg",
        author = "Author",
        pubDate = 1000L,
        duration = 300,
        audioUrl = "https://example.com/audio.mp3",
        podcastTitle = "Test Podcast"
    )
}
