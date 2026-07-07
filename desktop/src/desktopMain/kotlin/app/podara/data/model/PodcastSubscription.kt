package app.podara.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PodcastSubscription(
    val origin: String,
    val enableNotifications: Boolean = false,
    val enableAutoDownload: Boolean = false,
    val lastUpdate: Long = 0,
    val newEpisodes: Int = 0,
    val cacheETag: String = "",
    val cacheLastModified: String = "",
    val cacheContentLength: String = ""
)
