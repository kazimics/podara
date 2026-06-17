package app.podiumpodcasts.podium.sqldelight

import kotlin.Long
import kotlin.String

public data class SyncAction(
  public val id: String,
  public val actionType: String,
  public val origin: String,
  public val audioUrl: String?,
  public val position: Long?,
  public val total: Long?,
  public val timestamp: Long,
)
