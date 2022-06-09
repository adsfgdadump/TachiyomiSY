package exh.savedsearches.mappers

import android.database.Cursor
import androidx.core.content.contentValuesOf
import androidx.core.database.getLongOrNull
import com.pushtorefresh.storio.sqlite.SQLiteTypeMapping
import com.pushtorefresh.storio.sqlite.operations.delete.DefaultDeleteResolver
import com.pushtorefresh.storio.sqlite.operations.get.DefaultGetResolver
import com.pushtorefresh.storio.sqlite.operations.put.DefaultPutResolver
import com.pushtorefresh.storio.sqlite.queries.DeleteQuery
import com.pushtorefresh.storio.sqlite.queries.InsertQuery
import com.pushtorefresh.storio.sqlite.queries.UpdateQuery
import exh.savedsearches.mappers.FeedSavedSearchTable.COL_GLOBAL
import exh.savedsearches.mappers.FeedSavedSearchTable.COL_ID
import exh.savedsearches.mappers.FeedSavedSearchTable.COL_SAVED_SEARCH_ID
import exh.savedsearches.mappers.FeedSavedSearchTable.COL_SOURCE
import exh.savedsearches.mappers.FeedSavedSearchTable.TABLE
import exh.savedsearches.models.FeedSavedSearch

private object FeedSavedSearchTable {

    const val TABLE = "feed_saved_search"

    const val COL_ID = "_id"

    const val COL_SOURCE = "source"

    const val COL_SAVED_SEARCH_ID = "saved_search"

    const val COL_GLOBAL = "global"
}

class FeedSavedSearchTypeMapping : SQLiteTypeMapping<FeedSavedSearch>(
    FeedSavedSearchPutResolver(),
    FeedSavedSearchGetResolver(),
    FeedSavedSearchDeleteResolver(),
)

class FeedSavedSearchPutResolver : DefaultPutResolver<FeedSavedSearch>() {

    override fun mapToInsertQuery(obj: FeedSavedSearch) = InsertQuery.builder()
        .table(TABLE)
        .build()

    override fun mapToUpdateQuery(obj: FeedSavedSearch) = UpdateQuery.builder()
        .table(TABLE)
        .where("$COL_ID = ?")
        .whereArgs(obj.id)
        .build()

    override fun mapToContentValues(obj: FeedSavedSearch) = contentValuesOf(
        COL_ID to obj.id,
        COL_SOURCE to obj.source,
        COL_SAVED_SEARCH_ID to obj.savedSearch,
        COL_GLOBAL to obj.global,
    )
}

class FeedSavedSearchGetResolver : DefaultGetResolver<FeedSavedSearch>() {

    override fun mapFromCursor(cursor: Cursor): FeedSavedSearch = FeedSavedSearch(
        id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
        source = cursor.getLong(cursor.getColumnIndexOrThrow(COL_SOURCE)),
        savedSearch = cursor.getLongOrNull(cursor.getColumnIndexOrThrow(COL_SAVED_SEARCH_ID)),
        global = cursor.getInt(cursor.getColumnIndexOrThrow(COL_GLOBAL)) == 1,
    )
}

class FeedSavedSearchDeleteResolver : DefaultDeleteResolver<FeedSavedSearch>() {

    override fun mapToDeleteQuery(obj: FeedSavedSearch) = DeleteQuery.builder()
        .table(TABLE)
        .where("$COL_ID = ?")
        .whereArgs(obj.id)
        .build()
}
