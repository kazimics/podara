package app.podiumpodcasts.podium.manager

import app.podiumpodcasts.podium.api.rss.FetchPodcastClient
import app.podiumpodcasts.podium.api.rss.FetchPodcastClientResult
import app.podiumpodcasts.podium.data.model.Podcast
import app.podiumpodcasts.podium.data.model.PodcastEpisode
import app.podiumpodcasts.podium.data.repository.EpisodeRepository
import app.podiumpodcasts.podium.data.repository.PlayStateRepository
import app.podiumpodcasts.podium.data.repository.PodcastRepository
import app.podiumpodcasts.podium.data.repository.SubscriptionRepository
import app.podiumpodcasts.podium.utils.RssConverter

class SubscriptionManager(
    private val podcastRepository: PodcastRepository,
    private val episodeRepository: EpisodeRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val playStateRepository: PlayStateRepository,
    private val fetchPodcastClient: FetchPodcastClient = FetchPodcastClient()
) {

    suspend fun subscribe(origin: String) {
        subscriptionRepository.insert(origin)
    }

    suspend fun unsubscribe(origin: String) {
        subscriptionRepository.delete(origin)
    }

    suspend fun isSubscribed(origin: String): Boolean {
        return subscriptionRepository.getByOriginSync(origin) != null
    }

    suspend fun updatePodcast(
        origin: String,
        seedColor: Int?
    ): UpdatePodcastResult {
        val subscription = subscriptionRepository.getByOriginSync(origin)
            ?: return UpdatePodcastResult.NotSubscribed

        val response = fetchPodcastClient.fetch(
            origin = origin,
            lastModified = subscription.cacheLastModified,
            eTag = subscription.cacheETag,
            contentLength = subscription.cacheContentLength
        )

        when (response) {
            is FetchPodcastClientResult.Unchanged -> {
                return UpdatePodcastResult.Unchanged(response.reason)
            }

            is FetchPodcastClientResult.Failure -> {
                return UpdatePodcastResult.Error(response.e)
            }

            is FetchPodcastClientResult.Success -> {
                val podcast = RssConverter.toPodcast(response.rssChannel, origin, response.fileSize, seedColor)
                val episodes = response.rssChannel.items.map { RssConverter.toPodcastEpisode(it, podcast) }

                podcastRepository.updateFileSize(origin, response.fileSize)
                subscriptionRepository.updateCache(origin, response.eTag, response.lastModified, response.contentLength)

                val existingEpisodeIds = episodeRepository.getEpisodeIds(origin)
                val newEpisodes = episodes.filter { it.id !in existingEpisodeIds }

                newEpisodes.forEach { episode ->
                    episodeRepository.insert(episode)
                    playStateRepository.initState(episode.id)
                }

                subscriptionRepository.updateLastUpdate(origin, kotlin.system.TimeSource.Monotonic.markNow().toEpochMilliseconds())

                return UpdatePodcastResult.Updated(podcast, newEpisodes.size)
            }
        }
    }

    suspend fun updateAllPodcasts(): List<UpdatePodcastResult> {
        val subscriptions = subscriptionRepository.getAllSync()
        return subscriptions.map { updatePodcast(it.origin, null) }
    }
}

sealed class UpdatePodcastResult {
    data class Updated(val podcast: Podcast, val newEpisodesCount: Int) : UpdatePodcastResult()
    data class Unchanged(val reason: String) : UpdatePodcastResult()
    data class Error(val exception: Exception) : UpdatePodcastResult()
    data object NotSubscribed : UpdatePodcastResult()
}
