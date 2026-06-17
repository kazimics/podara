package app.podiumpodcasts.podium.data

import app.podiumpodcasts.podium.data.repository.*
import app.podiumpodcasts.podium.sqldelight.PodiumDatabase
import java.io.File

class AppDatabase(database: PodiumDatabase) {
    val podcasts = PodcastRepository(database)
    val episodes = EpisodeRepository(database)
    val playStates = PlayStateRepository(database)
    val downloads = DownloadRepository(database)
    val history = HistoryRepository(database)
    val subscriptions = SubscriptionRepository(database)
    val lists = ListRepository(database)
    val syncActions = SyncRepository(database)
    val statistics = StatisticsRepository(database)
}

object DatabaseManager {
    private var database: AppDatabase? = null

    fun build(databaseDir: File): AppDatabase {
        return database ?: run {
            databaseDir.mkdirs()
            val sqlDriver = createDatabaseDriver(File(databaseDir, "podium.db"))
            val podiumDatabase = PodiumDatabase(sqlDriver)
            AppDatabase(podiumDatabase).also { database = it }
        }
    }

    fun close() {
        database = null
    }
}
