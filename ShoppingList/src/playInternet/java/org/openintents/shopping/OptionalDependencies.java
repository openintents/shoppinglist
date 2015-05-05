package org.openintents.shopping;

import android.app.Activity;

import com.pollfish.constants.Position;
import com.pollfish.main.PollFish;

public class OptionalDependencies extends BaseOptionalDependencies {
    @Override
    public void onResumeShoppingActivity(final Activity context) {
        PollFish.init(context, BuildConfig.KEY_POLLFISH, Position.MIDDLE_RIGHT, 0);
    }
}
