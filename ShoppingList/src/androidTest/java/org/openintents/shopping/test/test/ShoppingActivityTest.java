package org.openintents.shopping.test.test;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.contrib.DrawerActions;
import android.support.test.rule.ActivityTestRule;
import android.support.v7.widget.ActionBarContainer;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Adapter;
import android.widget.AdapterView;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openintents.shopping.R;
import org.openintents.shopping.ShoppingActivity;
import org.openintents.shopping.library.provider.ShoppingContract;
import org.openintents.shopping.ui.PreferenceActivity;
import org.openintents.util.VersionUtils;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openContextualActionModeOverflowMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.CursorMatchers.withRowString;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isRoot;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class ShoppingActivityTest {

    public static final String BARCODE_SCANNER_ITEM = "test_barcodescanner_1234";
    private static final String TAG = ShoppingActivityTest.class.getSimpleName();
    @Rule
    public ActivityTestRule rule = new ActivityTestRule<>(ShoppingActivity.class);
    private Random random = new Random();

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

    /**
     * Perform action of waiting.
     */
    public static ViewAction waitFor(final long millis) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isRoot();
            }

            @Override
            public String getDescription() {
                return "wait during " + millis + " millis.";
            }

            @Override
            public void perform(final UiController uiController, final View view) {
                uiController.loopMainThreadForAtLeast(millis);
                uiController.loopMainThreadUntilIdle();
            }
        };
    }

    private String getAppString(int resId) {
        return rule.getActivity().getString(resId);
    }

    @Before
    public void setup() {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(rule.getActivity());
        SharedPreferences.Editor ed = prefs.edit();
        ed.putBoolean(PreferenceActivity.PREFS_HOLO_SEARCH, false);
        ed.putBoolean("eula_accepted", true);
        ed.putInt("org.openintents.distribution.version_number_check", VersionUtils.getVersionCode(InstrumentationRegistry.getTargetContext()));
        ed.commit();
    }

    @Test
    public void testUiAddItem() {
        String itemName = "testitem_add_" + random.nextInt(10000);

        addItem(itemName);

        onData(withRowString(equalTo(ShoppingContract.ContainsFull.ITEM_NAME), is(itemName)))
                .inAdapterView(withId(R.id.list_items))
                .check(matches(isDisplayed()));
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
        onView(allOf(withText(getAppString(R.string.menu_pick_items)),
                isDescendantOfA(isAssignableFrom(ActionBarContainer.class))))
                .check(matches(isDisplayed()));

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
        // "Scan barcode test" is defined in Manifest of this Test project
        String scan_barcode_test = "Scan barcode test";

        // Workaround: Open menu manually:
        clickOnMenuItem(scan_barcode_test);

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
        onView(withText(android.R.string.ok)).perform(click());

        addItem(movedItemName);

        longClickOnItem(movedItemName);
        onView(withText(R.string.menu_move_item)).perform(click());
        onData(withRowString(equalTo(ShoppingContract.Lists.NAME), is(getAppString(R.string.my_shopping_list)))).perform(click());

        // Make sure that the item was moved
        checkHasNotItem(movedItemName);

        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
        onView(withText(R.string.my_shopping_list)).perform(click());

        checkHasItem(movedItemName);
    }

    @Test
    public void testRenameItem() {
        int rndInt = random.nextInt(10000);
        String itemName = "not_rename" + rndInt;
        String newItemName = "rename" + rndInt;

        addItem(itemName);

        clickOnItem(itemName);

        // wait for dialog to show
        onView(isRoot()).perform(waitFor(TimeUnit.MILLISECONDS.toMillis(500)));

        onView(withId(R.id.edittext)).perform(replaceText(newItemName));
        onView(withText(android.R.string.ok)).perform(click());

        checkHasItem(newItemName);
    }

    private void addItem(String itemName) {
        onView(withId(R.id.autocomplete_add_item)).perform(typeText(itemName));
        onView(withId(R.id.button_add_item)).perform(click());
        hideSoftKeyboard();
    }

    private void hideSoftKeyboard() {
        Activity activity = rule.getActivity();
        View view = activity.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void longClickOnItem(String itemName) {
        onData(withRowString(equalTo(ShoppingContract.ContainsFull.ITEM_NAME), is(itemName)))
                .inAdapterView(withId(R.id.list_items))
                .onChildView(withText(itemName)).perform(longClick());
    }

    private void clickOnItem(String itemName) {
        onData(withRowString(equalTo(ShoppingContract.ContainsFull.ITEM_NAME), is(itemName)))
                .inAdapterView(withId(R.id.list_items)).onChildView(withText(itemName)).perform(click());
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

}
