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
 */

package org.openintents.distribution.about;

/**
 * Metadata definition belonging to OI About.
 *
 * @author pjv
 * @author Peli
 * @version 2011-02-07: Add Metadata for recent changes.
 */
public final class AboutMetaData {

    /**
     * Empty, preventing instantiation.
     */
    private AboutMetaData() {
        //Empty, preventing instantiation.
    }

    /**
     * Application metadata: Comments about the program.
     * <p/>
     * <p>Usage example:
     * <code>&lt;meta-data
     * android:name="org.openintents.metadata.COMMENTS"
     * android:value="@string/about_comments" /&gt;</code>
     * </p>
     * <p/>
     * <p>
     * This key matches with AboutIntents.EXTRA_COMMENTS.
     * </p>
     * <p/>
     * <p>
     * Constant Value: "org.openintents.metadata.COMMENTS"
     * </p>
     */
    public static final String METADATA_COMMENTS =
            "org.openintents.metadata.COMMENTS";

    /**
     * Application metadata: Copyright information for the program.
     * <p/>
     * <p>Usage example:
     * <code>&lt;meta-data
     * android:name="org.openintents.metadata.COPYRIGHT"
     * android:value="@string/about_comments" /&gt;</code>
     * </p>
     * <p/>
     * <p>
     * This key matches with AboutIntents.EXTRA_COPYRIGHT.
     * </p>
     * <p/>
     * <p>
     * Constant Value: "org.openintents.metadata.COPYRIGHT"
     * </p>
     */
    public static final String METADATA_COPYRIGHT =
            "org.openintents.metadata.COPYRIGHT";

    /**
     * Application metadata: The URL for the link to the website of the program.
     * <p/>
     * <p>
     * This key matches with AboutIntents.EXTRA_WEBSITE_URL.
     * </p>
     * <p/>
     * <p>
     * Constant Value: "org.openintents.metadata.WEBSITE_URL"
     * </p>
     */
    public static final String METADATA_WEBSITE_URL =
            "org.openintents.metadata.WEBSITE_URL";

    /**
     * Application metadata: The label for the link to the website of the
     * program.
     * <p/>
     * <p>
     * This key matches with AboutIntents.EXTRA_WEBSITE_LABEL.
     * </p>
     * <p/>
     * <p>
     * Constant Value: "org.openintents.metadata.WEBSITE_LABEL"
     * </p>
     */
    public static final String METADATA_WEBSITE_LABEL =
            "org.openintents.metadata.WEBSITE_LABEL";

    /**
     * Application metadata: The authors of the program, as an array of strings.
     * <p/>
     * <p>Usage example:
     * <code>&lt;meta-data
     * android:name="org.openintents.metadata.AUTHORS"
     * android:resource="@array/about_authors" /&gt;</code>
     * </p>
     * <p/>
     * <p>
     * This key matches with AboutIntents.EXTRA_AUTHORS.
     * </p>
     * <p/>
     * <p>
     * Constant Value: "org.openintents.metadata.AUTHORS"
     * </p>
     */
    public static final String METADATA_AUTHORS = "org.openintents.metadata.AUTHORS";

    /**
     * Application metadata: The people documenting the program, as an array of
     * strings.
     * <p/>
     * <p>
     * This key matches with AboutIntents.EXTRA_DOCUMENTERS.
     * </p>
     * <p/>
     * <p>
     * Constant Value: "org.openintents.metadata.DOCUMENTERS"
     * </p>
     */
    public static final String METADATA_DOCUMENTERS =
            "org.openintents.metadata.DOCUMENTERS";

    /**
     * Application metadata:: The people who made the translation for the current
     * localization, as an array of strings.
     * <p/>
     * <p>
     * This key matches with AboutIntents.EXTRA_TRANSLATORS.
     * </p>
     * <p/>
     * <p>
     * Constant Value: "org.openintents.metadata.TRANSLATORS"
     * </p>
     */
    public static final String METADATA_TRANSLATORS =
            "org.openintents.metadata.TRANSLATORS";

    /**
     * Application metadata: The people who contributed artwork to the program,
     * as an array of strings.
     * <p/>
     * <p>
     * This key matches with AboutIntents.EXTRA_ARTISTS.
     * </p>
     * <p/>
     * <p>
     * Constant Value: "org.openintents.metadata.ARTISTS"
     * </p>
     */
    public static final String METADATA_ARTISTS = "org.openintents.metadata.ARTISTS";

    /**
     * Application metadata: The raw resource containing the license of the program.
     * <p/>
     * <p>
     * This key matches with AboutIntents.EXTRA_LICENSE_RESOURCE.
     * </p>
     * <p/>
     * <p>
     * Constant Value: "org.openintents.metadata.LICENSE"
     * </p>
     */
    public static final String METADATA_LICENSE = "org.openintents.metadata.LICENSE";

    /**
     * Application metadata: The primary email address for this application.
     * <p/>
     * <p>
     * This key matches with AboutIntents.EXTRA_EMAIL.
     * </p>
     * <p/>
     * <p>
     * Constant Value: "org.openintents.metadata.EMAIL"
     * </p>
     */
    public static final String METADATA_EMAIL = "org.openintents.metadata.EMAIL";

    /**
     * Application metadata: The raw resource containing the license of the program.
     * <p/>
     * <p>
     * This key matches with AboutIntents.EXTRA_RECENT_CHANGES_RESOURCE.
     * </p>
     * <p/>
     * <p>
     * Constant Value: "org.openintents.metadata.RECENT_CHANGES"
     * </p>
     */


    public static final String METADATA_RECENT_CHANGES = "org.openintents.metadata.RECENT_CHANGES";
    /**
     * Application metadata: The xml resource containing the file with other About metadata.
     * When found, all other metadata in manifest are ignored.
     * <p/>
     * <p>
     * This key doesn't match with any AboutIntents constant.
     * </p>
     * <p/>
     * <p>
     * Constant Value: "org.openintents.about"
     * </p>
     */
    public static final String METADATA_ABOUT = "org.openintents.about";


}
