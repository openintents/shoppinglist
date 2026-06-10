/*
 * Copyright (C) 2007-2011 OpenIntents.org
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


import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

/**
 * @author Peli
 * @version 2011-02-07: Allow for string arguments.
 */
public class DownloadAppDialog extends AlertDialog implements OnClickListener {
    private static final String TAG = "DownloadAppDialog";

    Context mContext;
    String mDownloadAppName;
    String mDownloadPackageName;
    String mDownloadWebsite;
    String mMessageText;

    boolean mMarketAvailable;

    private boolean mHideMarketLink;

    public DownloadAppDialog(Context context) {
        super(context);
        mContext = context;
    }

    public DownloadAppDialog(Context context, int messageId, int downloadNameId, int downloadPackageId, int downloadWebsiteId) {
        super(context);
        mContext = context;
        set(messageId, downloadNameId, downloadPackageId, downloadWebsiteId);
    }

    public DownloadAppDialog(Context context, String message, String downloadName, String downloadPackage, String downloadWebsite) {
        super(context);
        mContext = context;
        set(message, downloadName, downloadPackage, downloadWebsite);
    }

    protected void set(int messageId, int downloadNameId,
                       int downloadPackageId, int downloadWebsiteId) {
        String message = mContext.getString(messageId);
        String downloadName = mContext.getString(downloadNameId);
        String downloadPackage = mContext.getString(downloadPackageId);
        String downloadWebsite = mContext.getString(downloadWebsiteId);
        set(message, downloadName, downloadPackage, downloadWebsite);
    }

    protected void set(String message, String downloadName,
                       String downloadPackage, String downloadWebsite) {
        mDownloadAppName = downloadName;
        mDownloadPackageName = downloadPackage;
        mDownloadWebsite = downloadWebsite;

        mMarketAvailable = org.openintents.distribution.MarketUtils.isMarketAvailable(mContext, mDownloadPackageName);
        mHideMarketLink = shouldHideMarketLink();

        StringBuilder sb = new StringBuilder();
        sb.append(message);
        sb.append(" ");
        if (mMarketAvailable && !mHideMarketLink) {
            sb.append(mContext.getString(R.string.oi_distribution_download_market_message,
                    mDownloadAppName));
        } else {
            sb.append(mContext.getString(R.string.oi_distribution_download_message,
                    mDownloadAppName));
        }
        mMessageText = sb.toString();
        setMessage(mMessageText);

        setTitle(mContext.getString(R.string.oi_distribution_download_title,
                mDownloadAppName));

        if (!mHideMarketLink) {
            setButton(BUTTON_POSITIVE, mContext.getText(R.string.oi_distribution_download_market), this);
        } else {
            getButton(BUTTON_POSITIVE).setVisibility(View.GONE);
        }
        setButton(BUTTON_NEGATIVE, mContext.getText(R.string.oi_distribution_download_web), this);
        setButton(BUTTON_NEUTRAL, mContext.getText(android.R.string.cancel), this);
    }

    protected boolean shouldHideMarketLink() {
        return MarketUtils.hideMarketLink(mContext);
    }

    public void onClick(DialogInterface dialog, int which) {
        Intent intent;

        if (which == BUTTON_POSITIVE) {
            intent = org.openintents.distribution.MarketUtils.getMarketDownloadIntent(mDownloadPackageName);
            startSaveActivity(intent);
        } else if (which == BUTTON_NEGATIVE) {
            intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.parse(mDownloadWebsite);
            intent.setData(uri);
            startSaveActivity(intent);
        }
    }

    public static void onPrepareDialog(Context context, Dialog dialog) {
        DownloadAppDialog d = (DownloadAppDialog) dialog;

        boolean hasAndroidMarket = org.openintents.distribution.MarketUtils.isMarketAvailable(context, d.mDownloadPackageName) && !d.mHideMarketLink;

        dialog.findViewById(android.R.id.button1).setVisibility(
                hasAndroidMarket ? View.VISIBLE : View.GONE);
    }

    /**
     * Start an activity but prompt a toast if activity is not found
     * (instead of crashing).
     *
     * @param intent
     */
    public void startSaveActivity(Intent intent) {
        try {
            mContext.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(mContext,
                    R.string.oi_distribution_update_error,
                    Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error starting second activity.", e);
        }
    }
}
