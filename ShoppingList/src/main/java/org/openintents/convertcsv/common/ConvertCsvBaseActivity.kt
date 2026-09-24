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

package org.openintents.convertcsv.common

import android.app.AlertDialog
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.ParcelFileDescriptor
import android.preference.PreferenceManager
import android.provider.OpenableColumns
import android.text.TextUtils
import android.util.Log
import android.util.Xml.Encoding
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.openintents.convertcsv.PreferenceActivity
import org.openintents.distribution.DownloadOIAppDialog
import org.openintents.shopping.R
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.Reader
import java.io.Writer

open class ConvertCsvBaseActivity : AppCompatActivity() {

    // Protected instance fields accessed/assigned by ConvertCsvActivity
    protected var mFilePathView: TextView? = null
    protected var mFileNameView: TextView? = null
    @JvmField protected var mConvertInfo: TextView? = null
    @JvmField protected var mSpinner: Spinner? = null
    @JvmField protected var PREFERENCE_FILENAME: String? = null
    @JvmField protected var DEFAULT_FILENAME: String? = null
    @JvmField protected var PREFERENCE_FORMAT: String? = null
    @JvmField protected var DEFAULT_FORMAT: String? = null
    @JvmField protected var PREFERENCE_ENCODING: String? = null
    @JvmField protected var PREFERENCE_USE_CUSTOM_ENCODING: String? = null
    @JvmField protected var RES_STRING_FILEMANAGER_TITLE: Int = 0
    @JvmField protected var RES_STRING_FILEMANAGER_BUTTON_TEXT: Int = 0
    @JvmField protected var RES_ARRAY_CSV_FILE_FORMAT: Int = 0
    @JvmField protected var RES_ARRAY_CSV_FILE_FORMAT_VALUE: Int = 0

    // Package-private in Java (no modifier) — keep as internal for same-module access
    internal var mFormatValues: Array<String>? = null

    // Message handler that receives status messages from the CSV import/export thread.
    private val mHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MESSAGE_SET_PROGRESS -> this@ConvertCsvBaseActivity.setConversionProgress(msg.arg1)
                MESSAGE_SET_MAX_PROGRESS -> this@ConvertCsvBaseActivity.setMaxProgress(msg.arg1)
                MESSAGE_SUCCESS -> this@ConvertCsvBaseActivity.displayMessage(msg.arg1, true)
                MESSAGE_ERROR -> this@ConvertCsvBaseActivity.displayMessage(msg.arg1, false)
            }
        }
    }

    private var mSpinnerEncoding: Spinner? = null
    private var mCustomEncoding: CheckBox? = null

    private val mCustomEncodingListener = android.widget.CompoundButton.OnCheckedChangeListener { _, isChecked ->
        mSpinnerEncoding?.isEnabled = isChecked
    }

    companion object {
        // Public static final int constants -> const val in companion object
        const val IMPORT_POLICY_DUPLICATE: Int = 0
        const val IMPORT_POLICY_KEEP: Int = 1
        const val IMPORT_POLICY_OVERWRITE: Int = 2
        const val IMPORT_POLICY_RESTORE: Int = 3
        const val IMPORT_POLICY_MAX: Int = IMPORT_POLICY_RESTORE
        const val MESSAGE_SET_PROGRESS: Int = 1
        const val MESSAGE_SUCCESS: Int = 2
        const val MESSAGE_ERROR: Int = 3
        const val MESSAGE_SET_MAX_PROGRESS: Int = 4

        // Protected static final int constants
        protected const val MENU_SETTINGS: Int = Menu.FIRST + 1
        protected const val MENU_CANCEL: Int = Menu.FIRST + 2
        protected const val MENU_DISTRIBUTION_START: Int = Menu.FIRST + 100 // MUST BE LAST
        protected const val DIALOG_ID_WARN_OVERWRITE: Int = 1
        protected const val DIALOG_ID_NO_FILE_MANAGER_AVAILABLE: Int = 2
        protected const val DIALOG_ID_WARN_RESTORE_POLICY: Int = 3
        protected const val DIALOG_ID_PERMISSIONS: Int = 4
        protected const val DIALOG_DISTRIBUTION_START: Int = 100 // MUST BE LAST
        protected const val REQUEST_CODE_PICK_FILE: Int = 1

        private const val TAG = "ConvertCsvBaseActivity"

        // static protected Handler — non-const static field -> @JvmField
        @JvmField protected var smCurrentHandler: Handler? = null

        // Package-private static fields (no modifier in Java)
        @JvmField internal var smHasWorkerThread: Boolean = false
        @JvmField internal var smProgressMax: Int = 0

        // Private static method (used only internally)
        private fun findString(array: Array<String>, string: String): Int {
            for (i in array.indices) {
                if (string == array[i]) {
                    return i
                }
            }
            return -1
        }

        // Public static methods -> @JvmStatic fun
        @JvmStatic
        fun dispatchSuccess(successMsg: Int) {
            dispatchMessage(MESSAGE_SUCCESS, successMsg)
        }

        @JvmStatic
        fun dispatchError(errorMsg: Int) {
            dispatchMessage(MESSAGE_ERROR, errorMsg)
        }

        @JvmStatic
        fun dispatchConversionProgress(newProgress: Int) {
            dispatchMessage(MESSAGE_SET_PROGRESS, newProgress)
        }

        @JvmStatic
        fun dispatchSetMaxProgress(maxProgress: Int) {
            dispatchMessage(MESSAGE_SET_MAX_PROGRESS, maxProgress)
        }

        // Package-private static method in Java
        @JvmStatic
        internal fun dispatchMessage(what: Int, argument: Int) {
            // Cache the handler since the other thread could modify it at any time.
            val handler = smCurrentHandler
            if (handler != null) {
                val msg = Message.obtain(handler, what, argument, 0)
                handler.sendMessage(msg)
            }
        }
    }

    /**
     * Called when the activity is first created.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Always create the main layout first, since we need to populate the
        // variables with all the views.
        switchToMainLayout()
        if (smHasWorkerThread) {
            switchToConvertLayout()
        }
    }

    private fun switchToMainLayout() {
        setContentView(R.layout.convert)

        DEFAULT_FILENAME = getString(R.string.default_filename)

        setPreferencesUsed()

        mFilePathView = findViewById(R.id.file_path)
        mFileNameView = findViewById(R.id.file_name)

        val pm: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val filepath = pm.getString(PREFERENCE_FILENAME, "")

        if (TextUtils.isEmpty(filepath)) {
            setFileUriUnknown()
        } else {
            setFileUri(Uri.parse(filepath))
        }

        var buttonFileManager: ImageButton = findViewById(R.id.new_document)
        buttonFileManager.setOnClickListener { openFileManagerForNewDocument() }

        buttonFileManager = findViewById(R.id.open_document)
        buttonFileManager.setOnClickListener { openFileManagerForChoosingDocument() }

        mConvertInfo = findViewById(R.id.convert_info)

        val buttonImport: Button = findViewById(R.id.file_import)
        buttonImport.setOnClickListener { startImport() }

        val buttonExport: Button = findViewById(R.id.file_export)
        buttonExport.setOnClickListener { startExport() }

        mSpinner = findViewById<Spinner>(R.id.spinner1)
        val adapter = ArrayAdapter.createFromResource(
            this, RES_ARRAY_CSV_FILE_FORMAT, android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mSpinner!!.adapter = adapter

        mFormatValues = resources.getStringArray(RES_ARRAY_CSV_FILE_FORMAT_VALUE)

        setSpinner(pm.getString(PREFERENCE_FORMAT, DEFAULT_FORMAT))

        // set encoding spinner
        mSpinnerEncoding = findViewById<Spinner>(R.id.spinner_encoding)
        val adapterEncoding = EncodingAdapter(this, android.R.layout.simple_spinner_item)
        adapterEncoding.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mSpinnerEncoding!!.adapter = adapterEncoding

        var encodingString = getDefaultEncoding().name
        try {
            encodingString = pm.getString(PREFERENCE_ENCODING, encodingString) ?: encodingString
        } catch (ignored: ClassCastException) {
        }

        val encoding: Encoding = try {
            Encoding.valueOf(encodingString)
        } catch (e: IllegalArgumentException) {
            Encoding.UTF_8
        }
        val encodingPosition = adapterEncoding.getPosition(encoding)
        if (encodingPosition != Spinner.INVALID_POSITION) {
            mSpinnerEncoding!!.setSelection(encodingPosition)
        }

        // set encoding checkbox
        mCustomEncoding = findViewById<CheckBox>(R.id.custom_encoding)
        mCustomEncoding!!.setOnCheckedChangeListener(mCustomEncodingListener)
        mCustomEncoding!!.isChecked = pm.getBoolean(PREFERENCE_USE_CUSTOM_ENCODING, false)

        val intent = intent
        val type = intent.type
        if (type != null && type == "text/csv") {
            // Someone wants to import a CSV document through the file manager.
            // Set the path accordingly:
            val path = getIntent().data
            if (path != null) {
                setFileUri(path)
            } else {
                setFileUriUnknown()
            }
        }
    }

    private fun switchToConvertLayout() {
        setContentView(R.layout.convertprogress)
        (findViewById<ProgressBar>(R.id.Progress)).max = smProgressMax
        smCurrentHandler = mHandler
    }

    override fun onDestroy() {
        super.onDestroy()
        // The worker thread is on its own now.
        smCurrentHandler = null
    }

    open fun setSpinner(value: String?) {
        // get the ID:
        val id = findString(mFormatValues ?: return, value ?: return)
        if (id != -1) {
            mSpinner!!.setSelection(id)
        }
    }

    open fun setPreferencesUsed() {
    }

    /**
     * Display the current import policy.
     */
    open fun displayImportPolicy() {
        val importPolicy = getValidatedImportPolicy()

        val policyStrings = resources.getStringArray(R.array.import_policy_detail)

        val policyView = findViewById<TextView>(R.id.import_policy_detail)

        policyView?.setText(policyStrings[importPolicy])
    }

    open fun getValidatedImportPolicy(): Int {
        val prefKey = getImportPolicyPrefString()

        if (prefKey == null) {
            // This activity does not support import policies.
            return IMPORT_POLICY_DUPLICATE
        }

        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        val importPolicy = prefs.getString(prefKey, getDefaultImportPolicy())

        return try {
            val policy = Integer.parseInt(importPolicy ?: "0")

            if (policy < 0 || policy > IMPORT_POLICY_MAX) {
                return 0
            }

            policy
        } catch (e: NumberFormatException) {
            // Invalid prefs.
            0
        }
    }

    /**
     * @return The string that identifies the import policy for this importer.
     * null if this derived activity does not support import policies.
     */
    open fun getImportPolicyPrefString(): String? {
        return null
    }

    /**
     * @return The default import policy
     */
    open fun getDefaultImportPolicy(): String {
        return "" + IMPORT_POLICY_DUPLICATE
    }

    open fun startImport() {
        val importPolicy = getValidatedImportPolicy()

        if (importPolicy == IMPORT_POLICY_RESTORE) {
            @Suppress("DEPRECATION")
            showDialog(DIALOG_ID_WARN_RESTORE_POLICY)
        } else {
            startImportPostCheck()
        }
    }

    open fun startImportPostCheck() {
        val fileName = getFilenameAndSavePreferences()

        Log.i(TAG, "Importing...$fileName")

        val file = Uri.parse(fileName)

        // If this is the RESTORE policy, make sure we let the user know
        // what kind of trouble he's getting himself into.
        switchToConvertLayout()
        smHasWorkerThread = true
        supportInvalidateOptionsMenu()

        Thread {
            try {
                val inputStream: InputStream = contentResolver.openInputStream(file)!!

                val reader: Reader

                val enc = getCurrentEncoding()
                reader = if (enc == null) {
                    InputStreamReader(inputStream)
                } else {
                    InputStreamReader(inputStream, enc.name)
                }

                val size = getDocumentSize(file)
                if (size > 0) {
                    smProgressMax = size
                    (findViewById<ProgressBar>(R.id.Progress)).max = smProgressMax
                }

                doImport(reader)

                reader.close()
                dispatchSuccess(R.string.import_finished)
                onImportFinished()

            } catch (e: FileNotFoundException) {
                dispatchError(R.string.error_file_not_found)
                Log.i(TAG, "File not found", e)
            } catch (e: IOException) {
                dispatchError(R.string.error_reading_file)
                Log.i(TAG, "IO exception", e)
            } catch (e: WrongFormatException) {
                dispatchError(R.string.wrong_csv_format)
                Log.i(TAG, "array index out of bounds", e)
            } catch (e: RuntimeException) {
                dispatchError(R.string.error_reading_file)
                Log.e(TAG, "Import failed", e)
            }

            smHasWorkerThread = false
            supportInvalidateOptionsMenu()
        }.start()
    }

    open fun getDocumentSize(uri: Uri): Int {
        val cursor = try {
            contentResolver.query(uri, null, null, null, null, null)
        } catch (e: RuntimeException) {
            // e.g. SecurityException if the URI permission is no longer granted
            Log.w(TAG, "Cannot query document size", e)
            null
        }

        var size = -1
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val displayName = cursor.getString(
                    cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                )
                Log.i(TAG, "Display Name: $displayName")

                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    size = cursor.getInt(sizeIndex)
                }
            }
        } finally {
            cursor?.close()
        }
        return size
    }

    open fun getDocumentName(uri: Uri): String {
        val cursor = try {
            contentResolver.query(uri, null, null, null, null, null)
        } catch (e: RuntimeException) {
            // e.g. SecurityException if the URI permission is no longer granted
            Log.w(TAG, "Cannot query document name", e)
            null
        }

        var displayName: String? = uri.lastPathSegment
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    displayName = cursor.getString(nameIndex)
                }
            }
        } finally {
            cursor?.close()
        }
        return displayName ?: uri.lastPathSegment ?: ""
    }

    protected open fun getCurrentEncoding(): Encoding? {
        return if (mCustomEncoding!!.isChecked) {
            mSpinnerEncoding!!.selectedItem as Encoding
        } else {
            getDefaultEncoding()
        }
    }

    protected open fun getDefaultEncoding(): Encoding {
        return Encoding.UTF_8
    }

    internal fun displayMessage(message: Int, success: Boolean) {
        AlertDialog.Builder(this)
            .setIcon(if (success) android.R.drawable.ic_dialog_info else android.R.drawable.ic_dialog_alert)
            .setMessage(message)
            .setPositiveButton(R.string.dialog_ok) { _, _ -> finish() }
            .show()
    }

    internal fun setConversionProgress(newProgress: Int) {
        (findViewById<ProgressBar>(R.id.Progress)).progress = newProgress
    }

    internal fun setMaxProgress(maxProgress: Int) {
        (findViewById<ProgressBar>(R.id.Progress)).max = maxProgress
    }

    /**
     * @param reader
     * @throws IOException
     */
    @Throws(IOException::class, WrongFormatException::class)
    open fun doImport(reader: Reader) {
    }

    open fun onImportFinished() {
    }

    open fun startExport() {
        Log.i(TAG, "Exporting...")
        doExport()
    }

    open fun doExport() {
        val fileName = getFilenameAndSavePreferences()
        val file = Uri.parse(fileName)

        switchToConvertLayout()
        smHasWorkerThread = true

        Thread {
            try {
                val writer: Writer
                val enc = getCurrentEncoding()
                val pfd: ParcelFileDescriptor = contentResolver.openFileDescriptor(file, "wt")!!
                writer = if (enc == null) {
                    OutputStreamWriter(FileOutputStream(pfd.fileDescriptor))
                } else {
                    OutputStreamWriter(FileOutputStream(pfd.fileDescriptor), enc.name)
                }

                doExport(writer)

                writer.close()
                pfd.close()
                dispatchSuccess(R.string.export_finished)

            } catch (e: IOException) {
                dispatchError(R.string.error_writing_file)
                Log.i(TAG, "IO exception", e)
            } catch (e: RuntimeException) {
                dispatchError(R.string.error_writing_file)
                Log.e(TAG, "Export failed", e)
            }

            smHasWorkerThread = false
        }.start()
    }

    /**
     * @param writer
     * @throws IOException
     */
    @Throws(IOException::class)
    open fun doExport(writer: Writer) {
    }

    /**
     * @return
     */
    open fun getFilenameAndSavePreferences(): String {
        val fileName = mFilePathView!!.text.toString()

        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = prefs.edit()
        editor.putString(PREFERENCE_FILENAME, fileName)
        editor.putString(PREFERENCE_FORMAT, getFormat())
        if (mCustomEncoding!!.isChecked) {
            editor.putString(PREFERENCE_ENCODING, (mSpinnerEncoding!!.selectedItem as Encoding).name)
        }
        editor.putBoolean(PREFERENCE_USE_CUSTOM_ENCODING, mCustomEncoding!!.isChecked)
        editor.apply()

        return fileName
    }

    open fun getFormat(): String? {
        val id = mSpinner!!.selectedItemPosition
        if (id != Spinner.INVALID_POSITION) {
            return mFormatValues!![id]
        }
        return DEFAULT_FORMAT
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        // Let's not let the user mess around while we're busy.
        if (!smHasWorkerThread) {
            menu.add(0, MENU_SETTINGS, 0, R.string.menu_settings)
                .setShortcut('1', 's')
                .setIcon(android.R.drawable.ic_menu_preferences)
        } else {
            menu.add(0, MENU_CANCEL, 0, R.string.menu_cancel)
                .setShortcut('1', 'c')
                .setIcon(android.R.drawable.ic_menu_close_clear_cancel)
        }

        return true
    }

    override fun onResume() {
        super.onResume()
        displayImportPolicy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            MENU_SETTINGS -> {
                val intent = Intent(this, PreferenceActivity::class.java)
                startActivity(intent)
            }
            MENU_CANCEL -> {
                smHasWorkerThread = false
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @Suppress("DEPRECATION")
    override fun onCreateDialog(id: Int): Dialog? {
        when (id) {
            DIALOG_ID_WARN_OVERWRITE -> {
                val inflater = LayoutInflater.from(this)
                val view = inflater.inflate(R.layout.file_exists, null)
                val cb = view.findViewById<CheckBox>(R.id.dont_ask_again)
                return AlertDialog.Builder(this)
                    .setView(view)
                    .setPositiveButton(android.R.string.yes) { _, _ ->
                        saveBooleanPreference(PreferenceActivity.PREFS_ASK_IF_FILE_EXISTS, !cb.isChecked)
                        finish()
                    }
                    .setNegativeButton(android.R.string.no) { _, _ ->
                        // Cancel should not do anything.
                    }
                    .create()
            }

            DIALOG_ID_WARN_RESTORE_POLICY -> {
                return AlertDialog.Builder(this)
                    .setTitle(R.string.warn_restore_policy_title)
                    .setMessage(R.string.warn_restore_policy)
                    .setPositiveButton(android.R.string.yes) { _, _ ->
                        startImportPostCheck()
                    }
                    .setNegativeButton(android.R.string.no, null)
                    .create()
            }

            DIALOG_ID_NO_FILE_MANAGER_AVAILABLE -> {
                return DownloadOIAppDialog(this, DownloadOIAppDialog.OI_FILEMANAGER)
            }

            DIALOG_ID_PERMISSIONS -> {
                return AlertDialog.Builder(this)
                    .setTitle(R.string.warn_install_order_title)
                    .setMessage(R.string.warn_install_order)
                    .setPositiveButton(android.R.string.ok) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()
            }
        }
        @Suppress("DEPRECATION")
        return super.onCreateDialog(id)
    }

    /**
     * @param preference
     * @param value
     */
    private fun saveBooleanPreference(preference: String, value: Boolean) {
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = prefs.edit()
        editor.putBoolean(preference, value)
        editor.apply()
        doExport()
    }

    private fun openFileManagerForNewDocument() {
        val fileName: String
        val filePath = mFilePathView!!.text.toString()
        fileName = if (TextUtils.isEmpty(filePath)) {
            DEFAULT_FILENAME ?: ""
        } else {
            mFileNameView!!.text.toString()
        }
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "text/*"
        intent.putExtra(Intent.EXTRA_TITLE, fileName)

        try {
            @Suppress("DEPRECATION")
            startActivityForResult(intent, REQUEST_CODE_PICK_FILE)
        } catch (e: ActivityNotFoundException) {
            @Suppress("DEPRECATION")
            showDialog(DIALOG_ID_NO_FILE_MANAGER_AVAILABLE)
        }
    }

    private fun openFileManagerForChoosingDocument() {
        val fileName = mFilePathView!!.text.toString()

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "text/*"
        intent.putExtra(Intent.EXTRA_TITLE, fileName)

        try {
            @Suppress("DEPRECATION")
            startActivityForResult(intent, REQUEST_CODE_PICK_FILE)
        } catch (e: ActivityNotFoundException) {
            @Suppress("DEPRECATION")
            showDialog(DIALOG_ID_NO_FILE_MANAGER_AVAILABLE)
        }
    }

    /**
     * Prepends the system's SD card path to the file name.
     *
     * @param filename
     * @return
     */
    protected open fun getSdCardFilename(filename: String): String {
        val sdpath = android.os.Environment.getExternalStorageDirectory().absolutePath
        return if (sdpath.substring(sdpath.length - 1, sdpath.length) == "/") {
            sdpath + filename
        } else {
            "$sdpath/$filename"
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data)
        Log.i(TAG, "onActivityResult")

        when (requestCode) {
            REQUEST_CODE_PICK_FILE -> {
                if (resultCode == RESULT_OK && data != null) {
                    val documentUri = data.data
                    if (documentUri != null) {
                        // Keep access to the document across reboots.
                        try {
                            contentResolver.takePersistableUriPermission(
                                documentUri,
                                data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                            )
                        } catch (e: RuntimeException) {
                            // No persistable grant offered (e.g. ACTION_GET_CONTENT providers).
                            Log.w(TAG, "Could not persist URI permission", e)
                        }
                        setFileUri(documentUri)
                    } else {
                        setFileUriUnknown()
                    }
                }
            }
        }
    }

    private fun setFileUriUnknown() {
        mFileNameView!!.setText(getString(R.string.unknown_document))
        mFilePathView!!.setText("")
    }

    private fun setFileUri(documentUri: Uri) {
        mFileNameView!!.setText(getDocumentName(documentUri))
        mFilePathView!!.setText(documentUri.toString())
    }
}
