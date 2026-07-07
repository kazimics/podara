package app.podara.api.apple.route

import app.podara.api.apple.ApplePodcastClient
import app.podara.api.apple.json
import app.podara.api.apple.model.SearchResponse
import app.podara.api.model.PodcastPreviewModel
import io.ktor.client.call.body
import io.ktor.client.request.get

class Search(
    val client: ApplePodcastClient
) {

    suspend fun search(
        query: String,
        countryCode: String = "US"
    ): List<PodcastPreviewModel> {
        val url = "https://itunes.apple.com/search?media=podcast&country=$countryCode&term=${query}"

        val body = client.httpClient.get(url).body<String>()

        val response = json.decodeFromString<SearchResponse>(body)
        return response.results.mapNotNull { it.toPodcastPreview() }
    }

}
