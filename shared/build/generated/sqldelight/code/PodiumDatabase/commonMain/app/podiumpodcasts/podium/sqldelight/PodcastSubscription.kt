package app.podiumpodcasts.podium.sqldelight

import kotlin.Long
import kotlin.String

public data class PodcastSubscription(
  public val origin: String,
  public val enableNotifications: Long,
  public val enableAutoDownload: Long,
  public val lastUpdate: Long,
  public val newEpisodes: Long,
  public val cacheETag: String,
  public val cacheLastModified: String,
  public val cacheContentLength: String,
)
