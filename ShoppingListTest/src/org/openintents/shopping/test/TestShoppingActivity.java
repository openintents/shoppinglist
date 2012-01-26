package org.openintents.shopping.test;

import java.util.ArrayList;
import java.util.Random;

import org.openintents.intents.GeneralIntents;
import org.openintents.intents.ShoppingListIntents;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.test.InstrumentationTestCase;
import android.util.Log;

import com.jayway.android.robotium.solo.Solo;

public class TestShoppingActivity extends
// ActivityInstrumentationTestCase2<ShoppingActivity> {
		InstrumentationTestCase {
	
	private static final String TAG = "TestShoppingActivity";
	
	private Solo solo;
	private Activity activity;
	private Random random = new Random();
	
	public static final String barcodescanneritem = "test_barcodescanner_1234";

	public TestShoppingActivity() {
		super();
	}

	protected void setUp() throws Exception {
		super.setUp();

		// Unfortunately, extending ActivityInstrumentationTestCase2
		// does not work for ShoppingActivity, so we launch the activity
		// manually:
		Intent i = new Intent();
		i.setAction("android.intent.action.MAIN");
		i.setClassName("org.openintents.shopping",
				"org.openintents.shopping.ShoppingActivity");
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		activity = getInstrumentation().startActivitySync(i);

		this.solo = new Solo(getInstrumentation(), activity);
	}

	protected void tearDown() throws Exception {
		try {
			this.solo.finishOpenedActivities();
		} catch (Throwable e) {
			e.printStackTrace();
		}
		// getActivity().finish();
		super.tearDown();
	}

	private String getAppString(int resId) {
		return activity.getString(resId);
	}
	
	public void test000Eula() {
		String accept = getAppString(org.openintents.distribution.R.string.oi_distribution_eula_accept);
		String cancel = getAppString(org.openintents.distribution.R.string.oi_distribution_eula_refuse);
		boolean existsAccept = solo.searchButton(accept);
		boolean existsCancel = solo.searchButton(cancel);
		
		if (existsAccept && existsCancel) {
			solo.clickOnButton(accept);
		}
	}

	public void test001RecentChanges() {
		String recentChanges = getAppString(org.openintents.distribution.R.string.oi_distribution_newversion_recent_changes);
		String cont = getAppString(org.openintents.distribution.R.string.oi_distribution_newversion_continue);
		boolean existsRecentChanges = solo.searchText(recentChanges);
		boolean existsCont = solo.searchButton(cont);
		
		if (existsRecentChanges && existsCont) {
			solo.clickOnButton(cont);
		}
	}
	
	public void testUiAddItem() {
		String itemname = "testitem_add_" + random.nextInt(10000);
		String add = getAppString(org.openintents.shopping.R.string.add);

		// Enter item
		solo.enterText(0, itemname);

		// Click on button
		solo.clickOnButton(add);

		// Verify that item got added
		assertTrue(solo.searchText(itemname));
	}
	
	public void testUiDeleteItemPermanently() {
		String itemname = "testitem_delete_" + random.nextInt(10000);
		String add = getAppString(org.openintents.shopping.R.string.add);
		
		solo.enterText(0, itemname);
		solo.clickOnButton(add);
		assertTrue(solo.searchText(itemname));
		
		// Now delete the item
		solo.clickLongOnText(itemname);

		// Make sure context menu opened:
		assertTrue(solo.searchText(getAppString(org.openintents.shopping.R.string.menu_edit_item)));
		assertTrue(solo.searchText(getAppString(org.openintents.shopping.R.string.menu_mark_item)));

		solo.clickOnText(getAppString(org.openintents.shopping.R.string.menu_delete_item));

		// Make sure user is asked before this
		assertTrue(solo.searchText(getAppString(org.openintents.shopping.R.string.delete_item_confirm)));

		solo.clickOnText(getAppString(org.openintents.shopping.R.string.delete));

		assertFalse(solo.searchText(itemname));
	}
	
	/**
	 * Test adding items through intent.
	 */
	public void testIntentAddItemsFromArrayList() {
		String itemname = "testitem_intent_" + random.nextInt(10000);
		String resultData = ""; // no shopping list specified
		
		Intent resultIntent = new Intent(GeneralIntents.ACTION_INSERT_FROM_EXTRAS, Uri.parse(resultData));
		
		ArrayList<String> newEntry = new ArrayList<String>();
		newEntry.add(itemname);
		resultIntent.setType(ShoppingListIntents.TYPE_STRING_ARRAYLIST_SHOPPING);
		resultIntent.putStringArrayListExtra(ShoppingListIntents.EXTRA_STRING_ARRAYLIST_SHOPPING, newEntry);

		try {
			activity.startActivity(resultIntent);
		} catch (Exception e) {
			// Shopping list not installed - query for that.
		}
		
		// User should be presented with pick list activity
		solo.assertCurrentActivity("Expected ShoppingListsActivity activity", "ShoppingListsActivity");
		assertTrue(solo.searchText("My shopping list"));

		solo.clickOnText("My shopping list");
		
		// Back to Shopping List:
		solo.assertCurrentActivity("Expected ShoppingActivity activity", "ShoppingActivity");
		
		assertTrue(solo.searchText(itemname));
		
	}
	
	/**
	 * Test the add for barcode scanner setting
	 */
	public void testSettingAddForBarcode() {
		// Navigate to preferences
		solo.clickOnMenuItem("Settings");
		solo.assertCurrentActivity("Expected PreferenceActivity activity", "PreferenceActivity");
		solo.clickOnText("Advanced settings");
		
		// Uncheck the checkbox if it's not checked
		if (solo.isCheckBoxChecked(3) == true) {
			solo.clickOnText("Barcode Scanner");
		}
		
		solo.goBackToActivity("ShoppingActivity");
		
		// Test that scan barcode intent isn't called
		solo.clickLongOnText("Add");
		assertTrue(solo.searchText("Pick items"));
		assertFalse(solo.searchText("Complete action using"));
		solo.goBack();
		
		// Test that it doesn't appear on short click
		solo.enterText(0, "Barcode test");
		solo.clickOnText("Add");
		assertFalse(solo.searchText("Pick items"));
		assertFalse(solo.searchText("Complete action using"));

		// Navigate to preferences
		solo.clickOnMenuItem("Settings");
		solo.assertCurrentActivity("Expected PreferenceActivity activity", "PreferenceActivity");
		solo.clickOnText("Advanced settings");

		// Check the checkbox if it's not checked
		if (solo.isCheckBoxChecked(3) == false) {
			solo.clickOnText("Barcode Scanner");
		}

		solo.goBackToActivity("ShoppingActivity");
		
		// Test that it doesn't appear on short click
		solo.enterText(0, "Barcode test");
		solo.clickOnText("Add");
		assertFalse(solo.searchText("Pick items"));
		assertFalse(solo.searchText("Complete action using"));
		
		// Test that scan barcode intent is called
		solo.clickLongOnText("Add");
		/* NOTE: Robotium cannot test further */
		//assertFalse(solo.searchText("Pick items"));
		//assertTrue(solo.searchText("Complete action using"));
		//solo.goBack();
	}

	/**
	 * Test adding items from menu > Barcode scanner test.
	 */
	public void testIntentBarcodeScanner() {
		Log.d(TAG, "Name 1: " + barcodescanneritem);
		
		if (solo.searchText(barcodescanneritem)) {
			// Item exists already - delete it first:
			// Now delete the item
			solo.clickLongOnText(barcodescanneritem);
			assertTrue(solo.searchText("Delete item permanently"));
			solo.clickOnText("Delete item permanently");
			assertTrue(solo.searchText("Are you sure"));
			solo.clickOnText("Delete");
		}
		
		// item does not exist yet:
		assertFalse(solo.searchText(barcodescanneritem));
		
		solo.clickOnMenuItem("Scan Barcode", true);

		Log.d(TAG, "Name 2: " + barcodescanneritem);
		
		// now item should exist:
		assertTrue(solo.searchText(barcodescanneritem));
	}
}
