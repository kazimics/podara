package app.podara.desktop

import app.podara.api.model.PodcastPreviewModel
import app.podara.data.AppDatabase
import app.podara.manager.AddPodcastResult
import app.podara.manager.PodcastManager
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.*

class AddPodcastFromPreviewTest {

    private lateinit var database: AppDatabase
    private lateinit var podcastManager: PodcastManager
    private lateinit var testDbFile: File

    private val preview = PodcastPreviewModel(
        fetchUrl = "https://example.com/feed.xml",
        link = "https://example.com",
        title = "Test Podcast",
        description = "A test podcast for preview subscription",
        author = "Test Author",
        imageUrl = "https://example.com/image.jpg",
        languageCode = "en"
    )

    @BeforeTest
    fun setup() {
        testDbFile = File(System.getProperty("java.io.tmpdir"), "podium_preview_test_${System.currentTimeMillis()}.db")
        testDbFile.deleteOnExit()
        database = AppDatabase.build(testDbFile)
        podcastManager = PodcastManager(database, fetchPodcastClient = FakeFetchPodcastClient())
    }

    @AfterTest
    fun teardown() {
        database.close()
        testDbFile.delete()
    }

    @Test
    fun testAddPodcastFromPreviewCreatesPodcast() = runBlocking {
        val result = podcastManager.addPodcastFromPreview(preview, null)

        assertTrue(result is AddPodcastResult.Created)
        val podcast = (result as AddPodcastResult.Created).podcast
        assertEquals("https://example.com/feed.xml", podcast.origin)
        assertEquals("Test Podcast", podcast.title)
        assertEquals("Test Author", podcast.author)
        assertEquals("A test podcast for preview subscription", podcast.description)
        assertEquals("https://example.com/image.jpg", podcast.imageUrl)
        assertEquals("en", podcast.languageCode)
    }

    @Test
    fun testAddPodcastFromPreviewSavesToDatabase() = runBlocking {
        podcastManager.addPodcastFromPreview(preview, null)

        val podcasts = database.podcasts.getAllSync()
        assertEquals(1, podcasts.size)
        assertEquals("Test Podcast", podcasts[0].title)
        assertEquals("https://example.com/feed.xml", podcasts[0].origin)
    }

    @Test
    fun testAddPodcastFromPreviewDuplicateReturnsDuplicate() = runBlocking {
        podcastManager.addPodcastFromPreview(preview, null)
        val secondResult = podcastManager.addPodcastFromPreview(preview, null)

        assertTrue(secondResult is AddPodcastResult.Duplicate)
    }

    @Test
    fun testAddPodcastFromPreviewDoesNotDownloadEpisodes() = runBlocking {
        podcastManager.addPodcastFromPreview(preview, null)

        // Key: preview subscription should NOT download any episodes (RSS request is skipped)
        val episodes = database.episodes.getAllByOrigin("https://example.com/feed.xml")
        assertEquals(0, episodes.size, "Preview subscription should not fetch any episodes")
    }

    @Test
    fun testAddPodcastFromPreviewWithDifferentOrigins() = runBlocking {
        val preview1 = preview.copy(fetchUrl = "https://example.com/feed1.xml", title = "Podcast One")
        val preview2 = preview.copy(fetchUrl = "https://example.com/feed2.xml", title = "Podcast Two")

        podcastManager.addPodcastFromPreview(preview1, null)
        podcastManager.addPodcastFromPreview(preview2, null)

        val podcasts = database.podcasts.getAllSync()
        assertEquals(2, podcasts.size)
    }

    @Test
    fun testAddPodcastFromPreviewAcceptsSeedColor() = runBlocking {
        val result = podcastManager.addPodcastFromPreview(preview, seedColor = 0xFF0000)

        assertTrue(result is AddPodcastResult.Created)
        val podcast = (result as AddPodcastResult.Created).podcast
        assertEquals(0xFF0000, podcast.imageSeedColor)
    }
}
