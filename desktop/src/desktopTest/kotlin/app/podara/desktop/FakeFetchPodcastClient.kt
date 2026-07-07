package app.podara.desktop

import app.podara.api.rss.FetchPodcastClient
import app.podara.api.rss.FetchPodcastClientResult
import com.prof18.rssparser.RssParser

class FakeFetchPodcastClient : FetchPodcastClient() {

    private val rssParser = RssParser()

    private val fakeXml: String
        get() = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0" xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd">
              <channel>
                <title>Fake Podcast</title>
                <link>https://example.com</link>
                <description>A fake podcast for testing</description>
                <itunes:author>Fake Author</itunes:author>
                <itunes:image href="https://example.com/image.jpg"/>
                <item>
                  <title>Episode 1</title>
                  <link>https://example.com/ep1</link>
                  <description>First episode</description>
                  <guid>ep-1</guid>
                  <pubDate>Mon, 01 Jan 2024 00:00:00 +0000</pubDate>
                  <enclosure url="https://example.com/audio1.mp3" type="audio/mpeg"/>
                </item>
                <item>
                  <title>Episode 2</title>
                  <link>https://example.com/ep2</link>
                  <description>Second episode</description>
                  <guid>ep-2</guid>
                  <pubDate>Tue, 02 Jan 2024 00:00:00 +0000</pubDate>
                  <enclosure url="https://example.com/audio2.mp3" type="audio/mpeg"/>
                </item>
              </channel>
            </rss>
        """.trimIndent()

    override suspend fun fetchNoCache(origin: String): FetchPodcastClientResult {
        val channel = rssParser.parse(fakeXml)
        return FetchPodcastClientResult.Success(
            rssChannel = channel,
            fileSize = fakeXml.toByteArray().size.toLong(),
            lastModified = "",
            eTag = "",
            contentLength = ""
        )
    }

    override suspend fun fetch(
        origin: String,
        lastModified: String,
        eTag: String,
        contentLength: String
    ): FetchPodcastClientResult {
        val channel = rssParser.parse(fakeXml)
        return FetchPodcastClientResult.Success(
            rssChannel = channel,
            fileSize = fakeXml.toByteArray().size.toLong(),
            lastModified = "",
            eTag = "",
            contentLength = ""
        )
    }
}
