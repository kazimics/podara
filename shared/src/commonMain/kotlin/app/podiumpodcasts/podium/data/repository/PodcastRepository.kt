package app.podiumpodcasts.podium.data.repository

import app.podiumpodcasts.podium.data.model.Podcast
import app.podiumpodcasts.podium.data.model.PodcastEpisode
import app.podiumpodcasts.podium.data.model.PodcastEpisodeBundle
import app.podiumpodcasts.podium.sqldelight.PodiumDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PodcastRepository(private val database: PodiumDatabase) {

    fun getAll(): Flow<List<Podcast>> {
        return database.podcastQueries.getAll()
            .asFlow()
            .map { query ->
                query.executeAsList().map { it.toPodcast() }
            }
    }

    fun get(origin: String): Flow<Podcast?> {
        return database.podcastQueries.getByOrigin(origin)
            .asFlow()
            .map { it.executeAsOneOrNull()?.toPodcast() }
    }

    suspend fun getAllSync(): List<Podcast> {
        return database.podcastQueries.getAll().executeAsList().map { it.toPodcast() }
    }

    suspend fun getAllOrigins(): List<String> {
        return database.podcastQueries.getAllOrigins().executeAsList()
    }

    suspend fun getByOrigin(origin: String): Podcast? {
        return database.podcastQueries.getByOrigin(origin).executeAsOneOrNull()?.toPodcast()
    }

    suspend fun search(query: String): List<Podcast> {
        return database.podcastQueries.search(query, query, query)
            .executeAsList().map { it.toPodcast() }
    }

    suspend fun insert(podcast: Podcast) {
        database.podcastQueries.insert(
            origin = podcast.origin,
            link = podcast.link,
            title = podcast.title,
            description = podcast.description,
            author = podcast.author,
            imageUrl = podcast.imageUrl,
            imageSeedColor = podcast.imageSeedColor,
            languageCode = podcast.languageCode,
            fileSize = podcast.fileSize,
            overrideTitle = podcast.overrideTitle,
            skipBeginning = podcast.skipBeginning,
            skipEnding = podcast.skipEnding
        )
    }

    suspend fun updateFileSize(origin: String, fileSize: Long) {
        database.podcastQueries.updateFileSize(fileSize, origin)
    }

    suspend fun setOverrideTitle(origin: String, overrideTitle: String) {
        database.podcastQueries.setOverrideTitle(overrideTitle, origin)
    }

    suspend fun setSkipBeginning(origin: String, skipBeginning: Int) {
        database.podcastQueries.setSkipBeginning(skipBeginning.toLong(), origin)
    }

    suspend fun setSkipEnding(origin: String, skipEnding: Int) {
        database.podcastQueries.setSkipEnding(skipEnding.toLong(), origin)
    }

    suspend fun updateImageSeedColor(origin: String, imageSeedColor: Int) {
        database.podcastQueries.updateImageSeedColor(imageSeedColor.toLong(), origin)
        database.podcastEpisodeQueries.updateImageSeedColor(imageSeedColor.toLong(), origin)
    }

    suspend fun delete(origin: String) {
        database.podcastQueries.delete(origin)
    }
}
