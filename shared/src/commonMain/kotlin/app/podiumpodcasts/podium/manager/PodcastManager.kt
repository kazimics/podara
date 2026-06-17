package app.podiumpodcasts.podium.manager

import app.podiumpodcasts.podium.api.rss.FetchPodcastClient
import app.podiumpodcasts.podium.api.rss.FetchPodcastClientResult
import app.podiumpodcasts.podium.data.model.Podcast
import app.podiumpodcasts.podium.data.model.PodcastEpisode
import app.podiumpodcasts.podium.data.repository.EpisodeRepository
import app.podiumpodcasts.podium.data.repository.PlayStateRepository
import app.podiumpodcasts.podium.data.repository.PodcastRepository
import app.podiumpodcasts.podium.utils.RssConverter

sealed class AddPodcastResult {
    data class Duplicate(val duplicate: Podcast) : AddPodcastResult()
    data class Created(val podcast: Podcast) : AddPodcastResult()
}

class PodcastManager(
    private val podcastRepository: PodcastRepository,
    private val episodeRepository: EpisodeRepository,
    private val playStateRepository: PlayStateRepository,
    private val fetchPodcastClient: FetchPodcastClient = FetchPodcastClient()
) {

    suspend fun addPodcast(
        origin: String,
        seedColor: Int?
    ): AddPodcastResult {
        podcastRepository.getByOrigin(origin)?.let { duplicate ->
            return AddPodcastResult.Duplicate(duplicate)
        }

        val response = fetchPodcastClient.fetchNoCache(origin)

        if (response !is FetchPodcastClientResult.Success)
            throw Exception(response.toString())

        val podcast = RssConverter.toPodcast(response.rssChannel, origin, response.fileSize, seedColor)
        val episodes = response.rssChannel.items.map { RssConverter.toPodcastEpisode(it, podcast) }

        return addPodcast(podcast, episodes, seedColor, false)
    }

    suspend fun addPodcast(
        podcast: Podcast,
        episodes: List<PodcastEpisode>,
        seedColor: Int?,
        duplicateCheck: Boolean = true
    ): AddPodcastResult {
        if (duplicateCheck) podcastRepository.getByOrigin(podcast.origin)?.let { duplicate ->
            return AddPodcastResult.Duplicate(duplicate)
        }

        val finalPodcast = podcast.copy(imageSeedColor = seedColor ?: podcast.imageSeedColor)
        val finalEpisodes = episodes.map { it.copy(imageSeedColor = finalPodcast.imageSeedColor) }

        podcastRepository.insert(finalPodcast)
        finalEpisodes.forEach { episodeRepository.insert(it) }
        finalEpisodes.forEach { playStateRepository.initState(it.id) }

        return AddPodcastResult.Created(podcast = finalPodcast)
    }
}
