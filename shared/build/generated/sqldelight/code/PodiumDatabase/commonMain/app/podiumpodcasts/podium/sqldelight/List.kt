package app.podiumpodcasts.podium.sqldelight

import kotlin.Long
import kotlin.String

public data class List(
  public val id: Long,
  public val name: String,
  public val description: String,
  public val itemCount: Long,
  public val imageUrls: String?,
  public val createdAt: Long,
  public val isSystemList: Long,
)
