package app.podiumpodcasts.podium.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PodcastEpisodeBundle(
    val episode: PodcastEpisode,
    val playState: PodcastEpisodePlayState? = null,
    val download: PodcastEpisodeDownload? = null
)
