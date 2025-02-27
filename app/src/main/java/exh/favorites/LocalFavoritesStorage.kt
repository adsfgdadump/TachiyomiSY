package exh.favorites

import eu.kanade.domain.manga.model.toDomainManga
import eu.kanade.tachiyomi.source.online.all.EHentai
import exh.metadata.metadata.EHentaiSearchMetadata
import exh.source.EXH_SOURCE_ID
import exh.source.isEhBasedManga
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.manga.interactor.DeleteFavoriteEntries
import tachiyomi.domain.manga.interactor.GetFavoriteEntries
import tachiyomi.domain.manga.interactor.GetFavorites
import tachiyomi.domain.manga.interactor.InsertFavoriteEntries
import tachiyomi.domain.manga.model.FavoriteEntry
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.injectLazy

class LocalFavoritesStorage {
    private val getFavorites: GetFavorites by injectLazy()
    private val getCategories: GetCategories by injectLazy()
    private val deleteFavoriteEntries: DeleteFavoriteEntries by injectLazy()
    private val getFavoriteEntries: GetFavoriteEntries by injectLazy()
    private val insertFavoriteEntries: InsertFavoriteEntries by injectLazy()

    suspend fun getChangedDbEntries() = getFavorites.await()
        .asFlow()
        .loadDbCategories()
        .parseToFavoriteEntries()
        .getChangedEntries()

    suspend fun getChangedRemoteEntries(entries: List<EHentai.ParsedManga>) = entries
        .asFlow()
        .map {
            it.fav to it.manga.toDomainManga(EXH_SOURCE_ID).copy(
                favorite = true,
                dateAdded = System.currentTimeMillis(),
            )
        }
        .parseToFavoriteEntries()
        .getChangedEntries()

    suspend fun snapshotEntries() {
        val dbMangas = getFavorites.await()
            .asFlow()
            .loadDbCategories()
            .parseToFavoriteEntries()

        // Delete old snapshot
        deleteFavoriteEntries.await()

        // Insert new snapshots
        insertFavoriteEntries.await(dbMangas.toList())
    }

    suspend fun clearSnapshots() {
        deleteFavoriteEntries.await()
    }

    private suspend fun Flow<FavoriteEntry>.getChangedEntries(): ChangeSet {
        val terminated = toList()

        val databaseEntries = getFavoriteEntries.await()

        val added = terminated.filter {
            queryListForEntry(databaseEntries, it) == null
        }

        val removed = databaseEntries
            .filter {
                queryListForEntry(terminated, it) == null
            } /*.map {
                todo see what this does
                realm.copyFromRealm(it)
            }*/

        return ChangeSet(added, removed)
    }

    private fun queryListForEntry(list: List<FavoriteEntry>, entry: FavoriteEntry) =
        list.find {
            it.gid == entry.gid &&
                it.token == entry.token &&
                it.category == entry.category
        }

    private suspend fun Flow<Manga>.loadDbCategories(): Flow<Pair<Int, Manga>> {
        val dbCategories = getCategories.await()
            .filterNot(Category::isSystemCategory)

        return filter(::validateDbManga).mapNotNull {
            val category = getCategories.await(it.id)

            dbCategories.indexOf(
                category.firstOrNull()
                    ?: return@mapNotNull null,
            ) to it
        }
    }

    private fun Flow<Pair<Int, Manga>>.parseToFavoriteEntries() =
        filter { (_, manga) ->
            validateDbManga(manga)
        }.mapNotNull { (categoryId, manga) ->
            FavoriteEntry(
                title = manga.ogTitle,
                gid = EHentaiSearchMetadata.galleryId(manga.url),
                token = EHentaiSearchMetadata.galleryToken(manga.url),
                category = categoryId,
            ).also {
                if (it.category > MAX_CATEGORIES) {
                    return@mapNotNull null
                }
            }
        }

    private fun validateDbManga(manga: Manga) =
        manga.favorite && manga.isEhBasedManga()

    companion object {
        const val MAX_CATEGORIES = 9
    }
}

data class ChangeSet(
    val added: List<FavoriteEntry>,
    val removed: List<FavoriteEntry>,
)
