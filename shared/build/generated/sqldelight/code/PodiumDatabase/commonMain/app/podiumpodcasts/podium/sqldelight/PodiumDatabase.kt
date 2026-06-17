package app.podiumpodcasts.podium.sqldelight

import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.podiumpodcasts.podium.sqldelight.shared.newInstance
import app.podiumpodcasts.podium.sqldelight.shared.schema
import kotlin.Unit

public interface PodiumDatabase : Transacter {
  public companion object {
    public val Schema: SqlSchema<QueryResult.Value<Unit>>
      get() = PodiumDatabase::class.schema

    public operator fun invoke(driver: SqlDriver): PodiumDatabase =
        PodiumDatabase::class.newInstance(driver)
  }
}
