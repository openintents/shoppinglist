package org.openintents.shopping.test;

import android.app.Activity;
import android.content.Intent;
import android.test.InstrumentationTestCase;

import com.jayway.android.robotium.solo.Solo;

public class TestMain extends
// ActivityInstrumentationTestCase2<ShoppingActivity> {
		InstrumentationTestCase {
	private Solo solo;

	public TestMain() {
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

		Activity a = getInstrumentation().startActivitySync(i);

		this.solo = new Solo(getInstrumentation(), a);
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

	public void test001AddItem() {

		// Enter 10 in first editfield
		solo.enterText(0, "testitem");

		// Click on button
		solo.clickOnButton("Add");

		// Verify that item got added
		assertTrue(solo.searchText("testitem"));

	}

	public void test002DeleteItem() {
		solo.clickLongOnText("testitem");

		// Make sure context menu opened:
		assertTrue(solo.searchText("Edit item"));
		assertTrue(solo.searchText("Mark item"));

		solo.clickOnText("Delete item permanently");

		// Make sure user is asked before this
		assertTrue(solo.searchText("Are you sure"));

		solo.clickOnText("Delete");

		assertFalse(solo.searchText("testitem"));
	}
}
