package org.openintents.shopping.automation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import org.openintents.intents.AutomationIntents
import org.openintents.intents.ShoppingListIntents
import org.openintents.shopping.LogConstants
import org.openintents.shopping.R
import org.openintents.shopping.library.provider.ShoppingContract

open class EditAutomationActivity : Activity() {

    private lateinit var mTextCommand: TextView
    // private lateinit var mTextSelectAction: TextView
    // private lateinit var mTextSelectCountdown: TextView
    private lateinit var mSpinnerAction: Spinner
    private lateinit var mButtonOk: Button
    private lateinit var mButtonCountdown: Button

    private var mDescriptionAction: String? = null
    private var mDescriptionShoppingList: String? = null

    private var mUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_edit_automation)

        mUri = null
        mDescriptionShoppingList = "?"

        mSpinnerAction = findViewById<Spinner>(R.id.spinner_action)
        val adapter = ArrayAdapter.createFromResource(
            this, R.array.automation_actions,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mSpinnerAction.adapter = adapter

        mSpinnerAction.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                mDescriptionAction = resources.getStringArray(
                    R.array.automation_actions
                )[position]

                updateTextViews()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
            }
        }

        mButtonCountdown = findViewById<Button>(R.id.button_list)
        mButtonCountdown.setOnClickListener {
            pickList()
        }

        mButtonOk = findViewById<Button>(R.id.button_ok)
        mButtonOk.setOnClickListener {
            doOk()
        }

        mButtonOk.isEnabled = false

        val b = findViewById<Button>(R.id.button_cancel)
        b.setOnClickListener {
            doCancel()
        }

        mTextCommand = findViewById<TextView>(R.id.command)
        // mTextSelectAction = findViewById(R.id.select_action)
        // mTextSelectCountdown = findViewById(R.id.select_countdown)

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(BUNDLE_ACTION)) {
                val i = savedInstanceState.getInt(BUNDLE_ACTION)
                mSpinnerAction.setSelection(i)
            }
            if (savedInstanceState.containsKey(BUNDLE_LIST_URI)) {
                mUri = Uri.parse(savedInstanceState.getString(BUNDLE_LIST_URI))
                setListNameFromUri()
            }
        } else {
            val intent = intent

            if (intent != null) {
                val action = intent.getStringExtra(ShoppingListIntents.EXTRA_ACTION)

                if (ShoppingListIntents.TASK_CLEAN_UP_LIST == action) {
                    mSpinnerAction.setSelection(0)
                } else {
                    // set default
                    mSpinnerAction.setSelection(0)
                }

                // Get list:
                val dataString = intent.getStringExtra(ShoppingListIntents.EXTRA_DATA)
                if (dataString != null) {
                    mUri = Uri.parse(dataString)
                }
                setListNameFromUri()
            }
        }

        updateTextViews()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        if (debug) {
            Log.i(TAG, "onPause")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(BUNDLE_ACTION, mSpinnerAction.selectedItemPosition)
        if (mUri != null) {
            outState.putString(BUNDLE_LIST_URI, mUri.toString())
        }
    }

    internal fun pickList() {
        val i = Intent(Intent.ACTION_PICK)
        i.data = ShoppingContract.Lists.CONTENT_URI

        startActivityForResult(i, REQUEST_CODE_PICK_LIST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        if (requestCode == REQUEST_CODE_PICK_LIST && resultCode == RESULT_OK) {
            mUri = intent?.data

            setListNameFromUri()
        }

        updateTextViews()
    }

    private fun setListNameFromUri() {
        mDescriptionShoppingList = ""

        if (mUri != null) {
            mButtonOk.isEnabled = true

            // Get name of list from content provider
            val c = contentResolver.query(
                mUri!!,
                arrayOf(ShoppingContract.Lists._ID, ShoppingContract.Lists.NAME),
                null, null, null
            )

            if (c != null && c.moveToFirst()) {
                mDescriptionShoppingList = c.getString(1)
            }

            c?.close()
        }

        if (TextUtils.isEmpty(mDescriptionShoppingList)) {
            mDescriptionShoppingList = getString(android.R.string.untitled)
        }
    }

    internal fun doOk() {
        updateResult()
        finish()
    }

    internal fun doCancel() {
        setResult(RESULT_CANCELED)
        finish()
    }

    internal fun updateResult() {
        val intent = Intent()

        val id = mSpinnerAction.selectedItemId
        if (id == 0L) {
            intent.putExtra(
                ShoppingListIntents.EXTRA_ACTION,
                ShoppingListIntents.TASK_CLEAN_UP_LIST
            )
        }
        intent.putExtra(ShoppingListIntents.EXTRA_DATA, mUri.toString())

        val description = mDescriptionAction + ": " + mDescriptionShoppingList
        intent.putExtra(AutomationIntents.EXTRA_DESCRIPTION, description)

        if (debug) {
            Log.i(TAG, "Created intent (URI)   : " + intent.toURI())
        }
        if (debug) {
            Log.i(TAG, "Created intent (String): " + intent.toString())
        }

        setResult(RESULT_OK, intent)
    }

    internal fun updateTextViews() {
        mTextCommand.text = mDescriptionAction + ": " + mDescriptionShoppingList
        // mTextSelectAction.setText(getString(R.string.select_action,
        // mDescriptionAction))
        // mTextSelectCountdown.setText(getString(R.string.select_countdown,
        // mDescriptionCountdown))
        // mTextSelectAction.setText(getString(R.string.select_action, ""))
        // mTextSelectCountdown.setText(getString(R.string.select_countdown,
        // ""))
        mButtonCountdown.text = mDescriptionShoppingList
    }

    companion object {
        private val TAG = LogConstants.TAG
        private val debug = false || LogConstants.debug

        private const val REQUEST_CODE_PICK_LIST = 1

        private const val BUNDLE_ACTION = "action"
        private const val BUNDLE_LIST_URI = "list"
    }
}
