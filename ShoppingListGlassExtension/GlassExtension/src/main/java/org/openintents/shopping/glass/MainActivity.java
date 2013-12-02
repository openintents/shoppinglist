package org.openintents.shopping.glass;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;
import com.google.api.client.util.IOUtils;

import org.apache.http.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private static final boolean debug = true;

    private static final String PARAM_AUTH_TOKEN = "AUTH_TOKEN";

    public static final String PREF_OAUTH_TOKEN = "OAUTH_TOKEN";
    public static final String PREF_LAST_MIRROR_ID = "LAST_MIRROR_ID";

    private static final int REQUEST_ACCOUNT_PICKER = 1;
    private static final int REQUEST_AUTHORIZATION = 2;

    private static final String GLASS_TIMELINE_SCOPE =
            "https://www.googleapis.com/auth/glass.timeline";
    private static final String GLASS_LOCATION_SCOPE =
            "https://www.googleapis.com/auth/glass.location";
    private static final String SCOPE = String.format("oauth2: %s %s",
            GLASS_TIMELINE_SCOPE, GLASS_LOCATION_SCOPE);

    private static ExecutorService sThreadPool =
            Executors.newSingleThreadExecutor();

    private final Handler mHandler = new Handler();

    private String mAuthToken;
    private Button mStartAuthButton;
    private Button mExpireTokenButton;
    private ImageButton mNewCardButton;
    private boolean mInvalideShoppingVersion;
    private String mLastMirrorId;
    private OIShoppingListSender sender;
    private Spinner listsSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            PackageInfo info = getPackageManager().getPackageInfo("org.openintents.shopping", 0);
            if (info.versionCode < 10024) {
                mInvalideShoppingVersion = true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            mInvalideShoppingVersion = true;
        }
        if (debug) { Log.d(TAG, "mInvalideShoppingVersion="+mInvalideShoppingVersion); }


        // Define our layout
        setContentView(R.layout.activity_main);

        // Get our views
        mStartAuthButton = (Button) findViewById(R.id.oauth_button);
        mExpireTokenButton = (Button) findViewById(R.id.oauth_expire_button);
        mNewCardButton = (ImageButton) findViewById(R.id.new_card_button);

        // Restore any saved instance state
        if (savedInstanceState != null) {
            onTokenResult(savedInstanceState.getString(PARAM_AUTH_TOKEN));
        } else {
            mStartAuthButton.setEnabled(true);
            mExpireTokenButton.setEnabled(false);
        }

        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        String oauthToken = prefs.getString(PREF_OAUTH_TOKEN, null);
        if (oauthToken != null)
        {
            if (debug) Log.d(TAG, "got OAUTH_TOKEN="+oauthToken);
            mAuthToken = oauthToken;
            mExpireTokenButton.setEnabled(true);
            mStartAuthButton.setEnabled(false);
        } else {
            if (debug) Log.d(TAG, "no save OAUTH_TOKEN");
        }

        String lastMirrorId = prefs.getString(PREF_LAST_MIRROR_ID, null);
        if (lastMirrorId != null)
        {
            if (debug) Log.d(TAG, "got LAST_MIRROR_ID="+lastMirrorId);
            mLastMirrorId=lastMirrorId;
        } else {
            mLastMirrorId=null;
        }

        mStartAuthButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Present the user with an account picker dialog with a list
                // of their Google accounts
                Intent intent = AccountPicker.newChooseAccountIntent(
                        null, null, new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE},
                        false, null, null, null, null);
                startActivityForResult(intent, REQUEST_ACCOUNT_PICKER);
            }
        });

        mExpireTokenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!TextUtils.isEmpty(mAuthToken)) {
                    // Expire the token, if any
                    GoogleAuthUtil.invalidateToken(MainActivity.this, mAuthToken);
                }
                saveCredentials(null);
            }
        });

        mNewCardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (debug) { Log.d(TAG, "mInvalideShoppingVersion="+mInvalideShoppingVersion); }
                createNewTimelineItem();
            }
        });
        sender=new OIShoppingListSender();
        sender.initSender(getApplicationContext());

        listsSpinner = (Spinner) findViewById(R.id.lists_spinner);
        updateListsSpinner();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(PARAM_AUTH_TOKEN, mAuthToken);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
        switch (requestCode) {
            case REQUEST_ACCOUNT_PICKER:
                if (RESULT_OK == resultCode) {
                    String account = data.getStringExtra(
                            AccountManager.KEY_ACCOUNT_NAME);
                    String type = data.getStringExtra(
                            AccountManager.KEY_ACCOUNT_TYPE);

                    // TODO: Cache the chosen account
                    Log.i(TAG, String.format("User selected account %s of type %s",
                            account, type));
                    fetchTokenForAccount(account);
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (RESULT_OK == resultCode) {
                    String token = data.getStringExtra(
                            AccountManager.KEY_AUTHTOKEN);

                    Log.i(TAG, String.format(
                            "Authorization request returned token %s", token));
                    onTokenResult(token);
                }
                break;
        }
    }

    public void updateListsSpinner() {

        String[] lists=sender.getLists();

        List<String> list = new ArrayList<String>();
        if (lists.length>0) {
            for(int i=0; i<lists.length; i++) {
                list.add(lists[i]);
            }
        } else {
            list.add("No shopping lists");
            listsSpinner.setEnabled(false);
        }
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
            android.R.layout.simple_spinner_item, list);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        listsSpinner.setAdapter(dataAdapter);
    }

    private JSONObject buildShoppingCard() throws JSONException {
        JSONObject card=new JSONObject();
        String html="<article class=\"auto-paginate\"><section>";
        html+="<ul class=\"text-x-small\">";
        String text="";
        sender.refreshCursor();
        String[] items=sender.getItems();
        for (String item : items) {
            text+=item+" ";
            html+="<li>"+item+"</li>\n";
        }
        html+="</ul></section><footer>\n<p>OI Shopping List</p>\n</footer></article>";
        card.put("html", html);
        card.put("text", text);
        return card;
    }

    private void createNewTimelineItem() {
        if (!TextUtils.isEmpty(mAuthToken)) {
                try {
                    JSONObject notification = new JSONObject();
                    notification.put("level", "DEFAULT"); // Play a chime

                    JSONObject json = buildShoppingCard();
                    json.put("notification", notification);

                    MirrorApiClient client = MirrorApiClient.getInstance(this);
                    MirrorApiClient.Callback callback=new MirrorApiClient.Callback() {
                        @Override
                        public void onSuccess(HttpResponse response) {
                            String timelineAction="Updated";
                            try {
                                InputStream inputStream = response.getEntity().getContent();
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                IOUtils.copy(inputStream, baos);
                                JSONObject jsonObject = new JSONObject(baos.toString());
                                String id=jsonObject.getString("id");
                                if (debug) Log.d(TAG, "id="+id);

                                if (id!=null && (id.length()>0)) {
                                    if (!TextUtils.equals(mLastMirrorId,id)) {
                                        SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
                                        editor.putString(PREF_LAST_MIRROR_ID, id);
                                        editor.commit();
                                        if (debug) Log.d(TAG, "saved LAST_MIRROR_ID="+id);
                                        mLastMirrorId=id;
                                        timelineAction="Created new";
                                    }
                                }
                            } catch (IOException e1) {
                                // Pass
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            Toast.makeText(MainActivity.this, timelineAction+" timeline item",
                                    Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(HttpResponse response, Throwable e) {
                            try {
                                InputStream inputStream = response.getEntity().getContent();
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                IOUtils.copy(inputStream, baos);
                                JSONObject jsonObject = new JSONObject(baos.toString());
                                JSONObject error = jsonObject.getJSONObject("error");
                                String message=error.getString("message");
                                if (debug) Log.d(TAG,"jsonObject="+jsonObject);
//                                if (debug) Log.d(TAG, "onFailure: " + EntityUtils.toString(response.getEntity()));
                                if (message!=null) {
                                    if (message.contentEquals("Not Found")) {
                                        if (debug) Log.d(TAG,"last id was not found");
                                        mLastMirrorId=null;
                                    } else if (message.contentEquals("Invalid Credentials")) {
                                        saveCredentials(null);
                                    }
                                }
                            } catch (IOException e1) {
                                // Pass
                            } catch (JSONException e1) {
                                e1.printStackTrace();
                            }
                            Toast.makeText(MainActivity.this, "Failed to create new timeline item",
                                    Toast.LENGTH_SHORT).show();
                        }
                    };

                    if (mLastMirrorId!=null) {
                        if (debug) Log.d(TAG,"update using "+mLastMirrorId);
                        client.updateTimelineItem(mAuthToken, json, callback, mLastMirrorId);
                    } else {
                        client.createTimelineItem(mAuthToken, json, callback);
                    }
                } catch (JSONException e) {
                    Toast.makeText(this, "Sorry, can't serialize that to JSON",
                            Toast.LENGTH_SHORT).show();
                }
        } else {
            Toast.makeText(this, "Sorry, can't create a new timeline card without a token",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void onTokenResult(String token) {
        Log.d(TAG, "onTokenResult: " + token);
        saveCredentials(token);
        if (!TextUtils.isEmpty(token)) {
            Toast.makeText(this, "New token result", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Sorry, invalid token result", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveCredentials(String token) {
        if (TextUtils.isEmpty(token)) {
            mAuthToken = null;
            mExpireTokenButton.setEnabled(false);
            mStartAuthButton.setEnabled(true);
        } else {
            mAuthToken = token;
            mExpireTokenButton.setEnabled(true);
            mStartAuthButton.setEnabled(false);
        }
        SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
        editor.putString(PREF_OAUTH_TOKEN, token);
        editor.commit();
        if (debug) Log.d(TAG, "saved OAUTH_TOKEN="+token);
    }

    private void fetchTokenForAccount(final String account) {
        // We fetch the token on a background thread otherwise Google Play
        // Services will throw an IllegalStateException
        sThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // If this returns immediately the OAuth framework thinks
                    // the token should be usable
                    final String token = GoogleAuthUtil.getToken(
                            MainActivity.this, account, SCOPE);

                    if (token != null) {
                        // Pass the token back to the UI thread
                        Log.i(TAG, String.format("getToken returned token %s", token));
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                onTokenResult(token);
                            }
                        });
                    }
                } catch (final UserRecoverableAuthException e) {
                    // This means that the app hasn't been authorized by the user for access
                    // to the scope, so we're going to have to fire off the (provided) Intent
                    // to arrange for that. But we only want to do this once. Multiple
                    // attempts probably mean the user said no.
                    Log.i(TAG, "Handling a UserRecoverableAuthException");

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
                        }
                    });
                } catch (IOException e) {
                    // Something is stressed out; the auth servers are by definition
                    // high-traffic and you can't count on 100% success. But it would be
                    // bad to retry instantly, so back off
                    Log.e(TAG, "Failed to fetch auth token!", e);

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this,
                                    "Failed to fetch token, try again later", Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (GoogleAuthException e) {
                    // Can't recover from this!
                    Log.e(TAG, "Failed to fetch auth token!", e);

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this,
                                    "Failed to fetch token, can't recover", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }
}
