package eu.kanade.tachiyomi.data.database.queries

import com.pushtorefresh.storio.sqlite.queries.DeleteQuery
import com.pushtorefresh.storio.sqlite.queries.RawQuery
import eu.kanade.tachiyomi.data.database.DbProvider
import eu.kanade.tachiyomi.data.database.models.History
import eu.kanade.tachiyomi.data.database.resolvers.HistoryChapterIdPutResolver
import eu.kanade.tachiyomi.data.database.resolvers.HistoryUpsertResolver
import eu.kanade.tachiyomi.data.database.tables.HistoryTable

interface HistoryQueries : DbProvider {

    fun getHistoryByMangaId(mangaId: Long) = db.get()
        .listOfObjects(History::class.java)
        .withQuery(
            RawQuery.builder()
                .query(getHistoryByMangaId())
                .args(mangaId)
                .observesTables(HistoryTable.TABLE)
                .build(),
        )
        .prepare()

    fun getHistoryByChapterUrl(chapterUrl: String) = db.get()
        .`object`(History::class.java)
        .withQuery(
            RawQuery.builder()
                .query(getHistoryByChapterUrl())
                .args(chapterUrl)
                .observesTables(HistoryTable.TABLE)
                .build(),
        )
        .prepare()

    /**
     * Updates the history last read.
     * Inserts history object if not yet in database
     * @param historyList history object list
     */
    fun upsertHistoryLastRead(historyList: List<History>) = db.put()
        .objects(historyList)
        .withPutResolver(HistoryUpsertResolver())
        .prepare()

    // SY -->
    fun updateHistoryChapterIds(history: List<History>) = db.put()
        .objects(history)
        .withPutResolver(HistoryChapterIdPutResolver())
        .prepare()

    fun deleteHistoryIds(ids: List<Long>) = db.delete()
        .byQuery(
            DeleteQuery.builder()
                .table(HistoryTable.TABLE)
                .where("${HistoryTable.COL_ID} IN (?)")
                .whereArgs(ids.joinToString())
                .build(),
        )
        .prepare()
    // SY <--
}
