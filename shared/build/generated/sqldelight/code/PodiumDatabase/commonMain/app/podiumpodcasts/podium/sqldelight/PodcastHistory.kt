package app.podiumpodcasts.podium.sqldelight

import kotlin.Long
import kotlin.String

public data class PodcastHistory(
  public val id: Long,
  public val origin: String,
  public val episodeId: String,
  public val timestamp: Long,
)
