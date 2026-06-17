package app.podiumpodcasts.podium.data.repository

import app.podiumpodcasts.podium.data.model.PodcastHistory
import app.podiumpodcasts.podium.sqldelight.PodiumDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HistoryRepository(private val database: PodiumDatabase) {

    fun getAll(): Flow<List<PodcastHistory>> {
        return database.podcastHistoryQueries.getAll()
            .asFlow()
            .map { query ->
                query.executeAsList().map {
                    PodcastHistory(
                        id = it.id.toInt(),
                        origin = it.origin,
                        episodeId = it.episodeId,
                        timestamp = it.timestamp
                    )
                }
            }
    }

    fun getLast(): Flow<PodcastHistory?> {
        return database.podcastHistoryQueries.getLast()
            .asFlow()
            .map { query ->
                query.executeAsOneOrNull()?.let {
                    PodcastHistory(
                        id = it.id.toInt(),
                        origin = it.origin,
                        episodeId = it.episodeId,
                        timestamp = it.timestamp
                    )
                }
            }
    }

    suspend fun getAllSync(): List<PodcastHistory> {
        return database.podcastHistoryQueries.getAll().executeAsList().map {
            PodcastHistory(
                id = it.id.toInt(),
                origin = it.origin,
                episodeId = it.episodeId,
                timestamp = it.timestamp
            )
        }
    }

    suspend fun insert(origin: String, episodeId: String) {
        database.podcastHistoryQueries.insert(
            origin = origin,
            episodeId = episodeId,
            timestamp = kotlin.system.TimeSource.Monotonic.markNow().toEpochMilliseconds()
        )
    }

    suspend fun updateTimestamp(episodeId: String, timestamp: Long) {
        database.podcastHistoryQueries.updateTimestamp(timestamp, episodeId)
    }

    suspend fun delete(episodeId: String) {
        database.podcastHistoryQueries.delete(episodeId)
    }
}
