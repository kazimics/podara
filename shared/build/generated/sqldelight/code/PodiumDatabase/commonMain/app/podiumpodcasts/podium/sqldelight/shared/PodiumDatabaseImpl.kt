package app.podiumpodcasts.podium.sqldelight.shared

import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.podiumpodcasts.podium.sqldelight.PodiumDatabase
import kotlin.Long
import kotlin.Unit
import kotlin.reflect.KClass

internal val KClass<PodiumDatabase>.schema: SqlSchema<QueryResult.Value<Unit>>
  get() = PodiumDatabaseImpl.Schema

internal fun KClass<PodiumDatabase>.newInstance(driver: SqlDriver): PodiumDatabase =
    PodiumDatabaseImpl(driver)

private class PodiumDatabaseImpl(
  driver: SqlDriver,
) : TransacterImpl(driver),
    PodiumDatabase {
  public object Schema : SqlSchema<QueryResult.Value<Unit>> {
    override val version: Long
      get() = 1

    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
      driver.execute(null, """
          |CREATE TABLE IF NOT EXISTS list (
          |    id INTEGER PRIMARY KEY AUTOINCREMENT,
          |    name TEXT NOT NULL,
          |    description TEXT NOT NULL DEFAULT '',
          |    itemCount INTEGER NOT NULL DEFAULT 0,
          |    imageUrls TEXT,
          |    createdAt INTEGER NOT NULL,
          |    isSystemList INTEGER NOT NULL DEFAULT 0
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE IF NOT EXISTS listItem (
          |    id INTEGER PRIMARY KEY AUTOINCREMENT,
          |    listId INTEGER NOT NULL,
          |    contentId TEXT NOT NULL,
          |    isPodcast INTEGER NOT NULL,
          |    position INTEGER NOT NULL,
          |    FOREIGN KEY (listId) REFERENCES list(id) ON DELETE CASCADE
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE IF NOT EXISTS podcast (
          |    origin TEXT NOT NULL PRIMARY KEY,
          |    link TEXT NOT NULL,
          |    title TEXT NOT NULL,
          |    description TEXT NOT NULL,
          |    author TEXT NOT NULL,
          |    imageUrl TEXT NOT NULL,
          |    imageSeedColor INTEGER NOT NULL DEFAULT 0,
          |    languageCode TEXT NOT NULL,
          |    fileSize INTEGER NOT NULL DEFAULT 0,
          |    overrideTitle TEXT NOT NULL DEFAULT '',
          |    skipBeginning INTEGER NOT NULL DEFAULT 0,
          |    skipEnding INTEGER NOT NULL DEFAULT 0
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE IF NOT EXISTS podcastEpisode (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    guid TEXT NOT NULL,
          |    origin TEXT NOT NULL,
          |    link TEXT NOT NULL,
          |    title TEXT NOT NULL,
          |    description TEXT NOT NULL,
          |    imageUrl TEXT,
          |    author TEXT NOT NULL,
          |    pubDate INTEGER NOT NULL,
          |    duration INTEGER NOT NULL,
          |    audioUrl TEXT NOT NULL,
          |    podcastTitle TEXT NOT NULL,
          |    imageSeedColor INTEGER NOT NULL DEFAULT 0,
          |    new INTEGER NOT NULL DEFAULT 0,
          |    FOREIGN KEY (origin) REFERENCES podcast(origin) ON DELETE CASCADE
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE IF NOT EXISTS podcastEpisodePlayState (
          |    episodeId TEXT NOT NULL PRIMARY KEY,
          |    state INTEGER NOT NULL DEFAULT 0,
          |    played INTEGER NOT NULL DEFAULT 0,
          |    lastUpdate INTEGER NOT NULL DEFAULT 0,
          |    FOREIGN KEY (episodeId) REFERENCES podcastEpisode(id) ON DELETE CASCADE
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE IF NOT EXISTS podcastEpisodeDownload (
          |    episodeId TEXT NOT NULL PRIMARY KEY,
          |    state INTEGER NOT NULL DEFAULT 0,
          |    filename TEXT,
          |    progress INTEGER NOT NULL DEFAULT 0,
          |    size INTEGER NOT NULL DEFAULT 0,
          |    timestamp INTEGER NOT NULL DEFAULT 0
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE IF NOT EXISTS podcastHistory (
          |    id INTEGER PRIMARY KEY AUTOINCREMENT,
          |    origin TEXT NOT NULL,
          |    episodeId TEXT NOT NULL,
          |    timestamp INTEGER NOT NULL,
          |    FOREIGN KEY (episodeId) REFERENCES podcastEpisode(id) ON DELETE CASCADE
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE IF NOT EXISTS podcastSubscription (
          |    origin TEXT NOT NULL PRIMARY KEY,
          |    enableNotifications INTEGER NOT NULL DEFAULT 0,
          |    enableAutoDownload INTEGER NOT NULL DEFAULT 0,
          |    lastUpdate INTEGER NOT NULL DEFAULT 0,
          |    newEpisodes INTEGER NOT NULL DEFAULT 0,
          |    cacheETag TEXT NOT NULL DEFAULT '',
          |    cacheLastModified TEXT NOT NULL DEFAULT '',
          |    cacheContentLength TEXT NOT NULL DEFAULT '',
          |    FOREIGN KEY (origin) REFERENCES podcast(origin) ON DELETE CASCADE
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE IF NOT EXISTS syncAction (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    actionType TEXT NOT NULL,
          |    origin TEXT NOT NULL,
          |    audioUrl TEXT,
          |    position INTEGER,
          |    total INTEGER,
          |    timestamp INTEGER NOT NULL
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE IF NOT EXISTS statisticsUpdatePodcastRun (
          |    timestamp INTEGER NOT NULL PRIMARY KEY,
          |    dataUsage INTEGER NOT NULL
          |)
          """.trimMargin(), 0)
      driver.execute(null,
          "SELECT list.id, list.name, list.description, list.itemCount, list.imageUrls, list.createdAt, list.isSystemList FROM list ORDER BY isSystemList DESC, createdAt DESC",
          0)
      driver.execute(null,
          "SELECT list.id, list.name, list.description, list.itemCount, list.imageUrls, list.createdAt, list.isSystemList FROM list WHERE id = ?",
          0)
      driver.execute(null,
          "SELECT list.id, list.name, list.description, list.itemCount, list.imageUrls, list.createdAt, list.isSystemList FROM list WHERE name = ?",
          0)
      driver.execute(null,
          "INSERT OR IGNORE INTO list (name, description, itemCount, imageUrls, createdAt, isSystemList) VALUES (?, ?, ?, ?, ?, ?)",
          0)
      driver.execute(null, "UPDATE list SET name = ?, description = ? WHERE id = ?", 0)
      driver.execute(null, "UPDATE list SET itemCount = ? WHERE id = ?", 0)
      driver.execute(null, "DELETE FROM list WHERE id = ?", 0)
      driver.execute(null,
          "INSERT OR IGNORE INTO list (id, name, description, itemCount, createdAt, isSystemList) VALUES (-2, 'Favorites', '', 0, ?, 1)",
          0)
      driver.execute(null,
          "INSERT OR IGNORE INTO list (id, name, description, itemCount, createdAt, isSystemList) VALUES (-1, 'Hear Later', '', 0, ?, 1)",
          0)
      driver.execute(null,
          "SELECT listItem.id, listItem.listId, listItem.contentId, listItem.isPodcast, listItem.position FROM listItem WHERE listId = ? ORDER BY position ASC",
          0)
      driver.execute(null,
          "SELECT listItem.id, listItem.listId, listItem.contentId, listItem.isPodcast, listItem.position FROM listItem WHERE listId = ? AND contentId = ?",
          0)
      driver.execute(null,
          "SELECT listItem.id, listItem.listId, listItem.contentId, listItem.isPodcast, listItem.position FROM listItem WHERE contentId = ?",
          0)
      driver.execute(null,
          "INSERT OR IGNORE INTO listItem (listId, contentId, isPodcast, position) VALUES (?, ?, ?, ?)",
          0)
      driver.execute(null, "UPDATE listItem SET position = ? WHERE id = ?", 0)
      driver.execute(null, "DELETE FROM listItem WHERE listId = ? AND contentId = ?", 0)
      driver.execute(null, "DELETE FROM listItem WHERE id = ?", 0)
      driver.execute(null,
          "SELECT podcast.origin, podcast.link, podcast.title, podcast.description, podcast.author, podcast.imageUrl, podcast.imageSeedColor, podcast.languageCode, podcast.fileSize, podcast.overrideTitle, podcast.skipBeginning, podcast.skipEnding FROM podcast",
          0)
      driver.execute(null,
          "SELECT podcast.origin, podcast.link, podcast.title, podcast.description, podcast.author, podcast.imageUrl, podcast.imageSeedColor, podcast.languageCode, podcast.fileSize, podcast.overrideTitle, podcast.skipBeginning, podcast.skipEnding FROM podcast WHERE origin = ?",
          0)
      driver.execute(null, "SELECT origin FROM podcast", 0)
      driver.execute(null,
          "SELECT podcast.origin, podcast.link, podcast.title, podcast.description, podcast.author, podcast.imageUrl, podcast.imageSeedColor, podcast.languageCode, podcast.fileSize, podcast.overrideTitle, podcast.skipBeginning, podcast.skipEnding FROM podcast WHERE title LIKE '%' || ? || '%' OR description LIKE '%' || ? || '%' OR author LIKE '%' || ? || '%'",
          0)
      driver.execute(null, "UPDATE podcast SET fileSize = ? WHERE origin = ?", 0)
      driver.execute(null, "UPDATE podcast SET overrideTitle = ? WHERE origin = ?", 0)
      driver.execute(null, "UPDATE podcast SET skipBeginning = ? WHERE origin = ?", 0)
      driver.execute(null, "UPDATE podcast SET skipEnding = ? WHERE origin = ?", 0)
      driver.execute(null, "UPDATE podcast SET imageSeedColor = ? WHERE origin = ?", 0)
      driver.execute(null,
          "INSERT OR IGNORE INTO podcast (origin, link, title, description, author, imageUrl, imageSeedColor, languageCode, fileSize, overrideTitle, skipBeginning, skipEnding) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
          0)
      driver.execute(null, "DELETE FROM podcast WHERE origin = ?", 0)
      driver.execute(null, """
          |SELECT e.id, e.guid, e.origin, e.link, e.title, e.description, e.imageUrl, e.author, e.pubDate, e.duration, e.audioUrl, e.podcastTitle, e.imageSeedColor, e.new, p.state AS playState_state, p.played AS playState_played, p.lastUpdate AS playState_lastUpdate, d.state AS download_state, d.filename AS download_filename, d.progress AS download_progress, d.size AS download_size, d.timestamp AS download_timestamp
          |FROM podcastEpisode AS e
          |LEFT JOIN podcastEpisodePlayState AS p ON e.id = p.episodeId
          |LEFT JOIN podcastEpisodeDownload AS d ON e.id = d.episodeId
          |WHERE e.origin = ?
          |ORDER BY e.pubDate DESC
          """.trimMargin(), 0)
      driver.execute(null, """
          |SELECT e.id, e.guid, e.origin, e.link, e.title, e.description, e.imageUrl, e.author, e.pubDate, e.duration, e.audioUrl, e.podcastTitle, e.imageSeedColor, e.new, p.state AS playState_state, p.played AS playState_played, p.lastUpdate AS playState_lastUpdate, d.state AS download_state, d.filename AS download_filename, d.progress AS download_progress, d.size AS download_size, d.timestamp AS download_timestamp
          |FROM podcastEpisode AS e
          |LEFT JOIN podcastEpisodePlayState AS p ON e.id = p.episodeId
          |LEFT JOIN podcastEpisodeDownload AS d ON e.id = d.episodeId
          |WHERE e.id = ?
          """.trimMargin(), 0)
      driver.execute(null, """
          |SELECT e.id, e.guid, e.origin, e.link, e.title, e.description, e.imageUrl, e.author, e.pubDate, e.duration, e.audioUrl, e.podcastTitle, e.imageSeedColor, e.new, p.state AS playState_state, p.played AS playState_played, p.lastUpdate AS playState_lastUpdate, d.state AS download_state, d.filename AS download_filename, d.progress AS download_progress, d.size AS download_size, d.timestamp AS download_timestamp
          |FROM podcastEpisode AS e
          |LEFT JOIN podcastEpisodePlayState AS p ON e.id = p.episodeId
          |LEFT JOIN podcastEpisodeDownload AS d ON e.id = d.episodeId
          |WHERE e.new = 1
          |ORDER BY e.pubDate DESC
          """.trimMargin(), 0)
      driver.execute(null,
          "SELECT podcastEpisode.id, podcastEpisode.guid, podcastEpisode.origin, podcastEpisode.link, podcastEpisode.title, podcastEpisode.description, podcastEpisode.imageUrl, podcastEpisode.author, podcastEpisode.pubDate, podcastEpisode.duration, podcastEpisode.audioUrl, podcastEpisode.podcastTitle, podcastEpisode.imageSeedColor, podcastEpisode.new FROM podcastEpisode WHERE origin = ? AND audioUrl = ?",
          0)
      driver.execute(null, "SELECT id FROM podcastEpisode WHERE origin = ?", 0)
      driver.execute(null, "UPDATE podcastEpisode SET new = 1 WHERE id = ?", 0)
      driver.execute(null, "UPDATE podcastEpisode SET new = 0 WHERE id = ?", 0)
      driver.execute(null, "UPDATE podcastEpisode SET imageSeedColor = ? WHERE origin = ?", 0)
      driver.execute(null,
          "UPDATE podcastEpisode SET title = ?, description = ?, imageUrl = ?, author = ?, pubDate = ?, duration = ?, audioUrl = ?, podcastTitle = ?, imageSeedColor = ?, new = ? WHERE id = ?",
          0)
      driver.execute(null,
          "INSERT OR IGNORE INTO podcastEpisode (id, guid, origin, link, title, description, imageUrl, author, pubDate, duration, audioUrl, podcastTitle, imageSeedColor, new) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
          0)
      driver.execute(null, "DELETE FROM podcastEpisode WHERE id = ?", 0)
      driver.execute(null,
          "INSERT OR IGNORE INTO podcastEpisodePlayState (episodeId, state, played, lastUpdate) VALUES (?, 0, 0, 0)",
          0)
      driver.execute(null, "UPDATE podcastEpisodePlayState SET state = ? WHERE episodeId = ?", 0)
      driver.execute(null, "UPDATE podcastEpisodePlayState SET played = ? WHERE episodeId = ?", 0)
      driver.execute(null,
          "UPDATE podcastEpisodePlayState SET state = ?, played = ?, lastUpdate = ? WHERE episodeId = ?",
          0)
      driver.execute(null,
          "INSERT OR IGNORE INTO podcastEpisodeDownload (episodeId, state, filename, progress, size, timestamp) VALUES (?, 0, NULL, 0, 0, 0)",
          0)
      driver.execute(null,
          "UPDATE podcastEpisodeDownload SET state = ?, filename = ?, progress = ?, size = ?, timestamp = ? WHERE episodeId = ?",
          0)
      driver.execute(null,
          "UPDATE podcastEpisodeDownload SET state = ?, progress = ? WHERE episodeId = ?", 0)
      driver.execute(null, "DELETE FROM podcastEpisodeDownload WHERE episodeId = ?", 0)
      driver.execute(null,
          "SELECT podcastEpisodeDownload.episodeId, podcastEpisodeDownload.state, podcastEpisodeDownload.filename, podcastEpisodeDownload.progress, podcastEpisodeDownload.size, podcastEpisodeDownload.timestamp FROM podcastEpisodeDownload WHERE state != 2",
          0)
      driver.execute(null,
          "SELECT podcastHistory.id, podcastHistory.origin, podcastHistory.episodeId, podcastHistory.timestamp FROM podcastHistory ORDER BY timestamp DESC",
          0)
      driver.execute(null,
          "SELECT podcastHistory.id, podcastHistory.origin, podcastHistory.episodeId, podcastHistory.timestamp FROM podcastHistory ORDER BY timestamp DESC LIMIT ?",
          0)
      driver.execute(null,
          "SELECT podcastHistory.id, podcastHistory.origin, podcastHistory.episodeId, podcastHistory.timestamp FROM podcastHistory WHERE episodeId = ?",
          0)
      driver.execute(null,
          "SELECT podcastHistory.id, podcastHistory.origin, podcastHistory.episodeId, podcastHistory.timestamp FROM podcastHistory ORDER BY timestamp DESC LIMIT 1",
          0)
      driver.execute(null, "UPDATE podcastHistory SET timestamp = ? WHERE episodeId = ?", 0)
      driver.execute(null,
          "INSERT INTO podcastHistory (origin, episodeId, timestamp) VALUES (?, ?, ?)", 0)
      driver.execute(null, "DELETE FROM podcastHistory WHERE episodeId = ?", 0)
      driver.execute(null,
          "SELECT podcastSubscription.origin, podcastSubscription.enableNotifications, podcastSubscription.enableAutoDownload, podcastSubscription.lastUpdate, podcastSubscription.newEpisodes, podcastSubscription.cacheETag, podcastSubscription.cacheLastModified, podcastSubscription.cacheContentLength FROM podcastSubscription ORDER BY origin ASC",
          0)
      driver.execute(null,
          "SELECT podcastSubscription.origin, podcastSubscription.enableNotifications, podcastSubscription.enableAutoDownload, podcastSubscription.lastUpdate, podcastSubscription.newEpisodes, podcastSubscription.cacheETag, podcastSubscription.cacheLastModified, podcastSubscription.cacheContentLength FROM podcastSubscription WHERE origin = ?",
          0)
      driver.execute(null, "SELECT origin FROM podcastSubscription", 0)
      driver.execute(null,
          "SELECT podcastSubscription.origin, podcastSubscription.enableNotifications, podcastSubscription.enableAutoDownload, podcastSubscription.lastUpdate, podcastSubscription.newEpisodes, podcastSubscription.cacheETag, podcastSubscription.cacheLastModified, podcastSubscription.cacheContentLength FROM podcastSubscription WHERE enableNotifications = 1",
          0)
      driver.execute(null,
          "SELECT podcastSubscription.origin, podcastSubscription.enableNotifications, podcastSubscription.enableAutoDownload, podcastSubscription.lastUpdate, podcastSubscription.newEpisodes, podcastSubscription.cacheETag, podcastSubscription.cacheLastModified, podcastSubscription.cacheContentLength FROM podcastSubscription WHERE enableAutoDownload = 1 AND lastUpdate < ?",
          0)
      driver.execute(null, "UPDATE podcastSubscription SET lastUpdate = ? WHERE origin = ?", 0)
      driver.execute(null, "UPDATE podcastSubscription SET newEpisodes = ? WHERE origin = ?", 0)
      driver.execute(null,
          "UPDATE podcastSubscription SET enableNotifications = ? WHERE origin = ?", 0)
      driver.execute(null, "UPDATE podcastSubscription SET enableAutoDownload = ? WHERE origin = ?",
          0)
      driver.execute(null,
          "UPDATE podcastSubscription SET cacheETag = ?, cacheLastModified = ?, cacheContentLength = ? WHERE origin = ?",
          0)
      driver.execute(null,
          "INSERT OR IGNORE INTO podcastSubscription (origin, enableNotifications, enableAutoDownload, lastUpdate, newEpisodes, cacheETag, cacheLastModified, cacheContentLength) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
          0)
      driver.execute(null, "DELETE FROM podcastSubscription WHERE origin = ?", 0)
      driver.execute(null,
          "SELECT syncAction.id, syncAction.actionType, syncAction.origin, syncAction.audioUrl, syncAction.position, syncAction.total, syncAction.timestamp FROM syncAction ORDER BY timestamp ASC",
          0)
      driver.execute(null,
          "SELECT syncAction.id, syncAction.actionType, syncAction.origin, syncAction.audioUrl, syncAction.position, syncAction.total, syncAction.timestamp FROM syncAction WHERE id = ?",
          0)
      driver.execute(null,
          "INSERT OR IGNORE INTO syncAction (id, actionType, origin, audioUrl, position, total, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?)",
          0)
      driver.execute(null, "DELETE FROM syncAction WHERE id = ?", 0)
      driver.execute(null, "DELETE FROM syncAction", 0)
      driver.execute(null,
          "SELECT statisticsUpdatePodcastRun.timestamp, statisticsUpdatePodcastRun.dataUsage FROM statisticsUpdatePodcastRun ORDER BY timestamp DESC",
          0)
      driver.execute(null,
          "SELECT statisticsUpdatePodcastRun.timestamp, statisticsUpdatePodcastRun.dataUsage FROM statisticsUpdatePodcastRun WHERE timestamp > ?",
          0)
      driver.execute(null,
          "INSERT OR IGNORE INTO statisticsUpdatePodcastRun (timestamp, dataUsage) VALUES (?, ?)",
          0)
      driver.execute(null, "DELETE FROM statisticsUpdatePodcastRun WHERE timestamp < ?", 0)
      return QueryResult.Unit
    }

    override fun migrate(
      driver: SqlDriver,
      oldVersion: Long,
      newVersion: Long,
      vararg callbacks: AfterVersion,
    ): QueryResult.Value<Unit> = QueryResult.Unit
  }
}
