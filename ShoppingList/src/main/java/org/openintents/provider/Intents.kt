package org.openintents.provider

import android.net.Uri

abstract class Intents {

    companion object {
        @JvmField
        val CONTENT_URI: Uri = Uri.parse("openintents://intents")

        const val EXTRA_TYPE = "type"
        const val EXTRA_ACTION = "action"
        const val EXTRA_URI = "uri"

        /**
         * boolean extra flag indicating whether action list should include all
         * android actions.
         */
        const val EXTRA_ANDROID_ACTIONS = "androidActions"

        /**
         * string extra containing comma separated list of actions that should be
         * included in action list.
         */
        const val EXTRA_ACTION_LIST = "actionList"

        const val TYPE_PREFIX_DIR = "vnd.android.cursor.dir"
        const val TYPE_PREFIX_ITEM = "vnd.android.cursor.item"
    }
}
