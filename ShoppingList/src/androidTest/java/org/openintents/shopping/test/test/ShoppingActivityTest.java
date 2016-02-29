package org.openintents.shopping.test.test;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.test.espresso.contrib.DrawerActions;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openintents.shopping.R;
import org.openintents.shopping.library.provider.ShoppingContract;
import org.openintents.shopping.ui.PreferenceActivity;
import org.openintents.shopping.ui.ShoppingActivity;

import java.util.Random;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openContextualActionModeOverflowMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@RunWith(AndroidJUnit4.class)
public class ShoppingActivityTest {

    private static final String TAG = ShoppingActivityTest.class.getSimpleName();

    @Rule
    ActivityTestRule rule = new ActivityTestRule<>(ShoppingActivity.class);
    private Random random = new Random();

    public static final String BARCODE_SCANNER_ITEM = "test_barcodescanner_1234";

    private String getAppString(int resId) {
        return rule.getActivity().getString(resId);
    }

    @Before
    public void setup() {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(rule.getActivity());
        SharedPreferences.Editor ed = prefs.edit();
        ed.putBoolean(PreferenceActivity.PREFS_HOLO_SEARCH, true);
        ed.commit();
    }

    @Test
    public void test000Eula() {
        onView(withText(org.openintents.distribution.R.string.oi_distribution_eula_refuse)).check(matches(isDisplayed()));
        onView(withText(org.openintents.distribution.R.string.oi_distribution_eula_accept)).check(matches(isDisplayed()));

        onView(withText(org.openintents.distribution.R.string.oi_distribution_eula_accept)).perform(click());
    }

    @Test
    public void test001RecentChanges() {
        onView(withText(org.openintents.distribution.R.string.oi_distribution_newversion_recent_changes)).check(matches(isDisplayed()));
        onView(withText(org.openintents.distribution.R.string.oi_distribution_newversion_continue)).check(matches(isDisplayed()));

        onView(withText(org.openintents.distribution.R.string.oi_distribution_newversion_continue)).perform(click());
    }

    @Test
    public void testUiAddItem() {
        String itemName = "testitem_add_" + random.nextInt(10000);

        addItem(itemName);

        onData(hasEntry(equalTo(ShoppingContract.Items.NAME), itemName)).check(matches(isDisplayed()));
    }

    @Test
    public void testUiDeleteItemPermanently() {
        String itemName = "testitem_delete_" + random.nextInt(10000);
        addItem(itemName);

        longClickOnItem(itemName);

        // click delete item
        onView(withText(R.string.menu_delete_item)).perform(click());
        // Make sure user is asked before this
        onView(withText(R.string.delete_item_confirm)).check(matches(isDisplayed()));
        // confirm
        onView(withText(R.string.delete)).perform(click());

        checkHasNotItem(itemName);
    }


    /**
     * Test the add for barcode scanner setting
     */
    @Test
    public void testSettingAddForBarcode() {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(rule.getActivity());
        SharedPreferences.Editor ed = prefs.edit();
        ed.putBoolean(PreferenceActivity.PREFS_ADDFORBARCODE, false);
        ed.apply();

        onView(withId(R.id.button_add_item)).perform(longClick());
        onView(withText(getAppString(R.string.pick_items_title))).check(matches(isDisplayed()));

        ed = prefs.edit();
        ed.putBoolean(PreferenceActivity.PREFS_ADDFORBARCODE, true);
        ed.apply();

        onView(withId(R.id.button_add_item)).perform(longClick());
        checkHasItem(BARCODE_SCANNER_ITEM);
    }

    /**
     * Test adding items from menu > Barcode scanner test.
     */
    @Test
    public void testIntentBarcodeScanner() {
        Log.d(TAG, "Name 1: " + BARCODE_SCANNER_ITEM);


        // "Scan barcode test" is defined in Manifest of this Test project
        String scan_barcode_test = "Scan barcode test";

        // The following does not always click on the correct menu item in Android 4.0:
        //solo.clickOnMenuItem(scan_barcode_test, true);

        // Workaround: Open menu manually:
        clickOnMenuItem(scan_barcode_test);

        Log.d(TAG, "Name 2: " + BARCODE_SCANNER_ITEM);

        // now item should exist:
        checkHasItem(BARCODE_SCANNER_ITEM);
    }

    @Test
    public void testMoveToAnotherList() {

        String newListName = "New Test List";
        String movedItemName = "testitem_move_" + random.nextInt(10000);

        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
        onView(withText(R.string.new_list)).perform(click());

        onView(withId(R.id.edittext)).perform(typeText(newListName));
        onView(withId(R.id.button_ok)).perform(click());

        addItem(movedItemName);

        longClickOnItem(movedItemName);
        onView(withText(R.string.menu_move_item)).perform(click());
        onData(hasEntry(equalTo(ShoppingContract.Lists.NAME), is(getAppString(R.string.my_shopping_list)))).perform(click());

        // Make sure that the item was moved
        checkHasNotItem(movedItemName);

        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
        onView(withText(newListName)).perform(click());

        checkHasItem(movedItemName);
    }

    @Test
    public void testRenameItem() {
        int rndInt = random.nextInt(10000);
        String itemName = "not_rename" + rndInt;
        String newItemName = "rename" + rndInt;

        addItem(itemName);

        clickOnItem(itemName);

        onView(withId(R.id.edittext)).perform(typeText(newItemName));
        onView(withText(R.id.button_ok)).perform(click());

        checkHasItem(newItemName);
    }


    private void addItem(String itemName) {
        onView(withId(R.id.autocomplete_add_item)).perform(typeText(itemName));
        onView(withId(R.id.button_add_item)).perform(click());
    }

    private void longClickOnItem(String itemName) {
        onData(hasEntry(equalTo(ShoppingContract.Items.NAME), is(itemName))).onChildView(withText(itemName)).perform(longClick());
    }

    private void clickOnItem(String itemName) {
        onData(hasEntry(equalTo(ShoppingContract.Items.NAME), is(itemName))).onChildView(withText(itemName)).perform(click());
    }

    private void checkHasItem(String itemName) {
        onData(hasEntry(equalTo(ShoppingContract.Items.NAME), is(itemName)));
    }


    private void checkHasNotItem(String itemName) {
        onView(withId(R.id.list_items)).check(matches(not(withAdaptedData(equalTo(itemName)))));
    }

    private void clickOnMenuItem(String text) {
        openContextualActionModeOverflowMenu();
        onView(withText(text)).perform(click());
    }

    private static Matcher<View> withAdaptedData(final Matcher<String> dataMatcher) {
        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("with class name: ");
                dataMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                if (!(view instanceof AdapterView)) {
                    return false;
                }
                @SuppressWarnings("rawtypes")
                Adapter adapter = ((AdapterView) view).getAdapter();
                for (int i = 0; i < adapter.getCount(); i++) {
                    if (dataMatcher.matches(adapter.getItem(i))) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

}
