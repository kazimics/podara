package app.podiumpodcasts.podium.data.repository

import app.podiumpodcasts.podium.data.model.PodcastEpisode
import app.podiumpodcasts.podium.data.model.PodcastEpisodeBundle
import app.podiumpodcasts.podium.sqldelight.PodiumDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class EpisodeRepository(private val database: PodiumDatabase) {

    fun getAllByOrigin(origin: String): Flow<List<PodcastEpisodeBundle>> {
        return database.podcastEpisodeQueries.getAllByOrigin(origin)
            .asFlow()
            .map { query ->
                query.executeAsList().map { it.toPodcastEpisodeBundle() }
            }
    }

    fun getById(id: String): Flow<PodcastEpisodeBundle?> {
        return database.podcastEpisodeQueries.getById(id)
            .asFlow()
            .map { it.executeAsOneOrNull()?.toPodcastEpisodeBundle() }
    }

    fun getAllNew(): Flow<List<PodcastEpisodeBundle>> {
        return database.podcastEpisodeQueries.getAllNew()
            .asFlow()
            .map { query ->
                query.executeAsList().map { it.toPodcastEpisodeBundle() }
            }
    }

    suspend fun getByIdSync(id: String): PodcastEpisodeBundle? {
        return database.podcastEpisodeQueries.getById(id)
            .executeAsOneOrNull()?.toPodcastEpisodeBundle()
    }

    suspend fun getByOriginAndAudioUrl(origin: String, audioUrl: String): PodcastEpisodeBundle? {
        return database.podcastEpisodeQueries.getByOriginAndAudioUrl(origin, audioUrl)
            .executeAsOneOrNull()?.toPodcastEpisodeBundle()
    }

    suspend fun getEpisodeIds(origin: String): List<String> {
        return database.podcastEpisodeQueries.getEpisodeIds(origin).executeAsList()
    }

    suspend fun search(query: String): List<PodcastEpisodeBundle> {
        return database.podcastEpisodeQueries.search(query, query)
            .executeAsList().map { it.toPodcastEpisodeBundle() }
    }

    suspend fun markAsNew(id: String) {
        database.podcastEpisodeQueries.markAsNew(id)
    }

    suspend fun markAsNotNew(id: String) {
        database.podcastEpisodeQueries.markAsNotNew(id)
    }

    suspend fun updateImageSeedColor(origin: String, imageSeedColor: Int) {
        database.podcastEpisodeQueries.updateImageSeedColor(imageSeedColor.toLong(), origin)
    }

    suspend fun insert(episode: PodcastEpisode) {
        database.podcastEpisodeQueries.insert(
            id = episode.id,
            guid = episode.guid,
            origin = episode.origin,
            link = episode.link,
            title = episode.title,
            description = episode.description,
            imageUrl = episode.imageUrl,
            author = episode.author,
            pubDate = episode.pubDate,
            duration = episode.duration.toLong(),
            audioUrl = episode.audioUrl,
            podcastTitle = episode.podcastTitle,
            imageSeedColor = episode.imageSeedColor.toLong(),
            new_ = if (episode.isNew) 1L else 0L
        )
    }

    suspend fun update(episode: PodcastEpisode) {
        database.podcastEpisodeQueries.updateEpisode(
            title = episode.title,
            description = episode.description,
            imageUrl = episode.imageUrl,
            author = episode.author,
            pubDate = episode.pubDate,
            duration = episode.duration.toLong(),
            audioUrl = episode.audioUrl,
            podcastTitle = episode.podcastTitle,
            imageSeedColor = episode.imageSeedColor.toLong(),
            new_ = if (episode.isNew) 1L else 0L,
            id = episode.id
        )
    }

    suspend fun delete(id: String) {
        database.podcastEpisodeQueries.delete(id)
    }
}
