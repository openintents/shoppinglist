package org.openintents.shopping;

import android.app.Activity;

import com.pollfish.constants.Position;
import com.pollfish.main.PollFish;

public class OptionalDependencies extends BaseOptionalDependencies {
    @Override
    public void onCreateShoppingListActivity(final Activity context) {
        PollFish.init(context, "something", Position.TOP_RIGHT, 0);
    }
}
