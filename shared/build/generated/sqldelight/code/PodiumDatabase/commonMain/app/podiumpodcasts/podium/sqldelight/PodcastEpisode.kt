package app.podiumpodcasts.podium.sqldelight

import kotlin.Long
import kotlin.String

public data class PodcastEpisode(
  public val id: String,
  public val guid: String,
  public val origin: String,
  public val link: String,
  public val title: String,
  public val description: String,
  public val imageUrl: String?,
  public val author: String,
  public val pubDate: Long,
  public val duration: Long,
  public val audioUrl: String,
  public val podcastTitle: String,
  public val imageSeedColor: Long,
  public val new: Long,
)
