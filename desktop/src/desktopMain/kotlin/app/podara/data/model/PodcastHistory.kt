package app.podara.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PodcastHistory(
    val id: Int = 0,
    val origin: String,
    val episodeId: String,
    val timestamp: Long
)
