package app.podiumpodcasts.podium.manager

import app.podiumpodcasts.podium.data.model.Podcast
import app.podiumpodcasts.podium.data.repository.PodcastRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.StringWriter

class ExportManager(
    private val podcastRepository: PodcastRepository
) {

    suspend fun exportOpml(): String = withContext(Dispatchers.IO) {
        val podcasts = podcastRepository.getAllSync()

        val writer = StringWriter()
        writer.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        writer.appendLine("<opml version=\"2.0\">")
        writer.appendLine("  <head>")
        writer.appendLine("    <title>Podium Podcasts</title>")
        writer.appendLine("  </head>")
        writer.appendLine("  <body>")

        podcasts.forEach { podcast ->
            writer.appendLine("    <outline type=\"rss\" text=\"${escapeXml(podcast.title)}\" title=\"${escapeXml(podcast.title)}\" xmlUrl=\"${escapeXml(podcast.origin)}\" htmlUrl=\"${escapeXml(podcast.link)}\"/>")
        }

        writer.appendLine("  </body>")
        writer.appendLine("</opml>")

        writer.toString()
    }

    suspend fun importOpml(opmlContent: String): List<Podcast> = withContext(Dispatchers.IO) {
        val podcasts = mutableListOf<Podcast>()

        val regex = Regex("""<outline\s+type="rss"\s+[^>]*xmlUrl="([^"]+)"[^>]*/>""")
        val matches = regex.findAll(opmlContent)

        matches.forEach { match ->
            val xmlUrl = match.groupValues[1]
            podcasts.add(
                Podcast(
                    origin = xmlUrl,
                    link = "",
                    title = "",
                    description = "",
                    author = "",
                    imageUrl = "",
                    languageCode = ""
                )
            )
        }

        podcasts
    }

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
