package org.openintents.distribution.about;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

public class AboutUtils {
    private static final String TAG = "AboutUtils";

    public static String getTextFromArray(final String[] array) {
        if (array == null) {
            return "";
        }
        String text = "";
        for (String person : array) {
            text += person + "\n";
        }
        if (text.length() > 0) {
            // delete last "\n"
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }

    /**
     * Get String array from Extra or from Meta-data through resources.
     *
     * @param packagename
     * @param intent
     * @param extra
     * @param metadata
     */
    public static String[] getStringArrayExtraOrMetadata(final MetaDataReader metaDataReader,
                                                         final Context context, final String packagename, final Intent intent, final String extra,
                                                         final String metadata) {
        if (intent.hasExtra(extra)
                && intent.getStringArrayExtra(extra)
                != null) {
            return intent.getStringArrayExtra(extra);
        } else {
            //Try meta data of package
            Bundle md = metaDataReader.getBundle();

            if (md != null) {
                String[] array = null;
                try {
                    int id = md.getInt(metadata);
                    if (id != 0) {
                        Resources resources = context.getPackageManager()
                                .getResourcesForApplication(packagename);
                        array = resources.getStringArray(id);
                    }
                } catch (NameNotFoundException e) {
                    Log.e(TAG, "Package name not found ", e);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Metadata not valid id.", e);
                } catch (Resources.NotFoundException e) {
                    Log.e(TAG, "Resource not found.", e);
                }

                return array;
            } else {
                return null;
            }
        }
    }

    /**
     * Get string from extra or from metadata.
     *
     * @param context
     * @param packagename
     * @param intent
     * @param extra
     * @param metadata
     * @return
     */
    public static String getStringExtraOrMetadata(final MetaDataReader metaDataReader, final Context context,
                                                  final String packagename, final Intent intent, final String extra, final String metadata) {
        if (intent.hasExtra(extra)
                && intent.getStringExtra(extra) != null) {
            return intent.getStringExtra(extra);
        } else {
            //Try meta data of package
            Bundle md = metaDataReader.getBundle();

            if (md != null) {
                Object value = md.get(metadata);
                if (value instanceof String && !TextUtils.isEmpty((String) value)) {
                    return (String) value;
                } else {
                    //Still try metadata but don't expect a ready string (get it from the resources).
                    try {
                        int id = md.getInt(metadata);
                        if (id != 0) {
                            Resources resources = context.getPackageManager()
                                    .getResourcesForApplication(packagename);
                            String text = resources.getString(id);
                            if (!TextUtils.isEmpty(text)) {
                                return text;
                            }
                        }
                    } catch (NameNotFoundException e) {
                        Log.e(TAG, "Package name not found ", e);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Metadata not valid id.", e);
                    } catch (Resources.NotFoundException e) {
                        Log.e(TAG, "Resource not found.", e);
                    }
                }
            }
            return "";
        }
    }


    /**
     * Get string from metadata in different localization.
     *
     * @param metaDataReader
     * @param metadata
     * @return
     */
    public static int getResourceIdMetadata(final MetaDataReader metaDataReader, final String metadata) {
        //Try meta data of package
        Bundle md = metaDataReader.getBundle();

        if (md != null) {
            //Still try metadata but don't expect a ready string (get it from the resources).
            try {
                return md.getInt(metadata);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Metadata not valid id. Using 0 instead", e);
            } catch (Resources.NotFoundException e) {
                Log.e(TAG, "Resource not found. Using 0 instead", e);
            }
        }
        return 0;
    }

    /**
     * Get resource ID from extra or from metadata.
     *
     * @param context
     * @param packagename
     * @param intent
     * @param extra
     * @param metadata
     * @return
     */
    public static int getResourceIdExtraOrMetadata(final MetaDataReader metaDataReader, final Context context,
                                                   final String packagename, final Intent intent, final String extra, final String metadata) {
        if (intent.hasExtra(extra)
                && intent.getStringExtra(extra) != null) {

            int id = 0;
            try {
                String resourcename = intent.getStringExtra(extra);
                Resources resources = context.getPackageManager()
                        .getResourcesForApplication(packagename);
                Log.i(TAG, "Looking up resource Id for " + resourcename);
                id = resources.getIdentifier(resourcename, "", packagename);
            } catch (NameNotFoundException e) {
                Log.e(TAG, "Package name not found", e);
            }
            return id;
        } else {
            return getResourceIdMetadata(metaDataReader, metadata);

        }
    }
}
