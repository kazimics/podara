package app.podiumpodcasts.podium.sqldelight

import kotlin.Long
import kotlin.String

public data class PodcastEpisodeDownload(
  public val episodeId: String,
  public val state: Long,
  public val filename: String?,
  public val progress: Long,
  public val size: Long,
  public val timestamp: Long,
)
