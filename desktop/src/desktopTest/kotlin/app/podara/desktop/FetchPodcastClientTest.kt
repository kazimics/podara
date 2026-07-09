package app.podara.desktop

import app.podara.api.rss.FetchPodcastClient
import app.podara.api.rss.FetchPodcastClientResult
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FetchPodcastClientTest {

    @Test
    fun testFetchDoesNotTreatEqualContentLengthAsUnchanged() = runBlocking {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0" xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd">
              <channel>
                <title>Length Changed Podcast</title>
                <link>https://example.com</link>
                <description>Content changed even though length header matched</description>
                <item>
                  <title>Episode 1</title>
                  <guid>ep-1</guid>
                  <enclosure url="https://example.com/audio.mp3" type="audio/mpeg"/>
                </item>
              </channel>
            </rss>
        """.trimIndent()
        var getRequested = false
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/feed.xml") { exchange ->
                if (exchange.requestMethod == "HEAD") {
                    exchange.responseHeaders.add("Content-Length", "123")
                    exchange.sendResponseHeaders(200, -1)
                } else {
                    getRequested = true
                    val bytes = xml.toByteArray()
                    exchange.responseHeaders.add("Content-Type", "application/rss+xml; charset=utf-8")
                    exchange.sendResponseHeaders(200, bytes.size.toLong())
                    exchange.responseBody.use { it.write(bytes) }
                }
            }
            start()
        }

        try {
            val client = FetchPodcastClient()
            val result = client.fetch(
                origin = "http://127.0.0.1:${server.address.port}/feed.xml",
                lastModified = "",
                eTag = "",
                contentLength = "123"
            )

            assertTrue(getRequested, "Fetch should perform GET even when HEAD Content-Length matches cached value")
            assertTrue(result is FetchPodcastClientResult.Success)
            assertEquals("Length Changed Podcast", result.rssChannel.title)
        } finally {
            server.stop(0)
        }
    }
}
