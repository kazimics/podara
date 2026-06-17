package app.podiumpodcasts.podium.data.repository

import app.podiumpodcasts.podium.sqldelight.PodiumDatabase

class SyncRepository(private val database: PodiumDatabase) {

    suspend fun addAction(id: String, actionType: String, origin: String, audioUrl: String?, position: Int?, total: Int?) {
        database.syncActionQueries.addAction(
            id = id,
            actionType = actionType,
            origin = origin,
            audioUrl = audioUrl,
            position = position?.toLong(),
            total = total?.toLong(),
            timestamp = kotlin.system.TimeSource.Monotonic.markNow().toEpochMilliseconds()
        )
    }

    suspend fun addPlayState(origin: String, episodeId: String, audioUrl: String, duration: Int, state: Int, played: Boolean) {
        val id = "play_${episodeId}_${kotlin.system.TimeSource.Monotonic.markNow().toEpochMilliseconds()}"
        database.syncActionQueries.addAction(
            id = id,
            actionType = "PLAY",
            origin = origin,
            audioUrl = audioUrl,
            position = state.toLong(),
            total = duration.toLong(),
            timestamp = kotlin.system.TimeSource.Monotonic.markNow().toEpochMilliseconds()
        )
    }

    suspend fun getAll(): List<app.podiumpodcasts.podium.data.model.SyncAction> {
        return database.syncActionQueries.getAll().executeAsList().map {
            app.podiumpodcasts.podium.data.model.SyncAction(
                id = it.id,
                actionType = it.actionType,
                origin = it.origin,
                audioUrl = it.audioUrl,
                position = it.position?.toInt(),
                total = it.total?.toInt(),
                timestamp = it.timestamp
            )
        }
    }

    suspend fun getById(id: String): app.podiumpodcasts.podium.data.model.SyncAction? {
        return database.syncActionQueries.getById(id).executeAsOneOrNull()?.let {
            app.podiumpodcasts.podium.data.model.SyncAction(
                id = it.id,
                actionType = it.actionType,
                origin = it.origin,
                audioUrl = it.audioUrl,
                position = it.position?.toInt(),
                total = it.total?.toInt(),
                timestamp = it.timestamp
            )
        }
    }

    suspend fun delete(id: String) {
        database.syncActionQueries.delete(id)
    }

    suspend fun deleteAll() {
        database.syncActionQueries.deleteAll()
    }
}
