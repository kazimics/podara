package app.podiumpodcasts.podium.data

import app.cash.sqldelight.db.SqlDriver
import java.io.File

expect fun createDatabaseDriver(databaseFile: File): SqlDriver
