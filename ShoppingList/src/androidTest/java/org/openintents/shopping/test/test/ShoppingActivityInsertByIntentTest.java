package org.openintents.shopping.test.test;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.matcher.RootMatchers;
import android.support.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openintents.intents.GeneralIntents;
import org.openintents.intents.ShoppingListIntents;
import org.openintents.shopping.R;
import org.openintents.shopping.ShoppingActivity;
import org.openintents.shopping.ui.PreferenceActivity;
import org.openintents.util.VersionUtils;

import java.util.ArrayList;
import java.util.Random;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class ShoppingActivityInsertByIntentTest {

    private Random random = new Random();

    private String lastItemName;

    @Rule
    public ActivityTestRule rule = new ActivityTestRule<ShoppingActivity>(ShoppingActivity.class) {
        @Override
        protected Intent getActivityIntent() {
            String itemName = "testitem_intent_" + random.nextInt(10000);
            lastItemName = itemName;
            String resultData = ""; // no shopping list specified

            Intent insertIntent = new Intent(GeneralIntents.ACTION_INSERT_FROM_EXTRAS, Uri.parse(resultData));

            ArrayList<String> newEntry = new ArrayList<>();
            newEntry.add(itemName);
            insertIntent.setType(ShoppingListIntents.TYPE_STRING_ARRAYLIST_SHOPPING);
            insertIntent.putStringArrayListExtra(ShoppingListIntents.EXTRA_STRING_ARRAYLIST_SHOPPING, newEntry);
            return insertIntent;
        }
    };

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
    public void testIntentAddItemsFromArrayList() {
        onView(withId(R.id.autocomplete_add_item)).perform(typeText(lastItemName));
        onData(is(lastItemName))
                .inRoot(RootMatchers.withDecorView(not(is(rule.getActivity().getWindow().getDecorView()))))
                .check(matches(isDisplayed()));
    }
}
