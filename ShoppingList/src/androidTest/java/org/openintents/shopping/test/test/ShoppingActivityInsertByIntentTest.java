package org.openintents.shopping.test.test;

import android.content.Intent;
import android.net.Uri;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openintents.intents.GeneralIntents;
import org.openintents.intents.ShoppingListIntents;
import org.openintents.shopping.R;
import org.openintents.shopping.library.provider.ShoppingContract;
import org.openintents.shopping.ui.ShoppingActivity;

import java.util.ArrayList;
import java.util.Random;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;

@RunWith(AndroidJUnit4.class)
public class ShoppingActivityInsertByIntentTest {

    private Random random = new Random();

    private String lastItemName;

    @Rule
    ActivityTestRule rule = new ActivityTestRule<ShoppingActivity>(ShoppingActivity.class){
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


    @Test
    public void testIntentAddItemsFromArrayList() {

        onData(hasEntry(equalTo(ShoppingContract.Items.NAME), is(lastItemName))).check(matches(isDisplayed()));
    }
}
