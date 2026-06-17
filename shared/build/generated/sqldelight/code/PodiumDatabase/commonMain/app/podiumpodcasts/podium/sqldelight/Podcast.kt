package app.podiumpodcasts.podium.sqldelight

import kotlin.Long
import kotlin.String

public data class Podcast(
  public val origin: String,
  public val link: String,
  public val title: String,
  public val description: String,
  public val author: String,
  public val imageUrl: String,
  public val imageSeedColor: Long,
  public val languageCode: String,
  public val fileSize: Long,
  public val overrideTitle: String,
  public val skipBeginning: Long,
  public val skipEnding: Long,
)
