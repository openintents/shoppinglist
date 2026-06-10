/* 
 * Copyright (C) 2007-2016 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openintents.distribution;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import org.openintents.util.VersionUtils;

/**
 * Support dialog
 */
public class SupportDialog extends AlertDialog implements DialogInterface.OnClickListener {

    private static final String TAG = SupportDialog.class.getName();
    private final Context mContext;
    private final String mSupportUrl;

    public SupportDialog(Context context) {
        super(context);
        mContext = context;
        mSupportUrl = context.getString(R.string.oi_distribution_support_page);
        String version = VersionUtils.getVersionNumber(context);
        String appName = VersionUtils.getApplicationName(context);
        String appNameVersion = context.getString(R.string.oi_distribution_name_and_version, appName, version);

        SpannableStringBuilder sb = new SpannableStringBuilder();
        sb.append(appNameVersion);
        sb.append("\n\n");
        sb.append(context.getString(R.string.oi_distribution_visit_oi_support_page));
        setMessage(sb);
        setTitle(R.string.oi_distribution_support_dialog_title);
        setButton(BUTTON_POSITIVE, context.getString(R.string.oi_distribution_open_page), this);
        setButton(BUTTON_NEGATIVE, context.getString(R.string.oi_distribution_not_now), this);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        Intent intent;

        if (which == BUTTON_POSITIVE) {
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mSupportUrl));
            try {
                mContext.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(mContext,
                        R.string.oi_distribution_browser_error,
                        Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error starting second activity.", e);
            }
        } else if (which == BUTTON_NEGATIVE) {
            dialog.dismiss();
        }
    }
}
