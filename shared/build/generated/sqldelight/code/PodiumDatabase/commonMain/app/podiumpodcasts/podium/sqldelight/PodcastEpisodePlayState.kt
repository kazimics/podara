package app.podiumpodcasts.podium.sqldelight

import kotlin.Long
import kotlin.String

public data class PodcastEpisodePlayState(
  public val episodeId: String,
  public val state: Long,
  public val played: Long,
  public val lastUpdate: Long,
)
