/*   
 * 	 Copyright (C) 2008-2017 pjv (and others, see About dialog)
 * 
 * 	 This file is part of OI About.
 *
 *   OI About is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   OI About is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with OI About.  If not, see <http://www.gnu.org/licenses/>.
 *   
 *   
 *   
 *   The idea, window layout and elements, and some of the comments below are based on GtkAboutDialog. See http://library.gnome.org/devel/gtk/stable/GtkAboutDialog.html and http://www.gtk.org.
 */

package org.openintents.distribution.about;

import android.app.Activity;
import android.app.TabActivity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.text.util.Linkify.TransformFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageSwitcher;
import android.widget.TabHost;
import android.widget.TextSwitcher;
import android.widget.TextView;

import org.openintents.distribution.R;
import org.openintents.intents.AboutMiniIntents;
import org.openintents.util.IntentUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Main About dialog activity.
 *
 * @author pjv
 */
public class About extends TabActivity {
    //TODO BUG rotating screen broken due to TabHost?

    //private static final String LAUNCHPAD_TRANSLATOR_CREDITS_SEPARATOR = ";";
    //private static final String LAUNCHPAD_TRANSLATOR_CREDITS_REGEX = "("+LAUNCHPAD_TRANSLATOR_CREDITS_SEPARATOR+" )|("+LAUNCHPAD_TRANSLATOR_CREDITS_SEPARATOR+")";
    // Replace anything that looks like a link (starts with http) ...
    private static final String LAUNCHPAD_TRANSLATOR_CREDITS_REGEX_1 = "(http[^ ]*)";
    // ... by surrounding line breaks and a smaller font.
    private static final String LAUNCHPAD_TRANSLATOR_CREDITS_REGEX_2 = "<br/><small><small>$1</small></small><br/>";
    // for international translations omit the link altogether:
    private static final String LAUNCHPAD_TRANSLATOR_CREDITS_REGEX_3 = "<br/>";
    private static final String LAUNCHPAD_TRANSLATOR_CREDITS_HEADER = "(Launchpad Contributions: )|"
            + "(This is a dummy translation so that the credits are counted as translated. Launchpad Contributions: )";
    private static final String LAUNCHPAD_TRANSLATOR_CREDITS_TAG = "translator-credits";

    private static final String TAG = "About";


    private MetaDataReader metaDataReader;

    /**
     * The views.
     */
    protected ImageSwitcher mLogoImage;
    protected TextSwitcher mProgramNameAndVersionText;
    protected TextSwitcher mCommentsText;
    protected TextSwitcher mCopyrightText;
    protected TextSwitcher mWebsiteText;
    protected TextSwitcher mEmailText;
    protected TextView mAuthorsLabel;
    protected TextView mAuthorsText;
    protected TextView mDocumentersLabel;
    protected TextView mDocumentersText;
    protected TextView mTranslatorsLabel;
    protected TextView mTranslatorsText;
    protected TextView mArtistsLabel;
    protected TextView mArtistsText;
    protected TextView mInternationalTranslatorsLabel;
    protected TextView mInternationalTranslatorsText;
    protected TextView mNoInformationText;
    protected TextView mLicenseText;
    protected TextView mRecentChangesText;

    protected TabHost tabHost;

    /**
     * Menu item id's.
     */
    public static final int MENU_ITEM_ABOUT = Menu.FIRST;


    /**
     * Retrieve the package name to be used with this intent.
     * <p/>
     * Package name is retrieved from EXTRA_PACKAGE or from
     * getCallingPackage().
     * <p/>
     * If none is supplied, it is set to this application.
     */
    String getPackageNameFromIntent(Intent intent) {
        String packagename = null;

        if (intent.hasExtra(AboutIntents.EXTRA_PACKAGE_NAME)) {
            packagename = intent.getStringExtra(AboutIntents.EXTRA_PACKAGE_NAME);

            // Check whether packagename is valid:
            try {
                getPackageManager().getApplicationInfo(
                        packagename, 0);
            } catch (NameNotFoundException e) {
                Log.e(TAG, "Package name " + packagename + " is not valid.", e);
                packagename = null;
            }
        }

        // If no valid name has been found, we try to obtain it from
        // the calling activit.
        if (packagename == null) {
            // Retrieve from calling activity
            packagename = getCallingPackage();
        }

        if (packagename == null) {
            // In the worst case, use our own name:
            packagename = getPackageName();
        }

        return packagename;
    }

    /**
     * Change the logo image using the resource in the string argument.
     *
     * @param logoString String of a content uri to an image resource
     */
    protected void changeLogoImageUri(final String logoString) {
        Uri imageDescriptionUri = Uri.parse(logoString);
        if (imageDescriptionUri != null) {
            mLogoImage.setImageURI(imageDescriptionUri);
        } else { // Not a uri, so invalid.
            throw new IllegalArgumentException("Not a valid image.");
        }
    }

    /**
     * Change the logo image using the resource name and package.
     *
     * @param resourceFileName    String of the name of the image resource (as you would append
     *                            it after "R.drawable.").
     * @param resourcePackageName String of the name of the source package of the image resource
     *                            (the package name of the calling app).
     */
    protected void changeLogoImageResource(final String resourceFileName,
                                           final String resourcePackageName) {
        try {
            Resources resources = getPackageManager()
                    .getResourcesForApplication(resourcePackageName);
            final int id = resources
                    .getIdentifier(resourceFileName, null, null);
            mLogoImage.setImageDrawable(resources.getDrawable(id));
        } catch (NumberFormatException e) { // Not a resource id
            throw new IllegalArgumentException("Not a valid image.");
        } catch (NotFoundException e) { // Resource not found
            throw new IllegalArgumentException("Not a valid image.");
        } catch (NameNotFoundException e) { //Not a package name
            throw new IllegalArgumentException(
                    "Not a valid (image resource) package name.");
        }
        /*The idea for this came from:
			android.content.Intent.ShortcutIconResource and related contstants and intents, in android.content.Intent: http://android.git.kernel.org/?p=platform/frameworks/base.git;a=blob;f=core/java/android/content/Intent.java;h=39888c1bc0f62effa788815e5b9376969d255766;hb=master
			what's done with this in com.android.launcher.Launcher: http://android.git.kernel.org/?p=platform/packages/apps/Launcher.git;a=blob;f=src/com/android/launcher/Launcher.java;h=928f4caecde593d0fb430718de28d5e52df989ad;hb=HEAD
				and in android.webkit.gears.DesktopAndroid: http://android.git.kernel.org/?p=platform/frameworks/base.git;a=blob;f=core/java/android/webkit/gears/DesktopAndroid.java
		*/
    }

    /**
     * Fetch and display artists information.
     *
     * @param intent The intent from which to fetch the information.
     */
    protected void displayArtists(final String packagename, final Intent intent) {
        String[] textarray = AboutUtils.getStringArrayExtraOrMetadata(metaDataReader, this, packagename, intent, AboutIntents.EXTRA_ARTISTS, AboutMetaData.METADATA_ARTISTS);

        String text = AboutUtils.getTextFromArray(textarray);

        if (!TextUtils.isEmpty(text)) {
            mArtistsText.setText(text);
            mArtistsLabel.setVisibility(View.VISIBLE);
            mArtistsText.setVisibility(View.VISIBLE);
        } else {
            mArtistsLabel.setVisibility(View.GONE);
            mArtistsText.setVisibility(View.GONE);
        }
    }

    /**
     * Fetch and display authors information.
     *
     * @param intent The intent from which to fetch the information.
     */
    private void displayAuthors(final String packagename, final Intent intent) {
        String[] textarray = AboutUtils.getStringArrayExtraOrMetadata(metaDataReader, this, packagename, intent, AboutIntents.EXTRA_AUTHORS, AboutMetaData.METADATA_AUTHORS);

        String text = AboutUtils.getTextFromArray(textarray);

        if (!TextUtils.isEmpty(text)) {
            mAuthorsText.setText(text);
            mAuthorsLabel.setVisibility(View.VISIBLE);
            mAuthorsText.setVisibility(View.VISIBLE);
        } else {
            mAuthorsLabel.setVisibility(View.GONE);
            mAuthorsText.setVisibility(View.GONE);
        }
    }

    /**
     * Fetch and display comments information.
     *
     * @param intent The intent from which to fetch the information.
     */
    protected void displayComments(final String packagename, final Intent intent) {
        String text = AboutUtils.getStringExtraOrMetadata(metaDataReader, this, packagename, intent,
                AboutIntents.EXTRA_COMMENTS, AboutMetaData.METADATA_COMMENTS);

        if (!TextUtils.isEmpty(text)) {
            mCommentsText.setText(text);
            mCommentsText.setVisibility(View.VISIBLE);
        } else {
            mCommentsText.setVisibility(View.GONE);
        }
    }

    /**
     * Fetch and display copyright information.
     *
     * @param intent The intent from which to fetch the information.
     */
    protected void displayCopyright(final String packagename, final Intent intent) {
        String text = AboutUtils.getStringExtraOrMetadata(metaDataReader, this, packagename, intent,
                AboutIntents.EXTRA_COPYRIGHT, AboutMetaData.METADATA_COPYRIGHT);

        if (!TextUtils.isEmpty(text)) {
            mCopyrightText.setText(text);
            mCopyrightText.setVisibility(View.VISIBLE);
        } else {
            mCopyrightText.setVisibility(View.GONE);
        }
    }

    /**
     * Fetch and display documenters information.
     *
     * @param intent The intent from which to fetch the information.
     */
    protected void displayDocumenters(final String packagename, final Intent intent) {
        String[] textarray = AboutUtils.getStringArrayExtraOrMetadata(metaDataReader, this, packagename, intent,
                AboutIntents.EXTRA_DOCUMENTERS, AboutMetaData.METADATA_DOCUMENTERS);
        String text = AboutUtils.getTextFromArray(textarray);

        if (!TextUtils.isEmpty(text)) {
            mDocumentersText.setText(text);
            mDocumentersLabel.setVisibility(View.VISIBLE);
            mDocumentersText.setVisibility(View.VISIBLE);
        } else {
            mDocumentersLabel.setVisibility(View.GONE);
            mDocumentersText.setVisibility(View.GONE);
        }
    }


    /**
     * Fetch and display license information.
     *
     * @param intent The intent from which to fetch the information.
     */
    protected void displayLicense(final String packagename, final Intent intent) {

        int resourceid = AboutUtils.getResourceIdExtraOrMetadata(metaDataReader, this, packagename, intent,
                AboutIntents.EXTRA_LICENSE_RESOURCE, AboutMetaData.METADATA_LICENSE);

        if (resourceid == 0) {
            mLicenseText.setText(R.string.no_information_available);
            return;
        }

        String license = getRawResource(packagename, resourceid, false);

        mLicenseText.setText(license);
    }

    /**
     * Fetch and display recent changes information.
     *
     * @param intent The intent from which to fetch the information.
     */
    protected void displayRecentChanges(final String packagename, final Intent intent) {

        int resourceid = AboutUtils.getResourceIdExtraOrMetadata(metaDataReader, this, packagename, intent,
                AboutIntents.EXTRA_RECENT_CHANGES_RESOURCE, AboutMetaData.METADATA_RECENT_CHANGES);

        if (resourceid == 0) {
            // Tab is hidden if there are no recent changes.
            //mRecentChangesText.setText(R.string.no_information_available);
            return;
        }

        String recentchanges = getRawResource(packagename, resourceid, true);

        mRecentChangesText.setText(recentchanges);
    }

    /**
     * @return true if recent changes are available
     */
    protected boolean hasRecentChanges(final String packagename, final Intent intent) {
        int resourceid = AboutUtils.getResourceIdExtraOrMetadata(metaDataReader, this, packagename, intent,
                AboutIntents.EXTRA_RECENT_CHANGES_RESOURCE, AboutMetaData.METADATA_RECENT_CHANGES);

        return resourceid != 0;
    }

    private String getRawResource(final String packagename, int resourceid, boolean preserveLineBreaks) {
        // Retrieve text from resource:
        String text = "";
        try {
            Resources resources = getPackageManager()
                    .getResourcesForApplication(packagename);

            //Read in the license file as a big String
            BufferedReader in
                    = new BufferedReader(new InputStreamReader(
                    resources.openRawResource(resourceid)));
            String line;
            StringBuilder sb = new StringBuilder();
            try {
                while ((line = in.readLine()) != null) { // Read line per line.
                    if (TextUtils.isEmpty(line)) {
                        // Empty line: Leave line break
                        if (preserveLineBreaks) {
                            sb.append("\n");
                        } else {
                            sb.append("\n\n");
                        }
                    } else {
                        sb.append(line);
                        if (preserveLineBreaks) {
                            sb.append("\n");
                        } else {
                            sb.append(" ");
                        }
                    }
                }
                text = sb.toString();
            } catch (IOException e) {
                //Should not happen.
                e.printStackTrace();
            }

        } catch (NameNotFoundException e) {
            Log.e(TAG, "Package name not found", e);
        }
        return text;
    }

    /**
     * Fetch and display logo information.
     *
     * @param intent The intent from which to fetch the information.
     */
    protected void displayLogo(final String packagename, final Intent intent) {
        if (intent.hasExtra(AboutIntents.EXTRA_ICON_RESOURCE)
                && intent.getStringExtra(AboutIntents.EXTRA_ICON_RESOURCE) != null) {
            try {
                changeLogoImageResource(intent.getStringExtra(AboutIntents.EXTRA_ICON_RESOURCE),
                        packagename);
            } catch (IllegalArgumentException e) {
                setErrorLogo();
            }
        } else if (intent.hasExtra(AboutIntents.EXTRA_ICON_URI)
                && intent.getStringExtra(AboutIntents.EXTRA_ICON_URI) != null) {
            try {
                changeLogoImageUri(intent.getStringExtra(AboutIntents.EXTRA_ICON_URI));
            } catch (IllegalArgumentException e) {
                setErrorLogo();
            }
        } else {
            try {
                PackageInfo pi = getPackageManager().getPackageInfo(
                        packagename, 0);
                Resources resources = getPackageManager()
                        .getResourcesForApplication(packagename);
                String resourcename = resources.getResourceName(pi.applicationInfo.icon);
                changeLogoImageResource(resourcename, packagename);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Package name not found", e);
                setErrorLogo();
            } catch (Resources.NotFoundException e) {
                Log.e(TAG, "Package name not found", e);
                setErrorLogo();
            } catch (IllegalArgumentException e) {
                setErrorLogo();
            }
        }
    }

    private void setErrorLogo() {
        mLogoImage.setImageResource(android.R.drawable.ic_menu_info_details);
    }

    /**
     * Fetch and display program name and version information.
     *
     * @param intent The intent from which to fetch the information.
     */
    protected void displayProgramNameAndVersion(final String packagename, final Intent intent) {
        String applicationlabel = getApplicationLabel(packagename, intent);
        String versionname = getVersionName(packagename, intent);

        String combined = applicationlabel;
        if (!TextUtils.isEmpty(versionname)) {
            combined += " " + versionname;
        }

        mProgramNameAndVersionText.setText(combined);

        String title = getString(R.string.about_activity_name_extended, applicationlabel);
        setTitle(title);
    }

    /**
     * Get application label.
     *
     * @param intent The intent from which to fetch the information.
     */
    protected String getApplicationLabel(final String packagename, final Intent intent) {
        String applicationlabel = null;
        if (intent.hasExtra(AboutIntents.EXTRA_APPLICATION_LABEL)
                && intent.getStringExtra(AboutIntents.EXTRA_APPLICATION_LABEL)
                != null) {
            applicationlabel = intent.getStringExtra(AboutIntents.EXTRA_APPLICATION_LABEL);
        } else {
            try {
                PackageInfo pi = getPackageManager().getPackageInfo(
                        packagename, 0);
                int labelid = pi.applicationInfo.labelRes;
                Resources resources = getPackageManager()
                        .getResourcesForApplication(packagename);
                applicationlabel = resources.getString(labelid);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Package name not found", e);
            }
        }
        return applicationlabel;
    }

    /**
     * Get version information.
     *
     * @param intent The intent from which to fetch the information.
     */
    protected String getVersionName(final String packagename, final Intent intent) {
        String versionname = null;
        if (intent.hasExtra(AboutIntents.EXTRA_VERSION_NAME)
                && intent.getStringExtra(AboutIntents.EXTRA_VERSION_NAME)
                != null) {
            versionname = intent.getStringExtra(AboutIntents.EXTRA_VERSION_NAME);
        } else {
            try {
                PackageInfo pi = getPackageManager().getPackageInfo(
                        packagename, 0);
                versionname = pi.versionName;
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Package name not found", e);
            }
        }
        return versionname;
    }

    /**
     * Fetch and display translators information.
     *
     * @param intent The intent from which to fetch the information.
     */
    protected void displayTranslators(final String packagename,
                                      final Intent intent) {

        String[] textarray = AboutUtils.getStringArrayExtraOrMetadata(metaDataReader, this,
                packagename, intent, AboutIntents.EXTRA_TRANSLATORS,
                AboutMetaData.METADATA_TRANSLATORS);
        String text = AboutUtils.getTextFromArray(textarray);

        if (!TextUtils.isEmpty(text)) {
            mTranslatorsText.setText(text);
            mTranslatorsLabel.setVisibility(View.VISIBLE);
            mTranslatorsText.setVisibility(View.VISIBLE);
        } else {
            text = AboutUtils.getStringExtraOrMetadata(metaDataReader, this,
                    packagename, intent, AboutIntents.EXTRA_TRANSLATORS,
                    AboutMetaData.METADATA_TRANSLATORS);

            // Create string array of translators from translated string
            // from Launchpad or (for English) from the array.
            if (!text.equals(LAUNCHPAD_TRANSLATOR_CREDITS_TAG) && !TextUtils.isEmpty(text)) {
                //	textarray = text.replaceFirst(
                //			LAUNCHPAD_TRANSLATOR_CREDITS_HEADER, "").split(LAUNCHPAD_TRANSLATOR_CREDITS_REGEX);
                //	text = AboutUtils.getTextFromArray(textarray);
                //	mTranslatorsText.setText(text);
                text = text.replaceFirst(
                        LAUNCHPAD_TRANSLATOR_CREDITS_HEADER, "").replaceAll(
                        LAUNCHPAD_TRANSLATOR_CREDITS_REGEX_1,
                        LAUNCHPAD_TRANSLATOR_CREDITS_REGEX_2);

                // take away final "<br/>"
                if (text.length() > 5) {
                    text = text.substring(0, text.length() - 5);
                }

                CharSequence styledText = Html.fromHtml(text);

                mTranslatorsText.setText(styledText);
                mTranslatorsText.setLinksClickable(true);
                mTranslatorsLabel.setVisibility(View.VISIBLE);
                mTranslatorsText.setVisibility(View.VISIBLE);
            } else {
                mTranslatorsLabel.setVisibility(View.GONE);
                mTranslatorsText.setVisibility(View.GONE);
            }
        }

    }

    /**
     * Fetch and display international translators information.
     * This is only possible through the string resource directly - not through intent.
     */
    protected void displayInternationalTranslators(final String packagename) {

        int id = AboutUtils.getResourceIdMetadata(metaDataReader, AboutMetaData.METADATA_TRANSLATORS);

        String text = null;

        if (id != 0) {
            // local resources
            Resources res = getResources();
            String[] languages = res.getStringArray(R.array.languages);
            String[] languagenames = res.getStringArray(R.array.language_names);

            if (languages.length != languagenames.length) {
                // Language array lengths must agree!
                throw new RuntimeException();
            }
            try {
                // remote resources:
                Resources resources = getPackageManager()
                        .getResourcesForApplication(packagename);

                StringBuilder sb = new StringBuilder();

                HashMap<String, String> translatorHash = new HashMap<String, String>();

                for (int i = 0; i < languages.length; i++) {
                    String lang = languages[i].substring(0, 2);
                    String country = "";
                    if (languages[i].length() > 3) {
                        country = languages[i].substring(3);
                    }

                    Locale locale = new Locale(lang, country);
                    Configuration config = new Configuration();
                    config.locale = locale;
                    resources.updateConfiguration(config, null);
                    text = resources.getString(id);

                    // Make sure that text is unique within a language.
                    // e.g. If pt and pt_BR give same translators,
                    //      only pt should be shown.
                    //      If they differ, both should be shown.
                    //      pt: Portugese, pt_BR: Portugese (Brazilian)
                    boolean showCountry = true;
                    if (translatorHash.containsKey(text) && translatorHash.get(text).equals(lang)) {
                        showCountry = false;
                    }
                    if (TextUtils.isEmpty(country)) {
                        // Only add base language translations to hash
                        translatorHash.put(text, lang);
                    }

                    if (showCountry
                            && !text.equals(LAUNCHPAD_TRANSLATOR_CREDITS_TAG)
                            && !TextUtils.isEmpty(text)) {
                        text = text.replaceFirst(
                                LAUNCHPAD_TRANSLATOR_CREDITS_HEADER, "").replaceAll(
                                LAUNCHPAD_TRANSLATOR_CREDITS_REGEX_1,
                                LAUNCHPAD_TRANSLATOR_CREDITS_REGEX_3);
                        sb.append("<font color=\"#c0c0c0\"><small>");
                        sb.append(languagenames[i]);
                        sb.append("</small></font><br/>");
                        sb.append(text);
                        sb.append("<br/>");
                    }
                }

                // take away final "<br/><br/>"
                if (sb.length() > 10) {
                    text = sb.substring(0, sb.length() - 10);
                } else {
                    text = sb.toString();
                }

            } catch (NameNotFoundException e) {
                Log.e(TAG, "Package name not found ", e);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Metadata not valid id.", e);
            } catch (Resources.NotFoundException e) {
                Log.e(TAG, "Resource not found.", e);
            }

        }

        if (!TextUtils.isEmpty(text)) {
            CharSequence styledText = Html.fromHtml(text);

            mInternationalTranslatorsText.setText(styledText);
            mInternationalTranslatorsText.setLinksClickable(true);
            mInternationalTranslatorsLabel.setVisibility(View.VISIBLE);
            mInternationalTranslatorsText.setVisibility(View.VISIBLE);
        } else {
            mInternationalTranslatorsLabel.setVisibility(View.GONE);
            mInternationalTranslatorsText.setVisibility(View.GONE);
        }

    }

    /**
     * Fetch and display website link information.
     *
     * @param intent The intent from which to fetch the information.
     */
    protected void displayWebsiteLink(final String packagename, final Intent intent) {
        String websitelabel = AboutUtils.getStringExtraOrMetadata(metaDataReader, this, packagename,
                intent, AboutIntents.EXTRA_WEBSITE_LABEL, AboutMetaData.METADATA_WEBSITE_LABEL);
        String websiteurl = AboutUtils.getStringExtraOrMetadata(metaDataReader, this, packagename,
                intent, AboutIntents.EXTRA_WEBSITE_URL, AboutMetaData.METADATA_WEBSITE_URL);

        setAndLinkifyWebsiteLink(websitelabel, websiteurl);
    }

    /**
     * Set the website link TextView and linkify.
     *
     * @param websiteLabel The label to set.
     * @param websiteUrl   The URL that the label links to.
     */
    protected void setAndLinkifyWebsiteLink(final String websiteLabel, final String websiteUrl) {
        if (!TextUtils.isEmpty(websiteUrl)) {
            if (TextUtils.isEmpty(websiteLabel)) {
                mWebsiteText.setText(websiteUrl);
            } else {
                mWebsiteText.setText(websiteLabel);
            }
            mWebsiteText.setVisibility(View.VISIBLE);

            //Create TransformFilter
            TransformFilter tf = new TransformFilter() {

                public String transformUrl(final Matcher matcher,
                                           final String url) {
                    return websiteUrl;
                }

            };

            //Allow a label and url through Linkify
            Linkify.addLinks((TextView) mWebsiteText.getChildAt(0), Pattern
                    .compile(".*"), "", null, tf);
            Linkify.addLinks((TextView) mWebsiteText.getChildAt(1), Pattern
                    .compile(".*"), "", null, tf);
        } else {
            mWebsiteText.setVisibility(View.GONE);
        }
    }

    /**
     * Fetch and display website link information.
     *
     * @param intent The intent from which to fetch the information.
     */
    protected void displayEmail(final String packagename, final Intent intent) {
        String email = AboutUtils.getStringExtraOrMetadata(metaDataReader, this, packagename,
                intent, AboutIntents.EXTRA_EMAIL, AboutMetaData.METADATA_EMAIL);

        if (!TextUtils.isEmpty(email)) {
            mEmailText.setText(email);
            mEmailText.setVisibility(View.VISIBLE);
        } else {
            mEmailText.setVisibility(View.GONE);
        }
    }

    /**
     * Check whether any credits are available.
     * If not, display "no information available".
     */
    void checkCreditsAvailable() {
        if (mAuthorsLabel.getVisibility() == View.GONE
                && mAuthorsLabel.getVisibility() == View.GONE
                && mAuthorsLabel.getVisibility() == View.GONE
                && mAuthorsLabel.getVisibility() == View.GONE) {
            mNoInformationText.setVisibility(View.VISIBLE);
        } else {
            mNoInformationText.setVisibility(View.GONE);
        }

    }

    /* (non-Javadoc)
     * @see android.app.ActivityGroup#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Resources res = getResources();

        Map<String, String> metaDataNameToTagName = new HashMap<String, String>();
        //TODO Move this to detached file?
        metaDataNameToTagName.put("comments", AboutMetaData.METADATA_COMMENTS);
        metaDataNameToTagName.put("copyright", AboutMetaData.METADATA_COPYRIGHT);
        metaDataNameToTagName.put("website-url", AboutMetaData.METADATA_WEBSITE_URL);
        metaDataNameToTagName.put("website-label", AboutMetaData.METADATA_WEBSITE_LABEL);
        metaDataNameToTagName.put("authors", AboutMetaData.METADATA_AUTHORS);
        metaDataNameToTagName.put("documenters", AboutMetaData.METADATA_DOCUMENTERS);
        metaDataNameToTagName.put("translators", AboutMetaData.METADATA_TRANSLATORS);
        metaDataNameToTagName.put("artists", AboutMetaData.METADATA_ARTISTS);
        metaDataNameToTagName.put("license", AboutMetaData.METADATA_LICENSE);
        metaDataNameToTagName.put("email", AboutMetaData.METADATA_EMAIL);
        metaDataNameToTagName.put("recent-changes", AboutMetaData.METADATA_RECENT_CHANGES);

        String packageName = getPackageNameFromIntent(getIntent());
        try {
            metaDataReader = new MetaDataReader(getApplicationContext(),
                    packageName,
                    metaDataNameToTagName);
        } catch (NameNotFoundException e) {
            throw new IllegalArgumentException("Package name '" + packageName + "' doesn't exist.");
        }


        //Set up the layout with the TabHost
        tabHost = getTabHost();

        LayoutInflater.from(this).inflate(R.layout.oi_distribution_about,
                tabHost.getTabContentView(), true);

        tabHost.addTab(tabHost.newTabSpec(getString(R.string.l_info))
                .setIndicator(getString(R.string.l_info))
                .setContent(R.id.info));
        tabHost.addTab(tabHost.newTabSpec(getString(R.string.l_credits))
                .setIndicator(getString(R.string.l_credits))
                .setContent(R.id.credits));
        tabHost.addTab(tabHost.newTabSpec(getString(R.string.l_license))
                .setIndicator(getString(R.string.l_license))
                .setContent(R.id.license));

        // Add fourth tab only if "recent changes" information
        // has been provided
        final Intent intent = getIntent();
        if (intent == null) {
            setIntent(new Intent());
        }

        if (hasRecentChanges(packageName, intent)) {
            tabHost.addTab(tabHost.newTabSpec(getString(R.string.l_recent_changes))
                    .setIndicator(getString(R.string.l_recent_changes))
                    .setContent(R.id.recent_changes));
        }

        //Set the animations for the switchers
        Animation in = AnimationUtils.loadAnimation(this,
                android.R.anim.slide_in_left);
        Animation out = AnimationUtils.loadAnimation(this,
                android.R.anim.slide_out_right);

        //Find the views
        mLogoImage = findViewById(R.id.i_logo);
        mLogoImage.setInAnimation(in);
        mLogoImage.setOutAnimation(out);

        mProgramNameAndVersionText = findViewById(R.id.t_program_name_and_version);
        mProgramNameAndVersionText.setInAnimation(in);
        mProgramNameAndVersionText.setOutAnimation(out);

        mCommentsText = findViewById(R.id.t_comments);
        mCommentsText.setInAnimation(in);
        mCommentsText.setOutAnimation(out);

        mCopyrightText = findViewById(R.id.t_copyright);
        mCopyrightText.setInAnimation(in);
        mCopyrightText.setOutAnimation(out);

        mWebsiteText = findViewById(R.id.t_website);
        mWebsiteText.setInAnimation(in);
        mWebsiteText.setOutAnimation(out);

        mEmailText = findViewById(R.id.t_email);
        mEmailText.setInAnimation(in);
        mEmailText.setOutAnimation(out);

        mAuthorsLabel = findViewById(R.id.l_authors);
        mAuthorsText = findViewById(R.id.et_authors);

        mDocumentersLabel = findViewById(R.id.l_documenters);
        mDocumentersText = findViewById(R.id.et_documenters);

        mTranslatorsLabel = findViewById(R.id.l_translators);
        mTranslatorsText = findViewById(R.id.et_translators);

        mArtistsLabel = findViewById(R.id.l_artists);
        mArtistsText = findViewById(R.id.et_artists);

        mInternationalTranslatorsLabel = findViewById(R.id.l_international_translators);
        mInternationalTranslatorsText = findViewById(R.id.et_international_translators);

        mNoInformationText = findViewById(R.id.tv_no_information);

        mLicenseText = findViewById(R.id.et_license);

        mRecentChangesText = findViewById(R.id.et_recent_changes);
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        // Generate any additional actions that can be performed on the
        // overall list. In a normal install, there are no additional
        // actions found here, but this allows other applications to extend
        // our menu with their own actions.
        Intent intent = new Intent(null, getIntent().getData());
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, About.class), null,
                intent, 0, null);

        return true;
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onNewIntent(android.content.Intent)
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }


    /* (non-Javadoc)
     * @see android.app.ActivityGroup#onResume()
     */
    @Override
    protected void onResume() {
        super.onResume();

        // tabHost.setCurrentTabByTag(getString(R.string.l_info));

        //Decode the intent, if any
        final Intent intent = getIntent();
		/*
        if (intent == null) {
        	refuseToShow();
        	return;
        }
        */
        if (intent == null) {
            setIntent(new Intent());
        }

        String packagename = getPackageNameFromIntent(intent);

        Log.i(TAG, "Showing About dialog for package " + packagename);

        displayLogo(packagename, intent);
        displayProgramNameAndVersion(packagename, intent);
        displayComments(packagename, intent);
        displayCopyright(packagename, intent);
        displayWebsiteLink(packagename, intent);
        displayAuthors(packagename, intent);
        displayDocumenters(packagename, intent);
        displayTranslators(packagename, intent);
        displayArtists(packagename, intent);
        displayInternationalTranslators(packagename);
        displayLicense(packagename, intent);
        displayEmail(packagename, intent);
        displayRecentChanges(packagename, intent);

        checkCreditsAvailable();

        setResult(RESULT_OK);
    }

    public static void showDialogOrStartActivity(Activity activity, int dialogId) {
        Intent intent = new Intent(AboutMiniIntents.ACTION_SHOW_ABOUT_DIALOG);
        intent.setComponent(new ComponentName(activity, About.class));
        intent.putExtra(AboutMiniIntents.EXTRA_PACKAGE_NAME, activity.getPackageName());

        if (IntentUtils.isIntentAvailable(activity, intent)) {
            activity.startActivity(intent);
        } else {
            activity.showDialog(dialogId);
        }
    }
}
