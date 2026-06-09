package org.openintents.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.Menu
import android.view.MenuItem

/**
 * Adds intent options with icons.
 *
 * This code is retrieved from this message:
 * http://groups.google.com/group/android-developers/browse_frm/thread/3fed25cdda765b02
 */
class MenuIntentOptionsWithIcons(
    private val mContext: Context,
    private val mMenu: Menu
) {

    fun addIntentOptions(
        group: Int,
        id: Int,
        categoryOrder: Int,
        caller: ComponentName?,
        specifics: Array<Intent>?,
        intent: Intent,
        flags: Int,
        outSpecificItems: Array<MenuItem?>?
    ): Int {
        val pm = mContext.packageManager
        val lri = pm.queryIntentActivityOptions(caller, specifics, intent, 0)
        val N = lri?.size ?: 0
        if ((flags and Menu.FLAG_APPEND_TO_GROUP) == 0) {
            mMenu.removeGroup(group)
        }
        for (i in 0 until N) {
            val ri = lri!![i]
            val rintent = Intent(
                if (ri.specificIndex < 0) intent else specifics!![ri.specificIndex]
            )
            rintent.component = ComponentName(
                ri.activityInfo.applicationInfo.packageName,
                ri.activityInfo.name
            )
            val item = mMenu
                .add(group, id, categoryOrder, ri.loadLabel(pm))
                .setIcon(ri.loadIcon(pm)).setIntent(rintent)
            if (outSpecificItems != null && ri.specificIndex >= 0) {
                outSpecificItems[ri.specificIndex] = item
            }
        }
        return N
    }
}
