package org.openintents.intents

//Version Nov 21, 2008

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.provider.BaseColumns
import android.text.TextUtils

object ProviderUtils {

    /**
     * Returns the row IDs of all affected rows.
     *
     * @param db
     * @param table
     * @param whereClause
     * @param whereArgs
     * @return
     */
    @JvmStatic
    fun getAffectedRows(
        db: SQLiteDatabase,
        table: String,
        whereClause: String?,
        whereArgs: Array<String?>?
    ): LongArray? {
        if (TextUtils.isEmpty(whereClause)) {
            return null
        }

        val c: Cursor = db.query(
            table, arrayOf(BaseColumns._ID),
            whereClause, whereArgs, null, null, null
        )
        var affectedRows: LongArray? = null
        if (c != null) {
            affectedRows = LongArray(c.count)
            var i = 0
            while (c.moveToNext()) {
                affectedRows[i] = c.getLong(0)
                i++
            }
        }
        c.close()
        return affectedRows
    }
}
