package org.openintents.distribution;

import android.app.Activity;
import android.app.Dialog;
import android.view.Menu;
import android.view.MenuItem;

import org.openintents.distribution.about.About;

public class DistributionLibrary {

    public static final int OFFSET_ABOUT = 0;
    public static final int OFFSET_UPDATE = 1;
    public static final int OFFSET_SUPPORT = 2;
    public static final int OFFSET_PRIVACY = 3;

    /**
     * Number of menu IDs that should be reserved
     * for DistributionLibrary.
     */
    public static final int MENU_COUNT = 4;
    public static final int DIALOG_COUNT = MENU_COUNT;


    Activity mActivity;
    int mFirstMenuId = 0;
    int mFirstDialogId = 0;

    public DistributionLibrary(Activity activity, int firstMenuId, int firstDialogId) {
        mActivity = activity;
        mFirstMenuId = firstMenuId;
        mFirstDialogId = firstDialogId;
    }

    public void setFirst(int firstMenuId, int firstDialogId) {
        mFirstMenuId = firstMenuId;
        mFirstDialogId = firstDialogId;
    }

    /**
     * Typical usage:
     * Put this code in the beginning of onCreate().
     * <pre>
     * if (DistributionLibrary.showEulaOrNewVersion(this)) {
     * return;
     * }
     * </pre>
     * <p>
     * If one of the two activities is shown, they make
     * sure that the calling intent is called again afterwards.
     *
     * @return true if one of the dialogs is being shown.
     * In this case, onCreate() should be aborted by
     * returning.
     */
    public boolean showEulaOrNewVersion() {
        return EulaOrNewVersion.showEula(mActivity)
                || EulaOrNewVersion.showNewVersion(mActivity);
    }

    public void onCreateOptionsMenu(Menu menu) {
        // Remove items first so that they don't appear twice:
        menu.removeItem(mFirstMenuId + OFFSET_UPDATE);
        menu.removeItem(mFirstMenuId + OFFSET_ABOUT);
        menu.removeItem(mFirstMenuId + OFFSET_SUPPORT);
        menu.removeItem(mFirstMenuId + OFFSET_PRIVACY);

        if (UpdateDialog.isUpdateMenuNecessary(mActivity)) {
            menu.add(0, mFirstMenuId + OFFSET_UPDATE, 0, R.string.oi_distribution_menu_update).setIcon(
                    android.R.drawable.ic_menu_info_details).setShortcut('9', 'u');
        }
        menu.add(0, mFirstMenuId + OFFSET_ABOUT, 0, R.string.oi_distribution_about).setIcon(
                android.R.drawable.ic_menu_info_details).setShortcut('0', 'a');

        menu.add(0, mFirstMenuId + OFFSET_SUPPORT, 0, R.string.oi_distribution_support).setIcon(
                android.R.drawable.ic_menu_info_details).setAlphabeticShortcut('s');

        menu.add(0, mFirstMenuId + OFFSET_PRIVACY, 0, R.string.oi_distribution_privacy).setIcon(
                android.R.drawable.ic_menu_info_details).setAlphabeticShortcut('p');

    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id - mFirstMenuId) {
            case OFFSET_UPDATE:
                mActivity.showDialog(mFirstDialogId + OFFSET_UPDATE);
                return true;
            case OFFSET_ABOUT:
                About.showDialogOrStartActivity(mActivity,
                        mFirstDialogId + OFFSET_ABOUT);
                return true;
            case OFFSET_SUPPORT:
                mActivity.showDialog(mFirstDialogId + OFFSET_SUPPORT);
                return true;
            case OFFSET_PRIVACY:
                mActivity.showDialog(mFirstDialogId + OFFSET_PRIVACY);
                return true;
            default:
                break;
        }
        return false;
    }

    public Dialog onCreateDialog(int id) {
        switch (id - mFirstDialogId) {
            case OFFSET_UPDATE:
                return new UpdateDialog(mActivity);
            case OFFSET_SUPPORT:
                return new SupportDialog(mActivity);
            case OFFSET_PRIVACY:
                return new PrivacyDialog(mActivity);
            default:
                break;
        }
        return null;
    }

    public void onPrepareDialog(int id, Dialog dialog) {
        // e.g. hide items
    }
}
