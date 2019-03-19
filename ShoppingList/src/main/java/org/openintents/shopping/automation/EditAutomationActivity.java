package org.openintents.shopping.automation;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import org.openintents.intents.AutomationIntents;
import org.openintents.intents.ShoppingListIntents;
import org.openintents.shopping.LogConstants;
import org.openintents.shopping.R;
import org.openintents.shopping.library.provider.ShoppingContract;

public class EditAutomationActivity extends Activity {

    private static final String TAG = LogConstants.TAG;
    private static final boolean debug = false || LogConstants.debug;

    private static final int REQUEST_CODE_PICK_LIST = 1;

    private static final String BUNDLE_ACTION = "action";
    private static final String BUNDLE_LIST_URI = "list";

    private TextView mTextCommand;
    // TextView mTextSelectAction;
    // TextView mTextSelectCountdown;
    private Spinner mSpinnerAction;
    private Button mButtonOk;
    private Button mButtonCountdown;

    private String mDescriptionAction;
    private String mDescriptionShoppingList;

    private Uri mUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_edit_automation);

        mUri = null;
        mDescriptionShoppingList = "?";

        mSpinnerAction = (Spinner) findViewById(R.id.spinner_action);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.automation_actions,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerAction.setAdapter(adapter);

        mSpinnerAction
                .setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(AdapterView<?> parent,
                                               View view, int position, long id) {
                        mDescriptionAction = getResources().getStringArray(
                                R.array.automation_actions)[position];

                        updateTextViews();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }

                });

        mButtonCountdown = (Button) findViewById(R.id.button_list);
        mButtonCountdown.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                pickList();
            }

        });

        mButtonOk = (Button) findViewById(R.id.button_ok);
        mButtonOk.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                doOk();
            }

        });

        mButtonOk.setEnabled(false);

        Button b = (Button) findViewById(R.id.button_cancel);
        b.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                doCancel();
            }

        });

        mTextCommand = (TextView) findViewById(R.id.command);
        // mTextSelectAction = (TextView) findViewById(R.id.select_action);
        // mTextSelectCountdown = (TextView)
        // findViewById(R.id.select_countdown);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(BUNDLE_ACTION)) {
                int i = savedInstanceState.getInt(BUNDLE_ACTION);
                mSpinnerAction.setSelection(i);
            }
            if (savedInstanceState.containsKey(BUNDLE_LIST_URI)) {
                mUri = Uri.parse(savedInstanceState.getString(BUNDLE_LIST_URI));
                setListNameFromUri();
            }
        } else {
            final Intent intent = getIntent();

            if (intent != null) {
                String action = intent
                        .getStringExtra(ShoppingListIntents.EXTRA_ACTION);

                if (ShoppingListIntents.TASK_CLEAN_UP_LIST.equals(action)) {
                    mSpinnerAction.setSelection(0);
                } else {
                    // set default
                    mSpinnerAction.setSelection(0);
                }

                // Get list:

                final String dataString = intent
                        .getStringExtra(ShoppingListIntents.EXTRA_DATA);
                if (dataString != null) {
                    mUri = Uri.parse(dataString);
                }
                setListNameFromUri();
            }
        }

        updateTextViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (debug) {
            Log.i(TAG, "onPause");
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(BUNDLE_ACTION, mSpinnerAction.getSelectedItemPosition());
        if (mUri != null) {
            outState.putString(BUNDLE_LIST_URI, mUri.toString());
        }
    }

    void pickList() {
        Intent i = new Intent(Intent.ACTION_PICK);
        i.setData(ShoppingContract.Lists.CONTENT_URI);

        startActivityForResult(i, REQUEST_CODE_PICK_LIST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == REQUEST_CODE_PICK_LIST && resultCode == RESULT_OK) {
            mUri = intent.getData();

            setListNameFromUri();
        }

        updateTextViews();
    }

    private void setListNameFromUri() {
        mDescriptionShoppingList = "";

        if (mUri != null) {
            mButtonOk.setEnabled(true);

            // Get name of list from content provider
            Cursor c = getContentResolver().query(
                    mUri,
                    new String[]{ShoppingContract.Lists._ID,
                            ShoppingContract.Lists.NAME}, null, null, null
            );

            if (c != null && c.moveToFirst()) {
                mDescriptionShoppingList = c.getString(1);
            }

            c.close();
        }

        if (TextUtils.isEmpty(mDescriptionShoppingList)) {
            mDescriptionShoppingList = getString(android.R.string.untitled);
        }
    }

    void doOk() {
        updateResult();
        finish();
    }

    void doCancel() {
        setResult(RESULT_CANCELED);
        finish();
    }

    void updateResult() {
        Intent intent = new Intent();

        long id = mSpinnerAction.getSelectedItemId();
        if (id == 0) {
            intent.putExtra(ShoppingListIntents.EXTRA_ACTION,
                    ShoppingListIntents.TASK_CLEAN_UP_LIST);
        }
        intent.putExtra(ShoppingListIntents.EXTRA_DATA, mUri.toString());

        String description = mDescriptionAction + ": "
                + mDescriptionShoppingList;
        intent.putExtra(AutomationIntents.EXTRA_DESCRIPTION, description);

        if (debug) {
            Log.i(TAG, "Created intent (URI)   : " + intent.toURI());
        }
        if (debug) {
            Log.i(TAG, "Created intent (String): " + intent.toString());
        }

        setResult(RESULT_OK, intent);
    }

    void updateTextViews() {
        mTextCommand.setText(mDescriptionAction + ": "
                + mDescriptionShoppingList);
        // mTextSelectAction.setText(getString(R.string.select_action,
        // mDescriptionAction));
        // mTextSelectCountdown.setText(getString(R.string.select_countdown,
        // mDescriptionCountdown));
        // mTextSelectAction.setText(getString(R.string.select_action, ""));
        // mTextSelectCountdown.setText(getString(R.string.select_countdown,
        // ""));
        mButtonCountdown.setText(mDescriptionShoppingList);
    }
}
