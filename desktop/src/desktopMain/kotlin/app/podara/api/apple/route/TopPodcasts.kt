package app.podara.api.apple.route

import app.podara.api.apple.ApplePodcastClient
import app.podara.api.apple.json
import app.podara.api.apple.model.Genre
import app.podara.api.apple.model.TopPodcastsResponse
import app.podara.api.model.PodcastPreviewModel
import io.ktor.client.call.body
import io.ktor.client.request.get

class TopPodcasts(
    val client: ApplePodcastClient
) {

    suspend fun load(
        countryCode: String = "US",
        limit: Int = 50,
        genre: Genre? = null
    ): List<PodcastPreviewModel> {
        val genreStr = genre?.let { "genre=${it.id}/" } ?: ""

        val body =
            client.httpClient.get("https://itunes.apple.com/$countryCode/rss/toppodcasts/limit=$limit/${genreStr}explicit=true/json")
                .body<String>()

        val response = json.decodeFromString<TopPodcastsResponse>(body)
        return response.feed.entry.mapNotNull { it.toPodcastPreview() }
    }

}
