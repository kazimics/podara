package app.podara.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PodcastEpisode(
    val id: String,
    val guid: String,
    val origin: String,
    val link: String,
    val title: String,
    val description: String,
    val imageUrl: String? = null,
    val author: String,
    val pubDate: Long,
    val duration: Int,
    val audioUrl: String,
    val podcastTitle: String,
    val imageSeedColor: Int = 0,
    val isNew: Boolean = false
)
