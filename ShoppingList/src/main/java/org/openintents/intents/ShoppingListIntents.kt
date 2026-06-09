package org.openintents.intents

/**
 * @author Peli
 * @version 1.2.4 (May 2010)
 */
object ShoppingListIntents {

    /**
     * String extra containing the action to be performed.
     *
     * Constant Value: "org.openintents.extra.ACTION"
     */
    const val EXTRA_ACTION = "org.openintents.extra.ACTION"

    /**
     * String extra containing the data on which to perform the action.
     *
     * Constant Value: "org.openintents.extra.DATA"
     */
    const val EXTRA_DATA = "org.openintents.extra.DATA"

    /**
     * Task to be used in EXTRA_ACTION.
     *
     * Constant Value: "org.openintents.shopping.task.clean_up_list"
     */
    const val TASK_CLEAN_UP_LIST = "org.openintents.shopping.task.clean_up_list"

    /**
     * Inserts shopping list items from a string array in intent extras.
     *
     * Constant Value: "org.openintents.type/string.arraylist.shopping"
     */
    const val TYPE_STRING_ARRAYLIST_SHOPPING = "org.openintents.type/string.arraylist.shopping"

    /**
     * Inserts shopping list items from a string array in intent extras.
     *
     * Constant Value: "org.openintents.extra.STRING_ARRAYLIST_SHOPPING"
     */
    const val EXTRA_STRING_ARRAYLIST_SHOPPING = "org.openintents.extra.STRING_ARRAYLIST_SHOPPING"

    /**
     * Intent extra for list of quantities corresponding to shopping list items
     * in STRING_ARRAYLIST_SHOPPING.
     *
     * Constant Value: "org.openintents.extra.STRING_ARRAYLIST_QUANTITY"
     */
    const val EXTRA_STRING_ARRAYLIST_QUANTITY = "org.openintents.extra.STRING_ARRAYLIST_QUANTITY"

    /**
     * Intent extra for list of prices corresponding to shopping list items in
     * STRING_ARRAYLIST_SHOPPING.
     *
     * Constant Value: "org.openintents.extra.STRING_ARRAYLIST_PRICE"
     */
    const val EXTRA_STRING_ARRAYLIST_PRICE = "org.openintents.extra.STRING_ARRAYLIST_PRICE"

    /**
     * Intent extra for list of barcodes corresponding to shopping list items in
     * STRING_ARRAYLIST_SHOPPING.
     *
     * Constant Value: "org.openintents.extra.STRING_ARRAYLIST_BARCODE"
     */
    const val EXTRA_STRING_ARRAYLIST_BARCODE = "org.openintents.extra.STRING_ARRAYLIST_BARCODE"
}
