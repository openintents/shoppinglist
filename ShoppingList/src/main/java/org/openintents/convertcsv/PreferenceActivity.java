/*
 * Copyright (C) 2008 OpenIntents.org
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

package org.openintents.convertcsv;

import android.os.Bundle;

import org.openintents.shopping.R;

public class PreferenceActivity extends android.preference.PreferenceActivity {
    public static final String PREFS_SHOPPINGLIST_ENCODING = "shopping_encoding";
    public static final String PREFS_SHOPPINGLIST_USE_CUSTOM_ENCODING = "shoppinglist_use_custom_encoding";
    public static final String PREFS_SHOPPINGLIST_FILENAME = "shoppinglist_filename";
    public static final String PREFS_SHOPPINGLIST_FORMAT = "shoppinglist_format";
    public static final String PREFS_ASK_IF_FILE_EXISTS = "ask_if_file_exists";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences_convertcsv);
    }
}
