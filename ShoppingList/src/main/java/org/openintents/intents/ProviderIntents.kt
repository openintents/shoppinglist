package org.openintents.intents

//Version Nov 21, 2008

/**
 * Provides OpenIntents actions, extras, and categories used by providers.
 *
 * These specifiers extend the standard Android specifiers.
 */
object ProviderIntents {

    /**
     * Broadcast Action: Sent after a new entry has been inserted.
     *
     * Constant Value: "org.openintents.action.INSERTED"
     */
    const val ACTION_INSERTED = "org.openintents.action.INSERTED"

    /**
     * Broadcast Action: Sent after an entry has been modified.
     *
     * Constant Value: "org.openintents.action.MODIFIED"
     */
    const val ACTION_MODIFIED = "org.openintents.action.MODIFIED"

    /**
     * Broadcast Action: Sent after an entry has been deleted.
     *
     * Constant Value: "org.openintents.action.DELETED"
     */
    const val ACTION_DELETED = "org.openintents.action.DELETED"

    /**
     * Added by the ACTION_DELETED broadcast if it contains a where clause.
     *
     * The extra contains a long[] which contains the row IDs of all rows
     * affected by the where clause. It contains NULL if all rows specified by
     * the URI are affected.
     *
     * Constant Value: "org.openintents.extra.AFFECTED_ROWS"
     */
    const val EXTRA_AFFECTED_ROWS = "org.openintents.extra.AFFECTED_ROWS"
}
