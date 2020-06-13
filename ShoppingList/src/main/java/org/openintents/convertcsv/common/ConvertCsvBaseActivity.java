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

package org.openintents.convertcsv.common;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.provider.OpenableColumns;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml.Encoding;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import org.openintents.convertcsv.PreferenceActivity;
import org.openintents.distribution.DownloadOIAppDialog;
import org.openintents.shopping.R;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

public class ConvertCsvBaseActivity extends AppCompatActivity {

    static final public int IMPORT_POLICY_DUPLICATE = 0;
    static final public int IMPORT_POLICY_KEEP = 1;
    static final public int IMPORT_POLICY_OVERWRITE = 2;
    static final public int IMPORT_POLICY_RESTORE = 3;
    static final public int IMPORT_POLICY_MAX = IMPORT_POLICY_RESTORE;
    static final public int MESSAGE_SET_PROGRESS = 1;    // Progress changed, arg1 = new status
    static final public int MESSAGE_SUCCESS = 2;        // Operation finished.
    static final public int MESSAGE_ERROR = 3;            // An error occured, arg1 = string ID of error
    static final public int MESSAGE_SET_MAX_PROGRESS = 4;    // Set maximum progress int, arg1 = new max value
    protected static final int MENU_SETTINGS = Menu.FIRST + 1;
    protected static final int MENU_CANCEL = Menu.FIRST + 2;
    protected static final int MENU_DISTRIBUTION_START = Menu.FIRST + 100; // MUST BE LAST
    protected static final int DIALOG_ID_WARN_OVERWRITE = 1;
    protected static final int DIALOG_ID_NO_FILE_MANAGER_AVAILABLE = 2;
    protected static final int DIALOG_ID_WARN_RESTORE_POLICY = 3;
    protected static final int DIALOG_ID_PERMISSIONS = 4;
    protected static final int DIALOG_DISTRIBUTION_START = 100; // MUST BE LAST
    protected static final int REQUEST_CODE_PICK_FILE = 1;
    private final static String TAG = "ConvertCsvBaseActivity";
    // This is the activity's message handler that the worker thread can use to communicate
    // with the main thread. This may be null if the activity is paused and could change, so
    // it needs to be read and verified before every use.
    static protected Handler smCurrentHandler;
    // True if we have an active worker thread.
    static boolean smHasWorkerThread;
    // Max value for the progress bar.
    static int smProgressMax;
    protected TextView mFilePathView;
    protected TextView mFileNameView;
    protected TextView mConvertInfo;
    protected Spinner mSpinner;
    protected String PREFERENCE_FILENAME;
    protected String DEFAULT_FILENAME;
    protected String PREFERENCE_FORMAT;
    protected String DEFAULT_FORMAT = null;
    protected String PREFERENCE_ENCODING;
    protected String PREFERENCE_USE_CUSTOM_ENCODING;
    protected int RES_STRING_FILEMANAGER_TITLE = 0;
    protected int RES_STRING_FILEMANAGER_BUTTON_TEXT = 0;
    protected int RES_ARRAY_CSV_FILE_FORMAT = 0;
    protected int RES_ARRAY_CSV_FILE_FORMAT_VALUE = 0;
    String[] mFormatValues;
    // Message handler that receives status messages from the
    // CSV import/export thread.
    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_SET_PROGRESS:
                    ConvertCsvBaseActivity.this.setConversionProgress(msg.arg1);
                    break;

                case MESSAGE_SET_MAX_PROGRESS:
                    ConvertCsvBaseActivity.this.setMaxProgress(msg.arg1);
                    break;


                case MESSAGE_SUCCESS:
                    ConvertCsvBaseActivity.this.displayMessage(msg.arg1, true);
                    break;

                case MESSAGE_ERROR:
                    ConvertCsvBaseActivity.this.displayMessage(msg.arg1, false);
                    break;
            }
        }
    };

    private Spinner mSpinnerEncoding;

    private CheckBox mCustomEncoding;

    private OnCheckedChangeListener mCustomEncodingListener = new OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            mSpinnerEncoding.setEnabled(isChecked);
        }
    };

    private static int findString(String[] array, String string) {
        int length = array.length;
        for (int i = 0; i < length; i++) {
            if (string.equals(array[i])) {
                return i;
            }
        }
        return -1;
    }

    static public void dispatchSuccess(int successMsg) {
        dispatchMessage(MESSAGE_SUCCESS, successMsg);
    }

    static public void dispatchError(int errorMsg) {
        dispatchMessage(MESSAGE_ERROR, errorMsg);
    }

    static public void dispatchConversionProgress(int newProgress) {
        dispatchMessage(MESSAGE_SET_PROGRESS, newProgress);
    }

    static public void dispatchSetMaxProgress(int maxProgress) {
        dispatchMessage(MESSAGE_SET_MAX_PROGRESS, maxProgress);
    }

    static void dispatchMessage(int what, int argument) {
        // Cache the handler since the other thread could modify it at any time.
        Handler handler = smCurrentHandler;

        if (handler != null) {
            Message msg = Message.obtain(handler, what, argument, 0);
            handler.sendMessage(msg);
        }
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Always create the main layout first, since we need to populate the
        // variables with all the views.
        switchToMainLayout();
        if (smHasWorkerThread) {
            switchToConvertLayout();
        }
    }


    private void switchToMainLayout() {
        setContentView(R.layout.convert);

        DEFAULT_FILENAME = getString(R.string.default_filename);

        setPreferencesUsed();

        mFilePathView = findViewById(R.id.file_path);
        mFileNameView = findViewById(R.id.file_name);

        SharedPreferences pm = PreferenceManager.getDefaultSharedPreferences(this);
        String filepath = pm.getString(PREFERENCE_FILENAME, "");

        if (TextUtils.isEmpty(filepath)) {
            setFileUriUnknown();
        } else {
            setFileUri(Uri.parse(filepath));
        }


        ImageButton buttonFileManager = findViewById(R.id.new_document);
        buttonFileManager.setOnClickListener(arg0 -> openFileManagerForNewDocument());

        buttonFileManager = findViewById(R.id.open_document);
        buttonFileManager.setOnClickListener(arg0 -> openFileManagerForChoosingDocument());

        mConvertInfo = findViewById(R.id.convert_info);

        Button buttonImport = findViewById(R.id.file_import);
        buttonImport.setOnClickListener(arg0 -> startImport());

        Button buttonExport = findViewById(R.id.file_export);
        buttonExport.setOnClickListener(arg0 -> startExport());

        mSpinner = (Spinner) findViewById(R.id.spinner1);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, RES_ARRAY_CSV_FILE_FORMAT, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(adapter);

        mFormatValues = getResources().getStringArray(RES_ARRAY_CSV_FILE_FORMAT_VALUE);

        setSpinner(pm.getString(PREFERENCE_FORMAT, DEFAULT_FORMAT));

        // set encoding spinner
        mSpinnerEncoding = (Spinner) findViewById(R.id.spinner_encoding);
        EncodingAdapter adapterEncoding = new EncodingAdapter(this, android.R.layout.simple_spinner_item);
        adapterEncoding.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerEncoding.setAdapter(adapterEncoding);

        String encodingString = getDefaultEncoding().name();
        try {
            encodingString = pm.getString(PREFERENCE_ENCODING, encodingString);
        } catch (ClassCastException ignored) {
        }

        Encoding encoding;
        try {
            encoding = Encoding.valueOf(encodingString);
        } catch (IllegalArgumentException e) {
            encoding = Encoding.UTF_8;
        }
        int encodingPosition = adapterEncoding.getPosition(encoding);
        if (encodingPosition != Spinner.INVALID_POSITION) {
            mSpinnerEncoding.setSelection(encodingPosition);
        }

        // set encoding checkbox
        mCustomEncoding = (CheckBox) findViewById(R.id.custom_encoding);
        mCustomEncoding.setOnCheckedChangeListener(mCustomEncodingListener);
        mCustomEncoding.setChecked(pm.getBoolean(PREFERENCE_USE_CUSTOM_ENCODING, false));

        Intent intent = getIntent();
        String type = intent.getType();
        if (type != null && type.equals("text/csv")) {
            // Someone wants to import a CSV document through the file manager.
            // Set the path accordingly:
            Uri path = getIntent().getData();
            if (path != null) {
                setFileUri(path);
            } else {
                setFileUriUnknown();
            }
        }
    }

    private void switchToConvertLayout() {
        setContentView(R.layout.convertprogress);
        ((ProgressBar) findViewById(R.id.Progress)).setMax(smProgressMax);
        smCurrentHandler = mHandler;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // The worker thread is on its own now.
        smCurrentHandler = null;
    }

    public void setSpinner(String value) {
        // get the ID:
        int id = findString(mFormatValues, value);

        if (id != -1) {
            mSpinner.setSelection(id);
        }
    }

    public void setPreferencesUsed() {

    }

    /**
     * Display the current import policy.
     */
    public void displayImportPolicy() {
        int importPolicy = getValidatedImportPolicy();

        String[] policyStrings = getResources().getStringArray(R.array.import_policy_detail);

        TextView policyView = findViewById(R.id.import_policy_detail);

        if (policyView != null) {
            policyView.setText(policyStrings[importPolicy]);
        }
    }

    public int getValidatedImportPolicy() {
        String prefKey = getImportPolicyPrefString();

        if (prefKey == null) {
            // This activity does not support import policies.
            return IMPORT_POLICY_DUPLICATE;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        String importPolicy = prefs.getString(prefKey, getDefaultImportPolicy());

        try {
            int policy = Integer.parseInt(importPolicy);

            if (policy < 0 || policy > IMPORT_POLICY_MAX) {
                return 0;
            }

            return policy;
        } catch (NumberFormatException e) {
            // Invalid prefs.
            return 0;
        }
    }

    /**
     * @return The string that identifies the import policy for this importer.
     * null if this derived activity does not support import policies.
     */
    public String getImportPolicyPrefString() {
        return null;
    }

    /**
     * @return The default import policy
     */
    public String getDefaultImportPolicy() {
        return "" + IMPORT_POLICY_DUPLICATE;
    }

    public void startImport() {
        int importPolicy = getValidatedImportPolicy();

        if (importPolicy == IMPORT_POLICY_RESTORE) {
            showDialog(DIALOG_ID_WARN_RESTORE_POLICY);
        } else {
            startImportPostCheck();
        }
    }

    public void startImportPostCheck() {
        // First delete old lists
        //getContentResolver().delete(Shopping.Contains.CONTENT_URI, null, null);
        //getContentResolver().delete(Shopping.Items.CONTENT_URI, null, null);
        //getContentResolver().delete(Shopping.Lists.CONTENT_URI, null, null);


        String fileName = getFilenameAndSavePreferences();

        Log.i(TAG, "Importing..." + fileName);

        final Uri file = Uri.parse(fileName);


        // If this is the RESTORE policy, make sure we let the user know
        // what kind of trouble he's getting himself into.
        switchToConvertLayout();
        smHasWorkerThread = true;
        supportInvalidateOptionsMenu();

        new Thread() {
            public void run() {
                try {

                    InputStream inputStream = getContentResolver().openInputStream(file);

                    Reader reader;

                    Encoding enc = getCurrentEncoding();
                    if (enc == null) {
                        reader = new InputStreamReader(inputStream);
                    } else {
                        reader = new InputStreamReader(inputStream, enc.name());
                    }

                    int size = getDocumentSize(file);
                    if (size > 0) {
                        smProgressMax = size;
                        ((ProgressBar) findViewById(R.id.Progress)).setMax(smProgressMax);
                    }

                    doImport(reader);

                    reader.close();
                    dispatchSuccess(R.string.import_finished);
                    onImportFinished();

                } catch (FileNotFoundException e) {
                    dispatchError(R.string.error_file_not_found);
                    Log.i(TAG, "File not found", e);
                } catch (IOException e) {
                    dispatchError(R.string.error_reading_file);
                    Log.i(TAG, "IO exception", e);
                } catch (WrongFormatException e) {
                    dispatchError(R.string.wrong_csv_format);
                    Log.i(TAG, "array index out of bounds", e);
                }

                smHasWorkerThread = false;
                supportInvalidateOptionsMenu();
            }
        }.start();

    }

    public int getDocumentSize(Uri uri) {
        Cursor cursor = getContentResolver()
                .query(uri, null, null, null, null, null);

        int size = -1;
        try {
            if (cursor != null && cursor.moveToFirst()) {
                String displayName = cursor.getString(
                        cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                Log.i(TAG, "Display Name: " + displayName);

                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (!cursor.isNull(sizeIndex)) {
                    size = cursor.getInt(sizeIndex);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return size;
    }

    public String getDocumentName(Uri uri) {
        Cursor cursor = getContentResolver()
                .query(uri, null, null, null, null, null);

        String displayName = uri.getLastPathSegment();
        try {
            if (cursor != null && cursor.moveToFirst()) {
                displayName = cursor.getString(
                        cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return displayName;
    }

    protected Encoding getCurrentEncoding() {
        if (mCustomEncoding.isChecked()) {
            return (Encoding) mSpinnerEncoding.getSelectedItem();
        } else {
            return getDefaultEncoding();
        }
    }

    protected Encoding getDefaultEncoding() {
        return Encoding.UTF_8;
    }

    void displayMessage(int message, boolean success) {
        // Just make a toast instead?
        //Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        //finish();

        new AlertDialog.Builder(this)
                .setIcon((success) ? android.R.drawable.ic_dialog_info : android.R.drawable.ic_dialog_alert)
                .setMessage(message)
                .setPositiveButton(R.string.dialog_ok, (dialog, which) -> finish())
                .show();
    }

    void setConversionProgress(int newProgress) {
        ((ProgressBar) findViewById(R.id.Progress)).setProgress(newProgress);
    }

    void setMaxProgress(int maxProgress) {
        ((ProgressBar) findViewById(R.id.Progress)).setMax(maxProgress);
    }

    /**
     * @param reader
     * @throws IOException
     */
    public void doImport(Reader reader) throws IOException,
            WrongFormatException {

    }

    public void onImportFinished() {

    }

    public void startExport() {
        Log.i(TAG, "Exporting...");
        doExport();
    }

    public void doExport() {
        String fileName = getFilenameAndSavePreferences();
        final Uri file = Uri.parse(fileName);

        switchToConvertLayout();
        smHasWorkerThread = true;

        new Thread() {
            public void run() {
                try {
                    Writer writer;
                    Encoding enc = getCurrentEncoding();
                    ParcelFileDescriptor pfd = getContentResolver().
                            openFileDescriptor(file, "w");
                    if (enc == null) {
                        writer = new OutputStreamWriter(new FileOutputStream(pfd.getFileDescriptor()));
                    } else {
                        writer = new OutputStreamWriter(new FileOutputStream(pfd.getFileDescriptor()), enc.name());
                    }


                    doExport(writer);

                    writer.close();
                    pfd.close();
                    dispatchSuccess(R.string.export_finished);

                } catch (IOException e) {
                    dispatchError(R.string.error_writing_file);
                    Log.i(TAG, "IO exception", e);
                }

                smHasWorkerThread = false;
            }
        }.start();
    }

    /**
     * @param writer
     * @throws IOException
     */
    public void doExport(Writer writer) throws IOException {

    }

    /**
     * @return
     */
    public String getFilenameAndSavePreferences() {

        String fileName = mFilePathView.getText().toString();

        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this);
        Editor editor = prefs.edit();
        editor.putString(PREFERENCE_FILENAME, fileName);
        editor.putString(PREFERENCE_FORMAT, getFormat());
        if (mCustomEncoding.isChecked()) {
            editor.putString(PREFERENCE_ENCODING, ((Encoding) mSpinnerEncoding.getSelectedItem()).name());
        }
        editor.putBoolean(PREFERENCE_USE_CUSTOM_ENCODING, mCustomEncoding.isChecked());
        editor.apply();

        return fileName;
    }

    public String getFormat() {
        int id = mSpinner.getSelectedItemPosition();
        if (id != Spinner.INVALID_POSITION) {
            return mFormatValues[id];
        }
        return DEFAULT_FORMAT;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        // Let's not let the user mess around while we're busy.
        if (!smHasWorkerThread) {
            menu.add(0, MENU_SETTINGS, 0, R.string.menu_settings).setShortcut(
                    '1', 's').setIcon(android.R.drawable.ic_menu_preferences);
        } else {
            menu.add(0, MENU_CANCEL, 0, R.string.menu_cancel).setShortcut(
                    '1', 'c').setIcon(android.R.drawable.ic_menu_close_clear_cancel);
        }

        return true;
    }

    @Override
    public void onResume() {
        super.onResume();

        displayImportPolicy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_SETTINGS:
                Intent intent = new Intent(this, PreferenceActivity.class);
                startActivity(intent);
                break;
            case MENU_CANCEL:
                smHasWorkerThread = false;
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected Dialog onCreateDialog(int id) {

        switch (id) {
            case DIALOG_ID_WARN_OVERWRITE:
                LayoutInflater inflater = LayoutInflater.from(this);
                View view = inflater.inflate(R.layout.file_exists, null);
                final CheckBox cb = (CheckBox) view
                        .findViewById(R.id.dont_ask_again);
                return new AlertDialog.Builder(this).setView(view).setPositiveButton(
                        android.R.string.yes, new OnClickListener() {

                            public void onClick(DialogInterface dialog, int which) {

                                saveBooleanPreference(PreferenceActivity.PREFS_ASK_IF_FILE_EXISTS, !cb.isChecked());
                                finish();

                            }


                        }).setNegativeButton(android.R.string.no, new OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        // Cancel should not do anything.

                        //saveBooleanPreference(PreferenceActivity.PREFS_ASK_IF_FILE_EXISTS, !cb.isChecked());
                        //finish();
                    }

                }).create();

            case DIALOG_ID_WARN_RESTORE_POLICY:
                return new AlertDialog.Builder(this)
                        .setTitle(R.string.warn_restore_policy_title)
                        .setMessage(R.string.warn_restore_policy)
                        .setPositiveButton(android.R.string.yes, new OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                startImportPostCheck();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .create();

            case DIALOG_ID_NO_FILE_MANAGER_AVAILABLE:
                return new DownloadOIAppDialog(this,
                        DownloadOIAppDialog.OI_FILEMANAGER);
            case DIALOG_ID_PERMISSIONS:
                return new AlertDialog.Builder(this)
                        .setTitle(R.string.warn_install_order_title)
                        .setMessage(R.string.warn_install_order)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            dialog.dismiss();
                        })
                        .create();
        }
        return super.onCreateDialog(id);
    }


    /**
     * @param preference
     * @param value
     */
    private void saveBooleanPreference(String preference, boolean value) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this);
        Editor editor = prefs.edit();
        editor.putBoolean(preference, value);
        editor.apply();
        doExport();
    }

    private void openFileManagerForNewDocument() {

        String fileName = mFileNameView.getText().toString();
        String filePath = mFilePathView.getText().toString();
        if (TextUtils.isEmpty(filePath)) {
            fileName = DEFAULT_FILENAME;
        }
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/*");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);

        try {
            startActivityForResult(intent, REQUEST_CODE_PICK_FILE);
        } catch (ActivityNotFoundException e) {
            showDialog(DIALOG_ID_NO_FILE_MANAGER_AVAILABLE);
        }
    }

    private void openFileManagerForChoosingDocument() {

        String fileName = mFilePathView.getText().toString();

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        intent.addCategory(Intent.CATEGORY_OPENABLE);

        intent.setType("text/*");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);

        try {
            startActivityForResult(intent, REQUEST_CODE_PICK_FILE);
        } catch (ActivityNotFoundException e) {
            showDialog(DIALOG_ID_NO_FILE_MANAGER_AVAILABLE);
        }
    }

    /**
     * Prepends the system's SD card path to the file name.
     *
     * @param filename
     * @return
     */
    protected String getSdCardFilename(String filename) {
        String sdpath = android.os.Environment
                .getExternalStorageDirectory().getAbsolutePath();
        String path;
        if (sdpath.substring(sdpath.length() - 1, sdpath.length()).equals("/")) {
            path = sdpath + filename;
        } else {
            path = sdpath + "/" + filename;
        }
        return path;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, "onActivityResult");

        switch (requestCode) {
            case REQUEST_CODE_PICK_FILE:
                if (resultCode == RESULT_OK && data != null) {
                    Uri documentUri = data.getData();
                    if (documentUri != null) {
                        setFileUri(documentUri);
                    } else {
                        setFileUriUnknown();
                    }

                }
                break;
        }
    }

    private void setFileUriUnknown() {
        mFileNameView.setText(getString(R.string.unknown_document));
        mFilePathView.setText("");
    }

    private void setFileUri(Uri documentUri) {
        mFileNameView.setText(getDocumentName(documentUri));
        mFilePathView.setText(documentUri.toString());
    }
}
