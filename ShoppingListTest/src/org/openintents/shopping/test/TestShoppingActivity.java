package org.openintents.shopping.test;

import java.util.ArrayList;
import java.util.Random;

import org.openintents.intents.GeneralIntents;
import org.openintents.intents.ShoppingListIntents;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.test.InstrumentationTestCase;

import com.jayway.android.robotium.solo.Solo;

public class TestShoppingActivity extends
// ActivityInstrumentationTestCase2<ShoppingActivity> {
		InstrumentationTestCase {
	private Solo solo;
	private Activity activity;
	private Random random = new Random();
	
	public static String barcodescanneritem;

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
			this.solo.finalize();
		} catch (Throwable e) {
			e.printStackTrace();
		}
		// getActivity().finish();
		super.tearDown();
	}

	public void testUiAddItem() {
		String itemname = "testitem_add_" + random.nextInt(10000);
		
		// Enter item
		solo.enterText(0, itemname);

		// Click on button
		solo.clickOnButton("Add");

		// Verify that item got added
		assertTrue(solo.searchText(itemname));
	}
	
	public void testUiDeleteItemPermanently() {
		String itemname = "testitem_delete_" + random.nextInt(10000);
		solo.enterText(0, itemname);
		solo.clickOnButton("Add");
		assertTrue(solo.searchText(itemname));
		
		// Now delete the item
		solo.clickLongOnText(itemname);

		// Make sure context menu opened:
		assertTrue(solo.searchText("Edit item"));
		assertTrue(solo.searchText("Mark item"));

		solo.clickOnText("Delete item permanently");

		// Make sure user is asked before this
		assertTrue(solo.searchText("Are you sure"));

		solo.clickOnText("Delete");

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
		
		assertTrue(solo.searchText(itemname));
		
	}

	/**
	 * Test adding items from menu > Barcode scanner test.
	 */
	public void testIntentBarcodeScanner() {
		barcodescanneritem = "test_barcodescanner_" + random.nextInt(10000);
		
		// item does not exist yet:
		assertFalse(solo.searchText(barcodescanneritem));
		
		solo.clickOnMenuItem("Scan barcode test", true);
		
		// now item should exist:
		assertTrue(solo.searchText(barcodescanneritem));
	}
}
