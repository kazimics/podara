package app.podiumpodcasts.podium.data.repository

import app.podiumpodcasts.podium.data.model.UpdatePodcastRun
import app.podiumpodcasts.podium.sqldelight.PodiumDatabase

class StatisticsRepository(private val database: PodiumDatabase) {

    suspend fun getAll(): List<UpdatePodcastRun> {
        return database.statisticsUpdatePodcastRunQueries.getAll().executeAsList().map {
            UpdatePodcastRun(
                timestamp = it.timestamp,
                dataUsage = it.dataUsage
            )
        }
    }

    suspend fun getSince(timestamp: Long): List<UpdatePodcastRun> {
        return database.statisticsUpdatePodcastRunQueries.getSince(timestamp).executeAsList().map {
            UpdatePodcastRun(
                timestamp = it.timestamp,
                dataUsage = it.dataUsage
            )
        }
    }

    suspend fun insert(timestamp: Long, dataUsage: Long) {
        database.statisticsUpdatePodcastRunQueries.insert(timestamp, dataUsage)
    }

    suspend fun deleteOlderThan(timestamp: Long) {
        database.statisticsUpdatePodcastRunQueries.deleteOlderThan(timestamp)
    }
}
