package eu.kanade.tachiyomi.data.database

import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.pushtorefresh.storio.sqlite.impl.DefaultStorIOSQLite
import eu.kanade.tachiyomi.data.database.mappers.CategoryTypeMapping
import eu.kanade.tachiyomi.data.database.mappers.ChapterTypeMapping
import eu.kanade.tachiyomi.data.database.mappers.HistoryTypeMapping
import eu.kanade.tachiyomi.data.database.mappers.MangaCategoryTypeMapping
import eu.kanade.tachiyomi.data.database.mappers.MangaTypeMapping
import eu.kanade.tachiyomi.data.database.mappers.TrackTypeMapping
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.History
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.database.queries.CategoryQueries
import eu.kanade.tachiyomi.data.database.queries.ChapterQueries
import eu.kanade.tachiyomi.data.database.queries.HistoryQueries
import eu.kanade.tachiyomi.data.database.queries.MangaCategoryQueries
import eu.kanade.tachiyomi.data.database.queries.MangaQueries
import eu.kanade.tachiyomi.data.database.queries.TrackQueries
import exh.favorites.sql.mappers.FavoriteEntryTypeMapping
import exh.favorites.sql.models.FavoriteEntry
import exh.favorites.sql.queries.FavoriteEntryQueries
import exh.merged.sql.mappers.MergedMangaTypeMapping
import exh.merged.sql.models.MergedMangaReference
import exh.merged.sql.queries.MergedQueries
import exh.metadata.sql.mappers.SearchMetadataTypeMapping
import exh.metadata.sql.mappers.SearchTagTypeMapping
import exh.metadata.sql.mappers.SearchTitleTypeMapping
import exh.metadata.sql.models.SearchMetadata
import exh.metadata.sql.models.SearchTag
import exh.metadata.sql.models.SearchTitle
import exh.metadata.sql.queries.SearchMetadataQueries
import exh.metadata.sql.queries.SearchTagQueries
import exh.metadata.sql.queries.SearchTitleQueries
import exh.savedsearches.mappers.FeedSavedSearchTypeMapping
import exh.savedsearches.mappers.SavedSearchTypeMapping
import exh.savedsearches.models.FeedSavedSearch
import exh.savedsearches.models.SavedSearch

/**
 * This class provides operations to manage the database through its interfaces.
 */
class DatabaseHelper(
    openHelper: SupportSQLiteOpenHelper,
) :
    MangaQueries,
    ChapterQueries,
    TrackQueries,
    CategoryQueries,
    MangaCategoryQueries,
    HistoryQueries,
    /* SY --> */
    SearchMetadataQueries,
    SearchTagQueries,
    SearchTitleQueries,
    MergedQueries,
    FavoriteEntryQueries
/* SY <-- */ {

    override val db = DefaultStorIOSQLite.builder()
        .sqliteOpenHelper(openHelper)
        .addTypeMapping(Manga::class.java, MangaTypeMapping())
        .addTypeMapping(Chapter::class.java, ChapterTypeMapping())
        .addTypeMapping(Track::class.java, TrackTypeMapping())
        .addTypeMapping(Category::class.java, CategoryTypeMapping())
        .addTypeMapping(MangaCategory::class.java, MangaCategoryTypeMapping())
        .addTypeMapping(History::class.java, HistoryTypeMapping())
        // SY -->
        .addTypeMapping(SearchMetadata::class.java, SearchMetadataTypeMapping())
        .addTypeMapping(SearchTag::class.java, SearchTagTypeMapping())
        .addTypeMapping(SearchTitle::class.java, SearchTitleTypeMapping())
        .addTypeMapping(MergedMangaReference::class.java, MergedMangaTypeMapping())
        .addTypeMapping(FavoriteEntry::class.java, FavoriteEntryTypeMapping())
        .addTypeMapping(SavedSearch::class.java, SavedSearchTypeMapping())
        .addTypeMapping(FeedSavedSearch::class.java, FeedSavedSearchTypeMapping())
        // SY <--
        .build()

    inline fun inTransaction(block: () -> Unit) = db.inTransaction(block)

    // SY -->
    fun lowLevel() = db.lowLevel()
    // SY <--
}
