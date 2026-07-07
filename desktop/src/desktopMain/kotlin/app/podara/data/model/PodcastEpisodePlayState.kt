package app.podara.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PodcastEpisodePlayState(
    val episodeId: String,
    val state: Int = 0,
    val played: Boolean = false,
    val lastUpdate: Long = 0L
)
