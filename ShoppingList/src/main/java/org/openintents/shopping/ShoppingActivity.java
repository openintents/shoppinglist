package org.openintents.shopping;

/**
 * The main activity prior to version 1.4 was ".ShoppingActivity". Home screens
 * may still contain a direct link to the old activity, therefore this class
 * must never be renamed or moved.
 * 
 * This class is derived from .ui.ShoppingActivity which contains the actual
 * implementation.
 * 
 * This solution is used instead of using an activity-alias in the Manifest,
 * because the activity-alias does not respect the
 * android:windowSoftInputMode="stateHidden|adjustResize" setting.
 */
public class ShoppingActivity extends
		org.openintents.shopping.ui.ShoppingActivity {

	/**
	 * For the implementation, see .ui.ShoppingActivity.
	 */
}
