package app.podiumpodcasts.podium.data.repository

import app.podiumpodcasts.podium.data.model.ListItem
import app.podiumpodcasts.podium.data.model.ListModel
import app.podiumpodcasts.podium.sqldelight.PodiumDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ListRepository(private val database: PodiumDatabase) {

    fun getAll(): Flow<List<ListModel>> {
        return database.listQueries.getAll()
            .asFlow()
            .map { query ->
                query.executeAsList().map { it.toListModel() }
            }
    }

    fun getById(id: Int): Flow<ListModel?> {
        return database.listQueries.getById(id.toLong())
            .asFlow()
            .map { it.executeAsOneOrNull()?.toListModel() }
    }

    suspend fun getByIdSync(id: Int): ListModel? {
        return database.listQueries.getById(id.toLong()).executeAsOneOrNull()?.toListModel()
    }

    suspend fun getByName(name: String): ListModel? {
        return database.listQueries.getByName(name).executeAsOneOrNull()?.toListModel()
    }

    suspend fun createFavorites() {
        database.listQueries.createFavorites(kotlin.system.TimeSource.Monotonic.markNow().toEpochMilliseconds())
    }

    suspend fun createHearLater() {
        database.listQueries.createHearLater(kotlin.system.TimeSource.Monotonic.markNow().toEpochMilliseconds())
    }

    suspend fun insert(name: String, description: String, imageUrls: String?) {
        database.listQueries.insert(
            name = name,
            description = description,
            imageUrls = imageUrls,
            createdAt = kotlin.system.TimeSource.Monotonic.markNow().toEpochMilliseconds()
        )
    }

    suspend fun update(id: Int, name: String, description: String) {
        database.listQueries.update(name, description, id.toLong())
    }

    suspend fun updateItemCount(id: Int, itemCount: Int) {
        database.listQueries.updateItemCount(itemCount.toLong(), id.toLong())
    }

    suspend fun delete(id: Int) {
        database.listQueries.delete(id.toLong())
    }

    // List Items
    fun getItemsByListId(listId: Int): Flow<List<ListItem>> {
        return database.listItemQueries.getByListId(listId.toLong())
            .asFlow()
            .map { query ->
                query.executeAsList().map {
                    ListItem(
                        id = it.id.toInt(),
                        listId = it.listId.toInt(),
                        contentId = it.contentId,
                        isPodcast = it.isPodcast == 1L,
                        position = it.position.toInt()
                    )
                }
            }
    }

    suspend fun getItemsByListIdSync(listId: Int): List<ListItem> {
        return database.listItemQueries.getByListId(listId.toLong()).executeAsList().map {
            ListItem(
                id = it.id.toInt(),
                listId = it.listId.toInt(),
                contentId = it.contentId,
                isPodcast = it.isPodcast == 1L,
                position = it.position.toInt()
            )
        }
    }

    suspend fun getListItem(listId: Int, contentId: String): ListItem? {
        return database.listItemQueries.getListItem(listId.toLong(), contentId).executeAsOneOrNull()?.let {
            ListItem(
                id = it.id.toInt(),
                listId = it.listId.toInt(),
                contentId = it.contentId,
                isPodcast = it.isPodcast == 1L,
                position = it.position.toInt()
            )
        }
    }

    suspend fun getListItemByContentId(contentId: String): ListItem? {
        return database.listItemQueries.getListItemByContentId(contentId).executeAsOneOrNull()?.let {
            ListItem(
                id = it.id.toInt(),
                listId = it.listId.toInt(),
                contentId = it.contentId,
                isPodcast = it.isPodcast == 1L,
                position = it.position.toInt()
            )
        }
    }

    suspend fun addListItem(listId: Int, contentId: String, isPodcast: Boolean, position: Int) {
        database.listItemQueries.insert(
            listId = listId.toLong(),
            contentId = contentId,
            isPodcast = if (isPodcast) 1L else 0L,
            position = position.toLong()
        )
    }

    suspend fun updatePosition(id: Int, position: Int) {
        database.listItemQueries.updatePosition(position.toLong(), id.toLong())
    }

    suspend fun removeListItem(listId: Int, contentId: String) {
        database.listItemQueries.removeByListIdAndContentId(listId.toLong(), contentId)
    }

    suspend fun removeListItemById(id: Int) {
        database.listItemQueries.removeById(id.toLong())
    }
}
