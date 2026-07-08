package app.podara.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PodcastFavorite(
    val episodeId: String,
    val origin: String,
    val timestamp: Long,
    val title: String,
    val podcastTitle: String,
    val imageUrl: String? = null,
    val audioUrl: String,
    val duration: Int,
    val pubDate: Long
)
