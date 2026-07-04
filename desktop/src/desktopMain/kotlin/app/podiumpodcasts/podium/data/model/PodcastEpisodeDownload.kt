package app.podiumpodcasts.podium.data.model

import kotlinx.serialization.Serializable

enum class PodcastEpisodeDownloadState(val value: Int) {
    NOT_DOWNLOADED(0),
    DOWNLOADING(1),
    DOWNLOADED(2)
}

@Serializable
data class PodcastEpisodeDownload(
    val episodeId: String,
    val state: Int = 0,
    val filename: String? = null,
    val progress: Long = 0,
    val size: Long = 0,
    val timestamp: Long = 0
) {
    fun parseState(): PodcastEpisodeDownloadState {
        return PodcastEpisodeDownloadState.entries.find { it.value == state }
            ?: PodcastEpisodeDownloadState.NOT_DOWNLOADED
    }
}
