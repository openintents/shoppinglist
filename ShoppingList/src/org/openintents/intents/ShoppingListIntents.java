package org.openintents.intents;

/**
 * 
 * @author Peli
 * @version 1.2.4 (May 2010)
 */
public class ShoppingListIntents {

	/**
	 * String extra containing the action to be performed.
	 * 
	 * <p>Constant Value: "org.openintents.extra.ACTION"</p>
	 */
	public static final String EXTRA_ACTION = "org.openintents.extra.ACTION";

	/**
	 * String extra containing the data on which to perform the action.
	 * 
	 * <p>Constant Value: "org.openintents.extra.DATA"</p>
	 */
	public static final String EXTRA_DATA = "org.openintents.extra.DATA";

	/**
	 * Task to be used in EXTRA_ACTION.
	 * 
	 * <p>Constant Value: "org.openintents.shopping.task.clean_up_list"</p>
	 */
	public static final String TASK_CLEAN_UP_LIST = "org.openintents.shopping.task.clean_up_list";
	
	/**
	 * Inserts shopping list items from a string array in intent extras.
	 * 
	 * <p>Constant Value: "org.openintents.type/string.arraylist.shopping"</p>
	 */
	public static final String TYPE_STRING_ARRAYLIST_SHOPPING = "org.openintents.type/string.arraylist.shopping";
	
	/**
	 * Inserts shopping list items from a string array in intent extras.
	 * 
	 * <p>Constant Value: "org.openintents.extra.STRING_ARRAYLIST_SHOPPING"</p>
	 */
	public static final String EXTRA_STRING_ARRAYLIST_SHOPPING = "org.openintents.extra.STRING_ARRAYLIST_SHOPPING";

	/**
	 * Intent extra for list of quantities corresponding to shopping list items in STRING_ARRAYLIST_SHOPPING.
	 * 
	 * <p>Constant Value: "org.openintents.extra.STRING_ARRAYLIST_QUANTITY"</p>
	 */
	public static final String EXTRA_STRING_ARRAYLIST_QUANTITY = "org.openintents.extra.STRING_ARRAYLIST_QUANTITY";
	
	/**
	 * Intent extra for list of prices corresponding to shopping list items in STRING_ARRAYLIST_SHOPPING.
	 * 
	 * <p>Constant Value: "org.openintents.extra.STRING_ARRAYLIST_PRICE"</p>
	 */
	public static final String EXTRA_STRING_ARRAYLIST_PRICE = "org.openintents.extra.STRING_ARRAYLIST_PRICE";
	
	/**
	 * Intent extra for list of barcodes corresponding to shopping list items in STRING_ARRAYLIST_SHOPPING.
	 * 
	 * <p>Constant Value: "org.openintents.extra.STRING_ARRAYLIST_BARCODE"</p>
	 */
	public static final String EXTRA_STRING_ARRAYLIST_BARCODE = "org.openintents.extra.STRING_ARRAYLIST_BARCODE";

	
}
