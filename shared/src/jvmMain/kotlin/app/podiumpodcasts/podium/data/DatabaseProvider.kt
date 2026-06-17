package app.podiumpodcasts.podium.data

import app.podiumpodcasts.podium.sqldelight.PodiumDatabase
import java.io.File

object DatabaseProvider {
    private var database: PodiumDatabase? = null

    fun getDatabase(databaseDir: File): PodiumDatabase {
        return database ?: run {
            databaseDir.mkdirs()
            val databaseFile = File(databaseDir, "podium.db")
            val driver = createDatabaseDriver(databaseFile)
            PodiumDatabase(driver).also { database = it }
        }
    }

    fun close() {
        database?.close()
        database = null
    }
}
