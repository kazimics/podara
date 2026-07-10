package app.podara.manager

import app.podara.api.rss.FetchPodcastClient
import app.podara.api.rss.FetchPodcastClientResult
import app.podara.data.AppDatabase
import app.podara.util.RssConverter

class SubscriptionManager(
    private val db: AppDatabase,
    private val fetchPodcastClient: FetchPodcastClient = FetchPodcastClient()
) {
    suspend fun subscribe(origin: String) {
        db.subscriptions.insert(origin, false, false)
    }

    suspend fun unsubscribe(origin: String) {
        db.episodes.deleteByOrigin(origin)
        db.podcasts.delete(origin)
        db.subscriptions.delete(origin)
        db.itunesLookup.deleteByRssUrl(origin)
    }

    suspend fun isSubscribed(origin: String): Boolean {
        return db.subscriptions.getByOriginSync(origin) != null
    }

    suspend fun updatePodcast(origin: String, seedColor: Int?): UpdatePodcastResult {
        val subscription = db.subscriptions.getByOriginSync(origin) ?: return UpdatePodcastResult.NotSubscribed

        val response = fetchPodcastClient.fetch(origin, subscription.cacheLastModified, subscription.cacheETag, subscription.cacheContentLength)
        return when (response) {
            is FetchPodcastClientResult.Unchanged -> UpdatePodcastResult.Unchanged(response.reason)
            is FetchPodcastClientResult.Failure -> UpdatePodcastResult.Error(response.e)
            is FetchPodcastClientResult.Success -> {
                val podcast = RssConverter.toPodcast(response.rssChannel, origin, response.fileSize, seedColor)
                val episodes = response.rssChannel.items.map { RssConverter.toPodcastEpisode(it, podcast) }

                db.subscriptions.updateCache(origin, response.eTag, response.lastModified, response.contentLength)
                db.podcasts.insert(podcast)

                val existingIds = db.episodes.getEpisodeIds(origin).toSet()
                val newEpisodes = episodes.filter { it.id !in existingIds }
                episodes.forEach { db.episodes.insert(it) }
                newEpisodes.forEach { db.playStates.initState(it.id) }
                db.subscriptions.updateLastUpdate(origin, System.currentTimeMillis())

                UpdatePodcastResult.Updated(podcast, newEpisodes.size)
            }
        }
    }
}

sealed class UpdatePodcastResult {
    data class Updated(val podcast: app.podara.data.model.Podcast, val newEpisodesCount: Int) : UpdatePodcastResult()
    data class Unchanged(val reason: String) : UpdatePodcastResult()
    data class Error(val exception: Exception) : UpdatePodcastResult()
    data object NotSubscribed : UpdatePodcastResult()
}
