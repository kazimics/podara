package app.podiumpodcasts.podium.data

import app.podiumpodcasts.podium.data.model.Podcast
import app.podiumpodcasts.podium.data.model.PodcastEpisode
import app.podiumpodcasts.podium.data.model.PodcastEpisodeBundle
import app.podiumpodcasts.podium.data.model.PodcastEpisodeDownload
import app.podiumpodcasts.podium.data.model.PodcastEpisodePlayState
import app.podiumpodcasts.podium.data.model.PodcastHistory
import app.podiumpodcasts.podium.data.model.PodcastSubscription
import app.podiumpodcasts.podium.sqldelight.PodiumDatabase
import app.podiumpodcasts.podium.sqldelight.Podcast as PodcastEntity
import app.podiumpodcasts.podium.sqldelight.PodcastEpisode as PodcastEpisodeEntity
import app.podiumpodcasts.podium.sqldelight.PodcastEpisodePlayState as PodcastEpisodePlayStateEntity
import app.podiumpodcasts.podium.sqldelight.PodcastEpisodeDownload as PodcastEpisodeDownloadEntity
import app.podiumpodcasts.podium.sqldelight.PodcastSubscription as PodcastSubscriptionEntity
import app.podiumpodcasts.podium.sqldelight.List as ListEntity
import app.podiumpodcasts.podium.sqldelight.ListItem as ListItemEntity

fun PodcastEntity.toPodcast(): Podcast {
    return Podcast(
        origin = origin,
        link = link,
        title = title,
        description = description,
        author = author,
        imageUrl = imageUrl,
        imageSeedColor = imageSeedColor.toInt(),
        languageCode = languageCode,
        fileSize = fileSize,
        overrideTitle = overrideTitle,
        skipBeginning = skipBeginning.toInt(),
        skipEnding = skipEnding.toInt()
    )
}

fun PodcastEpisodeEntity.toPodcastEpisode(): PodcastEpisode {
    return PodcastEpisode(
        id = id,
        guid = guid,
        origin = origin,
        link = link,
        title = title,
        description = description,
        imageUrl = imageUrl,
        author = author,
        pubDate = pubDate,
        duration = duration.toInt(),
        audioUrl = audioUrl,
        podcastTitle = podcastTitle,
        imageSeedColor = imageSeedColor.toInt(),
        isNew = new == 1L
    )
}

fun PodcastEpisodePlayStateEntity.toPodcastEpisodePlayState(): PodcastEpisodePlayState {
    return PodcastEpisodePlayState(
        episodeId = episodeId,
        state = state.toInt(),
        played = played == 1L,
        lastUpdate = lastUpdate
    )
}

fun PodcastEpisodeDownloadEntity.toPodcastEpisodeDownload(): PodcastEpisodeDownload {
    return PodcastEpisodeDownload(
        episodeId = episodeId,
        state = state.toInt(),
        filename = filename,
        progress = progress,
        size = size,
        timestamp = timestamp
    )
}

fun PodcastSubscriptionEntity.toPodcastSubscription(): PodcastSubscription {
    return PodcastSubscription(
        origin = origin,
        enableNotifications = enableNotifications == 1L,
        enableAutoDownload = enableAutoDownload == 1L,
        lastUpdate = lastUpdate,
        newEpisodes = newEpisodes.toInt(),
        cacheETag = cacheETag,
        cacheLastModified = cacheLastModified,
        cacheContentLength = cacheContentLength
    )
}

fun ListEntity.toListModel(): app.podiumpodcasts.podium.data.model.ListModel {
    return app.podiumpodcasts.podium.data.model.ListModel(
        id = id.toInt(),
        name = name,
        description = description,
        itemCount = itemCount.toInt(),
        imageUrls = imageUrls,
        createdAt = createdAt,
        isSystemList = isSystemList == 1L
    )
}

fun ListItemEntity.toListItem(): app.podiumpodcasts.podium.data.model.ListItem {
    return app.podiumpodcasts.podium.data.model.ListItem(
        id = id.toInt(),
        listId = listId.toInt(),
        contentId = contentId,
        isPodcast = isPodcast == 1L,
        position = position.toInt()
    )
}
