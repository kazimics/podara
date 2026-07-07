package app.podara.api.apple

import app.podara.api.apple.route.Lookup
import app.podara.api.apple.route.Search
import app.podara.api.apple.route.TopPodcasts
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

val json = Json { ignoreUnknownKeys = true }

class ApplePodcastClient {

    val httpClient = HttpClient {
        followRedirects = true

        install(ContentNegotiation) {
            json(json = json)
        }
    }

    val lookup = Lookup(this)
    val search = Search(this)
    val topPodcasts = TopPodcasts(this)

    fun close() {
        httpClient.close()
    }
}
