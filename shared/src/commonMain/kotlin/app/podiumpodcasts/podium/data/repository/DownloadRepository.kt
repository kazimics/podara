package app.podiumpodcasts.podium.data.repository

import app.podiumpodcasts.podium.sqldelight.PodiumDatabase

class DownloadRepository(private val database: PodiumDatabase) {

    suspend fun add(episodeId: String) {
        database.podcastEpisodeDownloadQueries.add(episodeId)
    }

    suspend fun updateState(episodeId: String, state: Int, filename: String?, progress: Long, size: Long, timestamp: Long) {
        database.podcastEpisodeDownloadQueries.updateStateAndInfo(
            state = state.toLong(),
            filename = filename,
            progress = progress,
            size = size,
            timestamp = timestamp,
            episodeId = episodeId
        )
    }

    suspend fun updateProgress(episodeId: String, state: Int, progress: Long) {
        database.podcastEpisodeDownloadQueries.updateProgress(
            state = state.toLong(),
            progress = progress,
            episodeId = episodeId
        )
    }

    suspend fun delete(episodeId: String) {
        database.podcastEpisodeDownloadQueries.delete(episodeId)
    }

    suspend fun getAllNotDownloaded(): List<String> {
        return database.podcastEpisodeDownloadQueries.getAllNotDownloaded().executeAsList()
    }
}
