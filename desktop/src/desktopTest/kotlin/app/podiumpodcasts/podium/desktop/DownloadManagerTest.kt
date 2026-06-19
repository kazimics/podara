package app.podiumpodcasts.podium.desktop

import app.podiumpodcasts.podium.data.AppDatabase
import app.podiumpodcasts.podium.manager.DownloadManager
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.*

class DownloadManagerTest {

    private lateinit var database: AppDatabase
    private lateinit var downloadManager: DownloadManager
    private lateinit var testDbFile: File
    private lateinit var testDownloadsDir: File

    @BeforeTest
    fun setup() {
        testDbFile = File(System.getProperty("java.io.tmpdir"), "podium_test_${System.currentTimeMillis()}.db")
        testDbFile.deleteOnExit()
        testDownloadsDir = File(System.getProperty("java.io.tmpdir"), "podium_test_downloads_${System.currentTimeMillis()}")
        testDownloadsDir.mkdirs()
        testDownloadsDir.deleteOnExit()
        database = AppDatabase.build(testDbFile)
        downloadManager = DownloadManager(database, testDownloadsDir)
    }

    @AfterTest
    fun teardown() {
        database.close()
        testDbFile.delete()
        testDownloadsDir.deleteRecursively()
    }

    @Test
    fun testGetDownloadFileReturnsCorrectPath() {
        val file = downloadManager.getDownloadFile("https://example.com/feed.xml", "https://example.com/audio.mp3")
        assertTrue(file.absolutePath.contains(testDownloadsDir.absolutePath))
        assertTrue(file.name.isNotEmpty())
    }

    @Test
    fun testGetDownloadFileConsistentPath() {
        val file1 = downloadManager.getDownloadFile("https://example.com/feed.xml", "https://example.com/audio.mp3")
        val file2 = downloadManager.getDownloadFile("https://example.com/feed.xml", "https://example.com/audio.mp3")
        assertEquals(file1.absolutePath, file2.absolutePath)
    }

    @Test
    fun testGetDownloadFileDifferentUrls() {
        val file1 = downloadManager.getDownloadFile("https://example.com/feed.xml", "https://example.com/audio1.mp3")
        val file2 = downloadManager.getDownloadFile("https://example.com/feed.xml", "https://example.com/audio2.mp3")
        assertNotEquals(file1.absolutePath, file2.absolutePath)
    }

    @Test
    fun testGetDownloadFileWithTitles() {
        val file = downloadManager.getDownloadFile(
            "https://example.com/feed.xml",
            "https://example.com/audio.m4a",
            episodeTitle = "E45 Test Episode",
            podcastTitle = "Test Podcast"
        )
        assertEquals("Test Podcast", file.parentFile.name)
        assertEquals("E45 Test Episode.m4a", file.name)
    }

    @Test
    fun testGetDownloadFileWithSpecialCharacters() {
        val file = downloadManager.getDownloadFile(
            "https://example.com/feed.xml",
            "https://example.com/audio.mp3",
            episodeTitle = "Episode: Test Special",
            podcastTitle = "Podcast With Backslash"
        )
        assertEquals("Podcast With Backslash", file.parentFile.name)
        assertEquals("Episode_ Test Special.mp3", file.name)
    }

    @Test
    fun testDownloadEpisodeWithTitles() = runBlocking {
        try {
            val result = downloadManager.downloadEpisode(
                episodeId = "test-ep",
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                origin = "https://example.com/feed.xml",
                episodeTitle = "Test Episode",
                podcastTitle = "My Podcast"
            )

            if (result.isSuccess) {
                val file = result.getOrNull()!!
                assertTrue(file.exists(), "Downloaded file should exist")
                assertTrue(file.length() > 0, "Downloaded file should not be empty")
                assertEquals("My Podcast", file.parentFile.name)
                assertEquals("Test Episode.mp3", file.name)
            }
        } catch (e: Exception) {
            println("Skipping test: Network not available: ${e.message}")
        }
    }

    @Test
    fun testGetDownloadFileWhenNotDownloaded() {
        val file = downloadManager.getDownloadFile(
            "https://example.com/feed.xml",
            "https://example.com/nonexistent.mp3"
        )
        assertFalse(file.exists())
    }

    @Test
    fun testSanitizeFileName() {
        assertEquals("test", downloadManager.sanitizeFileName("test"))
        assertEquals("a_b_c", downloadManager.sanitizeFileName("a/b\\c"))
        assertEquals("a_b_c_d", downloadManager.sanitizeFileName("a:b*c?d"))
        assertEquals("a__b", downloadManager.sanitizeFileName("a<>b"))
    }
}
