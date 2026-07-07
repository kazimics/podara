package app.podara.manager

import app.podara.api.apple.ApplePodcastClient
import app.podara.api.model.PodcastPreviewModel
import app.podara.api.rss.FetchPodcastClient
import app.podara.api.rss.FetchPodcastClientResult
import app.podara.data.AppDatabase
import app.podara.data.model.Podcast
import app.podara.data.model.PodcastEpisode
import app.podara.util.Logger
import app.podara.util.RssConverter

private const val TAG = "PodcastManager"

sealed class AddPodcastResult {
    data class Duplicate(val duplicate: Podcast) : AddPodcastResult()
    data class Created(val podcast: Podcast) : AddPodcastResult()
}

class PodcastManager(
    private val db: AppDatabase,
    private val fetchPodcastClient: FetchPodcastClient = FetchPodcastClient(),
    private val appleClient: ApplePodcastClient = ApplePodcastClient()
) {
    suspend fun addPodcast(origin: String, seedColor: Int?): AddPodcastResult {
        Logger.i(TAG, "addPodcast: origin=$origin")

        val resolvedOrigin: String
        val displayOrigin: String

        if (origin.startsWith("itunes-lookup:")) {
            val idStr = origin.removePrefix("itunes-lookup:")
            val id = idStr.toLongOrNull()
            if (id == null) {
                throw IllegalArgumentException("Invalid iTunes ID: $idStr")
            }
            Logger.d(TAG, "Resolving iTunes ID: $id")
            val preview = appleClient.lookup.lookupById(id)
                ?: throw Exception("iTunes lookup failed for ID: $id")
            resolvedOrigin = preview.fetchUrl
            displayOrigin = origin
            Logger.d(TAG, "Resolved to RSS feed: $resolvedOrigin")
        } else {
            resolvedOrigin = origin
            displayOrigin = origin
        }

        db.podcasts.getByOrigin(displayOrigin)?.let {
            Logger.d(TAG, "Podcast already exists: ${it.title}")
            return AddPodcastResult.Duplicate(it)
        }

        Logger.d(TAG, "Fetching RSS feed from: $resolvedOrigin")
        val response = fetchPodcastClient.fetchNoCache(resolvedOrigin)
        if (response !is FetchPodcastClientResult.Success) {
            Logger.e(TAG, "Failed to fetch RSS feed: $response")
            throw Exception(response.toString())
        }

        val podcast = RssConverter.toPodcast(response.rssChannel, displayOrigin, response.fileSize, seedColor)
        val episodes = response.rssChannel.items.map { RssConverter.toPodcastEpisode(it, podcast) }
        Logger.i(TAG, "Parsed podcast: ${podcast.title}, ${episodes.size} episodes")

        return addPodcast(podcast, episodes, seedColor, false)
    }

    suspend fun addPodcastFromPreview(preview: PodcastPreviewModel, seedColor: Int?): AddPodcastResult {
        // Resolve iTunes lookup URLs to actual RSS feed URLs
        val origin = if (preview.fetchUrl.startsWith("itunes-lookup:")) {
            val idStr = preview.fetchUrl.removePrefix("itunes-lookup:")
            val id = idStr.toLongOrNull()
                ?: throw IllegalArgumentException("Invalid iTunes ID: $idStr")
            Logger.d(TAG, "Resolving iTunes ID: $id")
            val resolved = appleClient.lookup.lookupById(id)
                ?: throw Exception("iTunes lookup failed for ID: $id")
            Logger.d(TAG, "Resolved to RSS feed: ${resolved.fetchUrl}")
            resolved.fetchUrl
        } else {
            preview.fetchUrl
        }

        Logger.i(TAG, "addPodcastFromPreview: origin=$origin, title=${preview.title}")

        db.podcasts.getByOrigin(origin)?.let {
            Logger.d(TAG, "Podcast already exists: ${it.title}")
            return AddPodcastResult.Duplicate(it)
        }

        val podcast = Podcast(
            origin = origin,
            link = preview.link,
            title = preview.title,
            description = preview.description,
            author = preview.author,
            imageUrl = preview.imageUrl,
            imageSeedColor = seedColor ?: 0,
            languageCode = preview.languageCode,
            fileSize = 0
        )

        db.podcasts.insert(podcast)
        Logger.i(TAG, "Podcast saved from preview: ${podcast.title}")
        return AddPodcastResult.Created(podcast)
    }

    suspend fun addPodcast(podcast: Podcast, episodes: List<PodcastEpisode>, seedColor: Int?, duplicateCheck: Boolean = true): AddPodcastResult {
        if (duplicateCheck) db.podcasts.getByOrigin(podcast.origin)?.let { return AddPodcastResult.Duplicate(it) }

        Logger.d(TAG, "Inserting podcast into database: ${podcast.title}")
        db.podcasts.insert(podcast)
        episodes.forEach { db.episodes.insert(it) }
        episodes.forEach { db.playStates.initState(it.id) }
        Logger.i(TAG, "Podcast saved: ${podcast.title} (${episodes.size} episodes)")

        return AddPodcastResult.Created(podcast)
    }
}
