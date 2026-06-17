package app.podiumpodcasts.podium.data.repository

import app.podiumpodcasts.podium.data.model.PodcastSubscription
import app.podiumpodcasts.podium.sqldelight.PodiumDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SubscriptionRepository(private val database: PodiumDatabase) {

    fun getAll(): Flow<List<PodcastSubscription>> {
        return database.podcastSubscriptionQueries.getAll()
            .asFlow()
            .map { query ->
                query.executeAsList().map { it.toPodcastSubscription() }
            }
    }

    fun getByOrigin(origin: String): Flow<PodcastSubscription?> {
        return database.podcastSubscriptionQueries.getByOrigin(origin)
            .asFlow()
            .map { it.executeAsOneOrNull()?.toPodcastSubscription() }
    }

    suspend fun getAllSync(): List<PodcastSubscription> {
        return database.podcastSubscriptionQueries.getAll().executeAsList().map { it.toPodcastSubscription() }
    }

    suspend fun getByOriginSync(origin: String): PodcastSubscription? {
        return database.podcastSubscriptionQueries.getByOrigin(origin).executeAsOneOrNull()?.toPodcastSubscription()
    }

    suspend fun getAllOrigins(): List<String> {
        return database.podcastSubscriptionQueries.getAllOrigins().executeAsList()
    }

    suspend fun getNotificationsEnabled(): List<PodcastSubscription> {
        return database.podcastSubscriptionQueries.getNotificationsEnabled().executeAsList().map { it.toPodcastSubscription() }
    }

    suspend fun getAutoDownloadEnabled(): List<PodcastSubscription> {
        return database.podcastSubscriptionQueries.getAutoDownloadEnabled(kotlin.system.TimeSource.Monotonic.markNow().toEpochMilliseconds())
            .executeAsList().map { it.toPodcastSubscription() }
    }

    suspend fun insert(origin: String, enableNotifications: Boolean = false, enableAutoDownload: Boolean = false) {
        database.podcastSubscriptionQueries.insert(
            origin = origin,
            enableNotifications = if (enableNotifications) 1L else 0L,
            enableAutoDownload = if (enableAutoDownload) 1L else 0L,
            lastUpdate = 0,
            newEpisodes = 0,
            cacheETag = "",
            cacheLastModified = "",
            cacheContentLength = ""
        )
    }

    suspend fun updateLastUpdate(origin: String, lastUpdate: Long) {
        database.podcastSubscriptionQueries.updateLastUpdate(lastUpdate, origin)
    }

    suspend fun updateNewEpisodes(origin: String, newEpisodes: Int) {
        database.podcastSubscriptionQueries.updateNewEpisodes(newEpisodes.toLong(), origin)
    }

    suspend fun setEnableNotifications(origin: String, enable: Boolean) {
        database.podcastSubscriptionQueries.setEnableNotifications(if (enable) 1L else 0L, origin)
    }

    suspend fun setEnableAutoDownload(origin: String, enable: Boolean) {
        database.podcastSubscriptionQueries.setEnableAutoDownload(if (enable) 1L else 0L, origin)
    }

    suspend fun updateCache(origin: String, eTag: String, lastModified: String, contentLength: String) {
        database.podcastSubscriptionQueries.updateCache(eTag, lastModified, contentLength, origin)
    }

    suspend fun delete(origin: String) {
        database.podcastSubscriptionQueries.delete(origin)
    }
}
