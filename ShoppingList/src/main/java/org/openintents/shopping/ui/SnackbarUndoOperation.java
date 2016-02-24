/*******************************************************************************
 *      borrowed from AOSP UnifiedEmail app
 *
 *      Copyright (C) 2011 Google Inc.
 *      Licensed to The Android Open Source Project.
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 *******************************************************************************/
package org.openintents.shopping.ui;

import android.content.Context;
import android.view.View;

/**
 * A simple holder class that stores the information to undo the application of a folder.
 */
public class SnackbarUndoOperation implements View.OnClickListener {
    public static final int UNDO = 0;
    public static final int ERROR = 1;
    protected final int mCount;
    protected final boolean mBatch;
    protected final int mType;

    /**
     * Create a SnackbarUndoOperation
     *
     * @param count  Number of conversations this action would be applied to.
     * @param type   type of action
     * @param batch  whether it is a batch operation
     */
    public SnackbarUndoOperation(int count, int type, boolean batch) {
        mCount = count;
        mBatch = batch;
        mType = type;
    }

    public int getType() {
        return mType;
    }

    public boolean isBatchUndo() {
        return mBatch;
    }

    /**
     * Get a string description of the operation that will be performed
     * when the user taps the undo bar.
     */
    public String getDescription(Context context) {
        final int resId = -1;

        final String desc = (resId == -1) ? "" :
                String.format(context.getResources().getQuantityString(resId, mCount), mCount);
        return desc;
    }

    public String getSingularDescription(Context context) {
        final int resId = -1;
        return (resId == -1) ? "" : context.getString(resId);
    }

    @Override
    public void onClick(View view) {

    }
}
