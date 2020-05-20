package org.openintents.shopping.test.test;

import android.os.SystemClock;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.action.CoordinatesProvider;
import android.support.test.espresso.action.GeneralLocation;
import android.support.test.espresso.action.GeneralSwipeAction;
import android.support.test.espresso.action.Press;
import android.support.test.espresso.action.Swipe;
import android.support.test.espresso.action.Tap;
import android.support.test.espresso.action.ViewActions;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.KeyEvent;
import android.view.View;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openintents.shopping.R;
import org.openintents.shopping.ShoppingActivity;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.pressKey;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isRoot;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withHint;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.openintents.shopping.test.test.IsEqualTrimmingAndIgnoringCase.equalToTrimmingAndIgnoringCase;
import static org.openintents.shopping.test.test.VisibleViewMatcher.isVisible;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class EditItemTest {

  @Rule
  public ActivityTestRule<ShoppingActivity> mActivityTestRule =
      new ActivityTestRule<>(ShoppingActivity.class);

  @Test
  public void editItemTest() {
    ViewInteraction android_widget_ImageView =
        onView(allOf(withId(R.id.image_bottom), isVisible()));
    android_widget_ImageView.perform(getClickAction());

    ViewInteraction android_widget_ImageButton =
        onView(
            allOf(
                withContentDescription(equalToTrimmingAndIgnoringCase("Open navigation drawer")),
                isVisible(),
                isDescendantOfA(
                    allOf(
                        withId(R.id.action_bar),
                        isDescendantOfA(
                            allOf(
                                withId(R.id.action_bar_container),
                                isDescendantOfA(withId(R.id.decor_content_parent))))))));
    android_widget_ImageButton.perform(getClickAction());

    ViewInteraction android_widget_RelativeLayout =
        onView(
            allOf(
                classOrSuperClassesName(is("android.widget.RelativeLayout")),
                isVisible(),
                hasDescendant(withId(R.id.mode_radio_button)),
                hasDescendant(
                    allOf(
                        withId(R.id.text1),
                        withTextOrHint(equalToTrimmingAndIgnoringCase("New list")))),
                isDescendantOfA(
                    allOf(withId(R.id.left_drawer), isDescendantOfA(withId(R.id.drawer_layout))))));
    android_widget_RelativeLayout.perform(getClickAction());

    Espresso.pressBack();

    ViewInteraction android_widget_Button =
        onView(
            allOf(
                withId(R.id.button_add_item),
                withTextOrHint(equalToTrimmingAndIgnoringCase("ADD")),
                isVisible(),
                isDescendantOfA(
                    allOf(
                        withId(R.id.add_panel),
                        isDescendantOfA(
                            allOf(
                                withId(R.id.background),
                                isDescendantOfA(
                                    allOf(
                                        withId(R.id.content_frame),
                                        isDescendantOfA(withId(R.id.drawer_layout))))))))));
    android_widget_Button.perform(getClickAction());

    ViewInteraction android_widget_EditText =
        onView(
            allOf(
                withId(R.id.autocomplete_add_item),
                isVisible(),
                isDescendantOfA(
                    allOf(
                        withId(R.id.add_panel),
                        isDescendantOfA(
                            allOf(
                                withId(R.id.background),
                                isDescendantOfA(
                                    allOf(
                                        withId(R.id.content_frame),
                                        isDescendantOfA(withId(R.id.drawer_layout))))))))));
    android_widget_EditText.perform(replaceText("carbuncled"));

    ViewInteraction root = onView(isRoot());
    root.perform(getSwipeAction(540, 897, 540, 0));

    waitToScrollEnd();

    ViewInteraction root2 = onView(isRoot());
    root2.perform(getSwipeAction(540, 897, 540, 1794));

    waitToScrollEnd();

    ViewInteraction root3 = onView(isRoot());
    root3.perform(getSwipeAction(540, 897, 540, 1794));

    waitToScrollEnd();

    ViewInteraction android_widget_ImageButton2 =
        onView(
            allOf(
                withContentDescription(equalToTrimmingAndIgnoringCase("Open navigation drawer")),
                isVisible(),
                isDescendantOfA(
                    allOf(
                        withId(R.id.action_bar),
                        isDescendantOfA(
                            allOf(
                                withId(R.id.action_bar_container),
                                isDescendantOfA(withId(R.id.decor_content_parent))))))));
    android_widget_ImageButton2.perform(getClickAction());

    ViewInteraction android_widget_ImageView2 =
        onView(
            allOf(
                withContentDescription(equalToTrimmingAndIgnoringCase("More options")),
                isVisible(),
                isDescendantOfA(
                    allOf(
                        withId(R.id.action_bar),
                        isDescendantOfA(
                            allOf(
                                withId(R.id.action_bar_container),
                                isDescendantOfA(withId(R.id.decor_content_parent))))))));
    android_widget_ImageView2.perform(getLongClickAction());

    onView(isRoot()).perform(pressKey(KeyEvent.KEYCODE_ENTER));

    onView(isRoot()).perform(pressKey(KeyEvent.KEYCODE_ENTER));

    ViewInteraction android_widget_CheckBox =
        onView(
            allOf(
                withId(R.id.check),
                isVisible(),
                isDescendantOfA(
                    allOf(
                        withId(R.id.check_surround),
                        isDescendantOfA(
                            allOf(
                                withId(R.id.list_items),
                                isDescendantOfA(
                                    allOf(
                                        withId(R.id.background),
                                        isDescendantOfA(
                                            allOf(
                                                withId(R.id.content_frame),
                                                isDescendantOfA(
                                                    withId(R.id.drawer_layout))))))))))));
    android_widget_CheckBox.perform(getClickAction());

    ViewInteraction android_widget_ImageView3 =
        onView(
            allOf(
                withContentDescription(equalToTrimmingAndIgnoringCase("More options")),
                isVisible(),
                isDescendantOfA(
                    allOf(
                        withId(R.id.action_bar),
                        isDescendantOfA(
                            allOf(
                                withId(R.id.action_bar_container),
                                isDescendantOfA(withId(R.id.decor_content_parent))))))));
    android_widget_ImageView3.perform(getClickAction());

    ViewInteraction android_widget_LinearLayout =
        onView(
            allOf(
                classOrSuperClassesName(is("android.widget.LinearLayout")),
                isVisible(),
                hasDescendant(
                    allOf(
                        withId(R.id.content),
                        hasDescendant(
                            allOf(
                                withId(R.id.title),
                                withTextOrHint(equalToTrimmingAndIgnoringCase("Rename list"))))))));
    android_widget_LinearLayout.perform(getClickAction());

    ViewInteraction android_widget_EditText2 =
        onView(
            allOf(
                withId(R.id.edittext),
                withTextOrHint(equalToTrimmingAndIgnoringCase("My shopping list")),
                isVisible()));
    android_widget_EditText2.perform(replaceText("Zug"));

    ViewInteraction android_widget_EditText3 =
        onView(
            allOf(
                withId(R.id.edittext),
                withTextOrHint(equalToTrimmingAndIgnoringCase("Zug")),
                isVisible()));
    android_widget_EditText3.perform(replaceText("demurrages"));

    Espresso.pressBack();

    ViewInteraction android_widget_Button2 =
        onView(
            allOf(
                withId(R.id.button_add_item),
                withTextOrHint(equalToTrimmingAndIgnoringCase("ADD")),
                isVisible(),
                isDescendantOfA(
                    allOf(
                        withId(R.id.add_panel),
                        isDescendantOfA(
                            allOf(
                                withId(R.id.background),
                                isDescendantOfA(
                                    allOf(
                                        withId(R.id.content_frame),
                                        isDescendantOfA(withId(R.id.drawer_layout))))))))));
    android_widget_Button2.perform(getClickAction());

    ViewInteraction android_widget_CheckBox2 =
        onView(
            allOf(
                withId(R.id.check),
                isVisible(),
                isDescendantOfA(
                    allOf(
                        withId(R.id.check_surround),
                        isDescendantOfA(
                            allOf(
                                withId(R.id.list_items),
                                isDescendantOfA(
                                    allOf(
                                        withId(R.id.background),
                                        isDescendantOfA(
                                            allOf(
                                                withId(R.id.content_frame),
                                                isDescendantOfA(
                                                    withId(R.id.drawer_layout))))))))))));
    android_widget_CheckBox2.perform(getClickAction());

    ViewInteraction android_widget_RelativeLayout2 =
        onView(
            allOf(
                withId(R.id.description),
                isVisible(),
                hasDescendant(
                    allOf(
                        withId(R.id.name),
                        withTextOrHint(equalToTrimmingAndIgnoringCase("carbuncled")))),
                isDescendantOfA(
                    allOf(
                        withId(R.id.list_items),
                        isDescendantOfA(
                            allOf(
                                withId(R.id.background),
                                isDescendantOfA(
                                    allOf(
                                        withId(R.id.content_frame),
                                        isDescendantOfA(withId(R.id.drawer_layout))))))))));
    android_widget_RelativeLayout2.perform(getClickAction());

    ViewInteraction android_widget_ImageButton3 =
        onView(
            allOf(withId(R.id.note), isVisible(), isDescendantOfA(withId(R.id.priority_and_note))));
    android_widget_ImageButton3.perform(getClickAction());

    Espresso.pressBack();

    ViewInteraction android_widget_EditText4 =
        onView(
            allOf(
                withId(R.id.editpriority),
                isVisible(),
                isDescendantOfA(withId(R.id.priority_and_note))));
    android_widget_EditText4.perform(replaceText("semivegetable"));

    ViewInteraction android_widget_EditText5 =
        onView(
            allOf(
                withId(R.id.editunits),
                isVisible(),
                isDescendantOfA(withId(R.id.quantity_and_price))));
    android_widget_EditText5.perform(replaceText("antes Caddo"));

    ViewInteraction android_widget_MultiAutoCompleteTextView =
        onView(
            allOf(
                withId(R.id.edittags),
                withTextOrHint(equalToTrimmingAndIgnoringCase("Tags")),
                isVisible()));
    android_widget_MultiAutoCompleteTextView.perform(replaceText("prodigies"));

    ViewInteraction android_widget_EditText6 =
        onView(
            allOf(
                withId(R.id.editquantity),
                isVisible(),
                isDescendantOfA(withId(R.id.quantity_and_price))));
    android_widget_EditText6.perform(replaceText("161914267488695"));

    ViewInteraction android_widget_MultiAutoCompleteTextView2 =
        onView(
            allOf(
                withId(R.id.edittags),
                withTextOrHint(equalToTrimmingAndIgnoringCase("prodigies")),
                isVisible()));
    android_widget_MultiAutoCompleteTextView2.perform(replaceText("tachygraph"));

    ViewInteraction android_widget_EditText7 =
        onView(
            allOf(
                withId(R.id.editprice),
                withTextOrHint(equalToTrimmingAndIgnoringCase("0.00")),
                isVisible(),
                isDescendantOfA(withId(R.id.quantity_and_price))));
    android_widget_EditText7.perform(replaceText("641661799570594"));

    ViewInteraction android_widget_MultiAutoCompleteTextView3 =
        onView(
            allOf(
                withId(R.id.edittags),
                withTextOrHint(equalToTrimmingAndIgnoringCase("tachygraph")),
                isVisible()));
    android_widget_MultiAutoCompleteTextView3.perform(replaceText("underreport"));

    ViewInteraction android_widget_EditText8 =
        onView(
            allOf(
                withId(R.id.editunits),
                withTextOrHint(equalToTrimmingAndIgnoringCase("antes Caddo")),
                isVisible(),
                isDescendantOfA(withId(R.id.quantity_and_price))));
    android_widget_EditText8.perform(replaceText("vindicableness"));

    onView(isRoot()).perform(pressKey(KeyEvent.KEYCODE_ENTER));

    onView(isRoot()).perform(pressKey(KeyEvent.KEYCODE_ENTER));

    ViewInteraction android_widget_EditText9 =
        onView(
            allOf(
                withId(R.id.editprice),
                withTextOrHint(equalToTrimmingAndIgnoringCase("641661799570594")),
                isVisible(),
                isDescendantOfA(withId(R.id.quantity_and_price))));
    android_widget_EditText9.perform(replaceText("224392970304939"));

    ViewInteraction android_widget_EditText10 =
        onView(
            allOf(
                withId(R.id.editunits),
                withTextOrHint(equalToTrimmingAndIgnoringCase("vindicableness")),
                isVisible(),
                isDescendantOfA(withId(R.id.quantity_and_price))));
    android_widget_EditText10.perform(replaceText("Tifanie"));

    ViewInteraction android_widget_EditText11 =
        onView(
            allOf(
                withId(R.id.editunits),
                withTextOrHint(equalToTrimmingAndIgnoringCase("Tifanie")),
                isVisible(),
                isDescendantOfA(withId(R.id.quantity_and_price))));
    android_widget_EditText11.perform(replaceText("waterphone"));

    ViewInteraction android_widget_EditText12 =
        onView(
            allOf(
                withId(R.id.editprice),
                withTextOrHint(equalToTrimmingAndIgnoringCase("224392970304939")),
                isVisible(),
                isDescendantOfA(withId(R.id.quantity_and_price))));
    android_widget_EditText12.perform(replaceText("515788406023873"));

    ViewInteraction android_widget_EditText13 =
        onView(
            allOf(
                withId(R.id.editpriority),
                withTextOrHint(equalToTrimmingAndIgnoringCase("semivegetable")),
                isVisible(),
                isDescendantOfA(withId(R.id.priority_and_note))));
    android_widget_EditText13.perform(replaceText("Thin hopheads"));

    ViewInteraction android_widget_EditText14 =
        onView(
            allOf(
                withId(R.id.editprice),
                withTextOrHint(equalToTrimmingAndIgnoringCase("515788406023873")),
                isVisible(),
                isDescendantOfA(withId(R.id.quantity_and_price))));
    android_widget_EditText14.perform(replaceText("021322814911013"));

    ViewInteraction android_widget_EditText15 =
        onView(
            allOf(
                withId(R.id.editpriority),
                withTextOrHint(equalToTrimmingAndIgnoringCase("Thin hopheads")),
                isVisible(),
                isDescendantOfA(withId(R.id.priority_and_note))));
    android_widget_EditText15.perform(replaceText("form-relieve"));

    ViewInteraction android_widget_MultiAutoCompleteTextView4 =
        onView(
            allOf(
                withId(R.id.edittags),
                withTextOrHint(equalToTrimmingAndIgnoringCase("underreport")),
                isVisible()));
    android_widget_MultiAutoCompleteTextView4.perform(replaceText("nosewort"));

    ViewInteraction android_widget_EditText16 =
        onView(
            allOf(
                withId(R.id.editunits),
                withTextOrHint(equalToTrimmingAndIgnoringCase("waterphone")),
                isVisible(),
                isDescendantOfA(withId(R.id.quantity_and_price))));
    android_widget_EditText16.perform(replaceText("nark"));

    ViewInteraction android_widget_EditText17 =
        onView(
            allOf(
                withId(R.id.editquantity),
                withTextOrHint(equalToTrimmingAndIgnoringCase("161914267488695")),
                isVisible(),
                isDescendantOfA(withId(R.id.quantity_and_price))));
    android_widget_EditText17.perform(replaceText("493587808711719"));

    ViewInteraction android_widget_MultiAutoCompleteTextView5 =
        onView(
            allOf(
                withId(R.id.edittags),
                withTextOrHint(equalToTrimmingAndIgnoringCase("nosewort")),
                isVisible()));
    android_widget_MultiAutoCompleteTextView5.perform(replaceText("sincipita Butt"));

    onView(isRoot()).perform(pressKey(KeyEvent.KEYCODE_ENTER));

    ViewInteraction android_widget_EditText18 =
        onView(
            allOf(
                withId(R.id.edittext),
                withTextOrHint(equalToTrimmingAndIgnoringCase("carbuncled")),
                isVisible()));
    android_widget_EditText18.perform(replaceText("nonrealizable"));

    ViewInteraction android_widget_ImageButton4 =
        onView(
            allOf(withId(R.id.note), isVisible(), isDescendantOfA(withId(R.id.priority_and_note))));
    android_widget_ImageButton4.perform(getClickAction());

    Espresso.pressBack();

    onView(isRoot()).perform(pressKey(KeyEvent.KEYCODE_ENTER));
  }

  private static Matcher<View> classOrSuperClassesName(final Matcher<String> classNameMatcher) {

    return new TypeSafeMatcher<View>() {
      @Override
      public void describeTo(Description description) {
        description.appendText("Class name or any super class name ");
        classNameMatcher.describeTo(description);
      }

      @Override
      public boolean matchesSafely(View view) {
        Class<?> clazz = view.getClass();
        String canonicalName;

        do {
          canonicalName = clazz.getCanonicalName();
          if (canonicalName == null) {
            return false;
          }

          if (classNameMatcher.matches(canonicalName)) {
            return true;
          }

          clazz = clazz.getSuperclass();
          if (clazz == null) {
            return false;
          }
        } while (!"java.lang.Object".equals(canonicalName));

        return false;
      }
    };
  }

  private static Matcher<View> withTextOrHint(final Matcher<String> stringMatcher) {
    return anyOf(withText(stringMatcher), withHint(stringMatcher));
  }

  private ViewAction getSwipeAction(
      final int fromX, final int fromY, final int toX, final int toY) {
    return ViewActions.actionWithAssertions(
        new GeneralSwipeAction(
            Swipe.SLOW,
            new CoordinatesProvider() {
              @Override
              public float[] calculateCoordinates(View view) {
                float[] coordinates = {fromX, fromY};
                return coordinates;
              }
            },
            new CoordinatesProvider() {
              @Override
              public float[] calculateCoordinates(View view) {
                float[] coordinates = {toX, toY};
                return coordinates;
              }
            },
            Press.FINGER));
  }

  private void waitToScrollEnd() {
    SystemClock.sleep(500);
  }

  private ClickWithoutDisplayConstraint getClickAction() {
    return new ClickWithoutDisplayConstraint(
        Tap.SINGLE,
        GeneralLocation.VISIBLE_CENTER,
        Press.FINGER);
  }

  private ClickWithoutDisplayConstraint getLongClickAction() {
    return new ClickWithoutDisplayConstraint(
        Tap.LONG,
        GeneralLocation.CENTER,
        Press.FINGER);
  }
}
