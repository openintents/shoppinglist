package org.openintents.shopping.automation

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import org.openintents.shopping.library.provider.ShoppingContract
import org.openintents.shopping.library.provider.ShoppingContract.Contains
import org.openintents.shopping.library.provider.ShoppingContract.Status

object AutomationActions {

    @JvmStatic
    fun cleanUpList(context: Context, uri: Uri?) {
        if (uri != null) {
            val id = uri.lastPathSegment!!.toInt().toLong()

            // by changing state
            val values = ContentValues()
            values.put(Contains.STATUS, Status.REMOVED_FROM_LIST)
            context.contentResolver.update(
                Contains.CONTENT_URI,
                values,
                ShoppingContract.Contains.LIST_ID + " = " + id + " AND "
                        + ShoppingContract.Contains.STATUS + " = "
                        + ShoppingContract.Status.BOUGHT,
                null
            )
        }
    }
}
