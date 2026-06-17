package app.podiumpodcasts.podium.sqldelight

import kotlin.Long

public data class StatisticsUpdatePodcastRun(
  public val timestamp: Long,
  public val dataUsage: Long,
)
