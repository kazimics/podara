package app.podiumpodcasts.podium.data.repository

import app.podiumpodcasts.podium.data.model.PodcastEpisodePlayState
import app.podiumpodcasts.podium.sqldelight.PodiumDatabase

class PlayStateRepository(private val database: PodiumDatabase) {

    suspend fun initState(episodeId: String) {
        database.podcastEpisodePlayStateQueries.initState(episodeId)
    }

    suspend fun saveState(episodeId: String, state: Int) {
        database.podcastEpisodePlayStateQueries.saveState(state.toLong(), episodeId)
    }

    suspend fun savePlayed(episodeId: String, played: Boolean) {
        database.podcastEpisodePlayStateQueries.savePlayed(
            if (played) 1L else 0L,
            episodeId
        )
    }

    suspend fun saveStateAndPlayed(episodeId: String, state: Int, played: Boolean) {
        database.podcastEpisodePlayStateQueries.saveStateAndPlayed(
            state = state.toLong(),
            played = if (played) 1L else 0L,
            lastUpdate = kotlin.system.TimeSource.Monotonic.markNow().toEpochMilliseconds(),
            episodeId = episodeId
        )
    }
}
