package org.openintents.shopping.data

/**
 * The interaction mode for the list screen.
 *
 * - [SHOPPING]: the normal view — items currently on the list, check them off.
 * - [PICK_ITEMS]: shows every item that has been on this list (including ones
 *   removed/cleaned off), with a checkbox for "on the list". Tapping toggles an
 *   item between WANT_TO_BUY and REMOVED_FROM_LIST, so you can re-pick past items.
 */
enum class ListMode { SHOPPING, PICK_ITEMS }
