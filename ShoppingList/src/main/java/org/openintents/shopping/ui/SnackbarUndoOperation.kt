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
package org.openintents.shopping.ui

import android.content.Context
import android.view.View

/**
 * A simple holder class that stores the information to undo the application of a folder.
 */
open class SnackbarUndoOperation(
    protected val mCount: Int,
    protected val mType: Int,
    protected val mBatch: Boolean
) : View.OnClickListener {

    companion object {
        const val UNDO: Int = 0
        const val ERROR: Int = 1
    }

    open fun getType(): Int = mType

    open fun isBatchUndo(): Boolean = mBatch

    /**
     * Get a string description of the operation that will be performed
     * when the user taps the undo bar.
     */
    open fun getDescription(context: Context): String {
        val resId = -1
        return if (resId == -1) ""
        else String.format(context.resources.getQuantityString(resId, mCount), mCount)
    }

    open fun getSingularDescription(context: Context): String {
        val resId = -1
        return if (resId == -1) "" else context.getString(resId)
    }

    override fun onClick(view: View) {
    }
}
