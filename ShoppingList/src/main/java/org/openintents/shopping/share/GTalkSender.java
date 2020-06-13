/*
 * Copyright (C) 2007-2008 OpenIntents.org
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

/*
 * This code is based on Android's API demos.
 */

package org.openintents.shopping.share;

import android.content.Context;
import android.text.TextUtils;
import android.widget.Toast;

/**
 * Handles sending out information about changes in shared shopping lists.
 */
public class GTalkSender {
    /**
     * Suffix for bundle items to mark them old.
     * <p/>
     * For the update version, both, the old and the new value are sent. Old
     * values are marked with this suffix.
     */
    public static final String OLD = "_old";
    /**
     * Bundle marker for sender.
     */
    public static final String SENDER = "sender";

    // ??? IGTalkSession mGTalkSession = null;
    /**
     * Bundle marker for data (content URI).
     * <p/>
     * This is only necessary for the Anroid m5 issue that data is not sent
     * along with a GTalk message.
     */
    public static final String DATA = "data";
    private static final String TAG = "GTalkSender";
    private Context mContext;
    private boolean mBound;

    /**
     * Constructs a new sender GTalk. You have to manually bind before using
     * GTalk.
     *
     * @param mContext
     */
    public GTalkSender(Context context) {
        mContext = context;
        mBound = false;

        // bindGTalkService();
    }

    /**
     * Bind to GTalk service.
     */
    /*
     * public void bindGTalkService() { if (!mBound) { Intent intent = new
     * Intent(); intent.setComponent(
     * com.google.android.gtalkservice.GTalkServiceConstants
     * .GTALK_SERVICE_COMPONENT); mContext.bindService(intent, mConnection, 0);
     * mBound = true; } else { // already bound - do nothing. } }
     */

    /*
     * public void unbindGTalkService() { if (mBound) {
     * mContext.unbindService(mConnection); mBound = false; } else { // have not
     * been bound - do nothing. } }
     */
    /*
     * ??? private ServiceConnection mConnection = new ServiceConnection() {
     * public void onServiceConnected(ComponentName className, IBinder service)
     * { // This is called when the connection with the GTalkService has been //
     * established, giving us the service object we can use to // interact with
     * the service. We are communicating with our // service through an IDL
     * interface, so get a client-side // representation of that from the raw
     * service object. IGTalkService GTalkService =
     * IGTalkService.Stub.asInterface(service);
     *
     * try { mGTalkSession = GTalkService.getDefaultSession();
     *
     * if (mGTalkSession == null) { // this should not happen.
     * //showMessage(mContext.getText(R.string.gtalk_session_not_found));
     * showMessage(mContext.getText(R.string.gtalk_not_connected)); return; } }
     * catch (DeadObjectException ex) { Log.e(TAG, "caught " + ex);
     * showMessage(mContext.getText(R.string.gtalk_found_stale_service)); } }
     *
     * public void onServiceDisconnected(ComponentName className) { // This is
     * called when the connection with the service has been // unexpectedly
     * disconnected -- that is, its process crashed. mGTalkSession = null; } };
     */
    private boolean isValidUsername(String username) {
        if (TextUtils.isEmpty(username)) {
            return false;
        }

        return username.indexOf('@') != -1;

    }

    private void showMessage(CharSequence msg) {
        Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
    }

    // ////////////////////////////////////////////////////
    // Shopping related methods follow

    /**
     * Sends updated list and email information to all recipients.
     *
     * "local/[id]" refers to a local shopping list.
     */
    /*
     * public void sendList (String recipients, String shareListName) {
     * Log.i(TAG, "sendList(" + recipients + ", " + shareListName + ")");
     *
     * // First take out white spaces String r = recipients.replace(" ", ""); //
     * Then convert to list String[] recipientList = r.split(",");
     *
     * int max = recipientList.length; if (max == 1 &&
     * recipientList[0].equals("")) { // this is an empty list - nothing to
     * send: return; } for (int i = 0; i < max; i++) { String recipient =
     * recipientList[i];
     *
     * // Let us construct the recipient list without // the current recipient
     * StringBuilder modifiedRecipientList = new StringBuilder(); for (int j=0;
     * j < max; j++) { if (j != i) { // Note, we start with ',' but this will be
     * // prepended by the sending user data modifiedRecipientList.append(",");
     * modifiedRecipientList.append(recipientList[j]); } }
     *
     * if (recipient.startsWith("local/")) { Log.i(TAG, "local recipient: " +
     * recipient); // Recipient is local address
     *
     * // Prepend the modified recipient list by information // about the sender
     * modifiedRecipientList.insert(0, "local/" + shareListName);
     *
     * // If we have a local list, we map the shareListName to a // new name so
     * that we can synchronize two different local // lists. String
     * newShareListName = recipient.substring("local/".length());
     *
     * Intent intent = new Intent(OpenIntents.SHARE_UPDATE_ACTION,
     * Shopping.Lists.CONTENT_URI); intent.putExtra(Shopping.Lists.SHARE_NAME,
     * newShareListName); intent.putExtra(Shopping.Lists.SHARE_CONTACTS,
     * modifiedRecipientList.toString());
     *
     * mContext.broadcastIntent(intent);
     *
     * } else { Log.i(TAG, "remote recipient: " + recipient);
     *
     * // Recipient is remote address if (mGTalkSession != null) { try {
     *
     *
     *
     *
     * // Prepend the modified recipient list by information // about the sender
     * modifiedRecipientList.insert(0, mGTalkSession.getJid()); // or
     * getUsername() ?
     *
     * //Intent intent = new Intent(OpenIntents.SHARE_UPDATE_ACTION, //
     * Shopping.Lists.CONTENT_URI);
     *
     * // workaround for Anroid m5 issue: send content URI in bundle. Intent
     * intent = new Intent(OpenIntents.SHARE_UPDATE_ACTION);
     * intent.putExtra(DATA, Shopping.Lists.CONTENT_URI.toString());
     *
     * intent.putExtra(Shopping.Lists.SHARE_NAME, shareListName);
     * intent.putExtra(Shopping.Lists.SHARE_CONTACTS,
     * modifiedRecipientList.toString());
     *
     * mGTalkSession.sendDataMessage(recipient, intent); } catch
     * (DeadObjectException ex) { Log.e(TAG, "caught " + ex);
     * showMessage(mContext.getText(R.string.gtalk_found_stale_service));
     * mGTalkSession = null; bindGTalkService(); } } else {
     * //showMessage(mContext.getText(R.string.gtalk_service_not_connected));
     * showMessage(mContext.getText(R.string.gtalk_not_connected)); return; }
     *
     * } }
     *
     * }
     */
    /**
     * Sends information about a new item to all recipients.
     */
    /*
     * public void sendItem(String recipients, String shareListName, String
     * itemName) { Log.i(TAG, "sendItem(" + recipients + ", " + shareListName +
     * ", " + itemName + ")");
     *
     * // First take out white spaces String r = recipients.replace(" ", ""); //
     * Then convert to list String[] recipientList = r.split(",");
     *
     * int max = recipientList.length; if (max == 1 &&
     * recipientList[0].equals("")) { // this is an empty list - nothing to
     * send: return; } for (int i = 0; i < max; i++) { String recipient =
     * recipientList[i];
     *
     * if (recipient.startsWith("local/")) { Log.i(TAG, "local recipient: " +
     * recipient); // Recipient is local address
     *
     * // If we have a local list, we map the shareListName to a // new name so
     * that we can synchronize two different local // lists. String
     * newShareListName = recipient.substring("local/".length());
     *
     * //Intent intent = new Intent(OpenIntents.SHARE_INSERT_ACTION, //
     * Shopping.Items.CONTENT_URI);
     *
     * // workaround for Anroid m5 issue: send content URI in bundle. Intent
     * intent = new Intent(OpenIntents.SHARE_INSERT_ACTION);
     * intent.putExtra(DATA, Shopping.Items.CONTENT_URI.toString());
     * intent.putExtra(Shopping.Lists.SHARE_NAME, newShareListName);
     * intent.putExtra(Shopping.Items.NAME, itemName);
     *
     * mContext.broadcastIntent(intent);
     *
     * } else { Log.i(TAG, "remote recipient: " + recipient);
     *
     * // Recipient is remote address if (mGTalkSession != null) { try {
     * //Intent intent = new Intent(OpenIntents.SHARE_INSERT_ACTION, //
     * Shopping.Items.CONTENT_URI);
     *
     * // workaround for Anroid m5 issue: send content URI in bundle. Intent
     * intent = new Intent(OpenIntents.SHARE_INSERT_ACTION);
     * intent.putExtra(DATA, Shopping.Items.CONTENT_URI.toString());
     *
     * intent.putExtra(Shopping.Lists.SHARE_NAME, shareListName);
     * intent.putExtra(Shopping.Items.NAME, itemName);
     *
     * mGTalkSession.sendDataMessage(recipient, intent); } catch
     * (DeadObjectException ex) { Log.e(TAG, "caught " + ex);
     * showMessage(mContext.getText(R.string.gtalk_found_stale_service));
     * mGTalkSession = null; bindGTalkService(); } } else {
     * showMessage(mContext.getText(R.string.gtalk_service_not_connected));
     * return; }
     *
     * } }
     *
     *
     * }
     */
    /**
     * Sends information about a new item to all recipients.
     */
    /*
     * public void sendItemUpdate(String recipients, String shareListName,
     * String itemNameOld, String itemName, Long itemStatusOld, Long itemStatus)
     * { Log.i(TAG, "sendItemUpdate(" + recipients + ", " + shareListName + ", "
     * + itemNameOld + ", " + itemName + ", " + itemStatusOld + ", " +
     * itemStatus + ")");
     *
     * String itemSender = "";
     *
     *
     *
     * // First take out white spaces String r = recipients.replace(" ", ""); //
     * Then convert to list String[] recipientList = r.split(",");
     *
     * int max = recipientList.length; if (max == 1 &&
     * recipientList[0].equals("")) { // this is an empty list - nothing to
     * send: return; } for (int i = 0; i < max; i++) { String recipient =
     * recipientList[i];
     *
     * if (recipient.startsWith("local/")) { Log.i(TAG, "local recipient: " +
     * recipient); // Recipient is local address
     *
     * // If we have a local list, we map the shareListName to a // new name so
     * that we can synchronize two different local // lists. String
     * newShareListName = recipient.substring("local/".length());
     *
     * // The item sender's name will be just the unique list id: itemSender =
     * shareListName;
     *
     * Intent intent = new Intent(OpenIntents.SHARE_UPDATE_ACTION,
     * Shopping.Items.CONTENT_URI); intent.putExtra(Shopping.Lists.SHARE_NAME,
     * newShareListName); intent.putExtra(Shopping.Items.NAME + OLD,
     * itemNameOld); intent.putExtra(Shopping.Items.NAME, itemName); // TODO: In
     * m5, Android only supports Strings in bundles for GTalk
     * intent.putExtra(Shopping.Contains.STATUS + OLD, "" + itemStatusOld);
     * intent.putExtra(Shopping.Contains.STATUS, "" + itemStatus);
     * intent.putExtra(GTalkSender.SENDER, itemSender);
     *
     * mContext.broadcastIntent(intent);
     *
     * } else { Log.i(TAG, "remote recipient: " + recipient);
     *
     * // Recipient is remote address if (mGTalkSession != null) { try {
     * itemSender = mGTalkSession.getUsername();
     *
     * //Intent intent = new Intent(OpenIntents.SHARE_UPDATE_ACTION, //
     * Shopping.Items.CONTENT_URI);
     *
     * // workaround for Anroid m5 issue: send content URI in bundle. Intent
     * intent = new Intent(OpenIntents.SHARE_UPDATE_ACTION);
     * intent.putExtra(DATA, Shopping.Items.CONTENT_URI.toString());
     *
     * intent.putExtra(Shopping.Lists.SHARE_NAME, shareListName);
     * intent.putExtra(Shopping.Items.NAME + OLD, itemNameOld);
     * intent.putExtra(Shopping.Items.NAME, itemName); // TODO: In m5, Android
     * only supports Strings in bundles for GTalk
     * intent.putExtra(Shopping.Contains.STATUS + OLD, "" + itemStatusOld);
     * intent.putExtra(Shopping.Contains.STATUS, "" + itemStatus);
     * intent.putExtra(GTalkSender.SENDER, itemSender);
     *
     * mGTalkSession.sendDataMessage(recipient, intent); } catch
     * (DeadObjectException ex) { Log.e(TAG, "caught " + ex);
     * showMessage(mContext.getText(R.string.gtalk_found_stale_service));
     * mGTalkSession = null; bindGTalkService(); } } else { //
     * showMessage(mContext.getText(R.string.gtalk_service_not_connected));
     * showMessage(mContext.getText(R.string.gtalk_not_connected)); return; }
     *
     * } }
     *
     *
     * }
     */

}
