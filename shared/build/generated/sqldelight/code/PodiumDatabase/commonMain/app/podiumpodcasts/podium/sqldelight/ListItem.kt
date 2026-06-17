package app.podiumpodcasts.podium.sqldelight

import kotlin.Long
import kotlin.String

public data class ListItem(
  public val id: Long,
  public val listId: Long,
  public val contentId: String,
  public val isPodcast: Long,
  public val position: Long,
)
