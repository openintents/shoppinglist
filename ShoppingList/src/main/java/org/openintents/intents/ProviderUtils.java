package org.openintents.intents;

//Version Nov 21, 2008

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.text.TextUtils;

public class ProviderUtils {

    /**
     * Returns the row IDs of all affected rows.
     *
     * @param db
     * @param table
     * @param whereClause
     * @param whereArgs
     * @return
     */
    public static long[] getAffectedRows(SQLiteDatabase db, String table,
                                         String whereClause, String[] whereArgs) {
        if (TextUtils.isEmpty(whereClause)) {
            return null;
        }

        Cursor c = db.query(table, new String[]{BaseColumns._ID},
                whereClause, whereArgs, null, null, null);
        long[] affectedRows = null;
        if (c != null) {
            affectedRows = new long[c.getCount()];
            for (int i = 0; c.moveToNext(); i++) {
                affectedRows[i] = c.getLong(0);
            }
        }
        c.close();
        return affectedRows;
    }

}
