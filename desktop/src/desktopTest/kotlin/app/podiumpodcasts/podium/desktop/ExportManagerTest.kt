package app.podiumpodcasts.podium.desktop

import app.podiumpodcasts.podium.data.AppDatabase
import app.podiumpodcasts.podium.data.model.Podcast
import app.podiumpodcasts.podium.manager.ExportManager
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.*

class ExportManagerTest {

    private lateinit var database: AppDatabase
    private lateinit var exportManager: ExportManager
    private lateinit var testDbFile: File

    @BeforeTest
    fun setup() {
        testDbFile = File(System.getProperty("java.io.tmpdir"), "podium_test_${System.currentTimeMillis()}.db")
        testDbFile.deleteOnExit()
        database = AppDatabase.build(testDbFile)
        exportManager = ExportManager(database)
    }

    @AfterTest
    fun teardown() {
        database.close()
        testDbFile.delete()
    }

    @Test
    fun testExportOpmlWithNoPodcasts() = runBlocking {
        val opml = exportManager.exportOpml()

        assertTrue(opml.contains("<?xml version=\"1.0\""))
        assertTrue(opml.contains("<opml version=\"2.0\">"))
        assertTrue(opml.contains("<title>Podium Podcasts</title>"))
        assertTrue(opml.contains("</opml>"))
    }

    @Test
    fun testExportOpmlWithPodcasts() = runBlocking {
        val podcast = Podcast(
            origin = "https://example.com/feed.xml",
            link = "https://example.com",
            title = "Test Podcast",
            description = "A test podcast",
            author = "Test Author",
            imageUrl = "https://example.com/image.jpg",
            imageSeedColor = 0,
            languageCode = "en",
            fileSize = 1000L,
            overrideTitle = "",
            skipBeginning = 0,
            skipEnding = 0
        )
        database.podcasts.insert(podcast)

        val opml = exportManager.exportOpml()

        assertTrue(opml.contains("Test Podcast"))
        assertTrue(opml.contains("https://example.com/feed.xml"))
        assertTrue(opml.contains("type=\"rss\""))
    }

    @Test
    fun testExportOpmlEscapesXml() = runBlocking {
        val podcast = Podcast(
            origin = "https://example.com/feed.xml",
            link = "https://example.com",
            title = "Podcast & <Title> \"Quotes\"",
            description = "Description",
            author = "Author",
            imageUrl = "https://example.com/image.jpg",
            imageSeedColor = 0,
            languageCode = "en",
            fileSize = 1000L,
            overrideTitle = "",
            skipBeginning = 0,
            skipEnding = 0
        )
        database.podcasts.insert(podcast)

        val opml = exportManager.exportOpml()

        assertTrue(opml.contains("&amp;"))
        assertTrue(opml.contains("&lt;"))
        assertTrue(opml.contains("&gt;"))
        assertTrue(opml.contains("&quot;"))
    }
}
