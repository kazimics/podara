package app.podiumpodcasts.podium.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UpdatePodcastRun(
    val timestamp: Long,
    val dataUsage: Long
)
