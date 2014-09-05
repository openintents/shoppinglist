package org.openintents.shopping.glass;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.mirror.model.MenuItem;
import com.google.api.services.mirror.model.NotificationConfig;
import com.google.api.services.mirror.model.TimelineItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final String TAG = "Glass";
    private static final String PARAM_AUTH_TOKEN =
            "com.example.mirror.android.AUTH_TOKEN";

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
    private EditText mNewCardEditText;
    private boolean mInvalideShoppingVersion;
    private OIShoppingListSender shoppingListSender;
    private ArrayList<String> sentItems = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // Define our layout
        setContentView(R.layout.activity_main);

        // Get our views
        mStartAuthButton = (Button) findViewById(R.id.oauth_button);
        mExpireTokenButton = (Button) findViewById(R.id.oauth_expire_button);
        mNewCardButton = (ImageButton) findViewById(R.id.new_card_button);
        mNewCardEditText = (EditText) findViewById(R.id.new_card_message);

        // Restore any saved instance state
        if (savedInstanceState != null) {
            onTokenResult(savedInstanceState.getString(PARAM_AUTH_TOKEN));
        } else {
            mStartAuthButton.setEnabled(true);
            mExpireTokenButton.setEnabled(false);
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
                    mAuthToken = null;
                    mExpireTokenButton.setEnabled(false);
                    mStartAuthButton.setEnabled(true);
                }
            }
        });

        shoppingListSender  = new OIShoppingListSender(this);

        mNewCardEditText.setText(shoppingListSender.getShoppingListName());

        mNewCardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sentItems.clear();
                addItems();

            }
        });

    }

    private void addItems() {

        String shoppingListName = mNewCardEditText.getText().toString();
        for (int i = 0; i< shoppingListSender.getCount();i++){
            OIShoppingListSender.Item item = shoppingListSender.getItem(i);
            TimelineItem timelineItem = new TimelineItem();
            NotificationConfig notification = new NotificationConfig();
            notification.setLevel("DEFAULT");
            timelineItem.setNotification(notification);
            timelineItem.setBundleId(shoppingListName);
            timelineItem.setText(item.item);

            createNewTimelineItem(timelineItem);
        }
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

    private void createNewTimelineItem(TimelineItem message) {

        if (!TextUtils.isEmpty(mAuthToken)) {
            if (message != null) {
                message.setFactory(new JacksonFactory());
                try {
                    MirrorApiClient client = MirrorApiClient.getInstance(this);
                    client.createTimelineItem(mAuthToken, message.toPrettyString(), new MirrorApiClient.Callback() {
                        @Override
                        public void onSuccess(HttpResponse response) {
                            try {
                                String jsonString = EntityUtils.toString(response.getEntity());
                                Log.v(TAG, "onSuccess: " + jsonString);

                                JSONObject timelineItem = new JSONObject(jsonString);
                                String id = timelineItem.getString("id");
                                sentItems.add(id);
                                if (sentItems.size() == shoppingListSender.getCount()){
                                    sendListCard();
                                }
                            } catch (IOException e1) {
                                // Pass
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            Toast.makeText(MainActivity.this, "Created new timeline item",
                                    Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(HttpResponse response, Throwable e) {
                            try {
                                Log.v(TAG, "onFailure: " + EntityUtils.toString(response.getEntity()));
                            } catch (IOException e1) {
                                // Pass
                            }
                            Toast.makeText(MainActivity.this, "Failed to create new timeline item",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else {
                Toast.makeText(this, "Sorry, can't create an empty timeline item",
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Sorry, can't create a new timeline card without a token",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void sendListCard() {
        String shoppingListName = mNewCardEditText.getText().toString();


        TimelineItem timelineItem = new TimelineItem();
        timelineItem.setText(shoppingListSender.getShoppingListName());
        List<MenuItem> menuItems = new ArrayList<MenuItem>();
        MenuItem menuItem = new MenuItem();
        menuItem.setAction("OPEN_URI");
        menuItem.setPayload("shoppingitem://item/" + Html.escapeHtml(shoppingListName) + "?ids=" + TextUtils.join(",", sentItems));
        menuItems.add(menuItem);
        timelineItem.setMenuItems(menuItems);
        timelineItem.setBundleId(shoppingListName);
        createNewTimelineItem(timelineItem);
    }

    private void onTokenResult(String token) {
        Log.d(TAG, "onTokenResult: " + token);
        if (!TextUtils.isEmpty(token)) {
            mAuthToken = token;
            mExpireTokenButton.setEnabled(true);
            mStartAuthButton.setEnabled(false);
            Toast.makeText(this, "New token result", Toast.LENGTH_SHORT).show();
        } else {
            mExpireTokenButton.setEnabled(false);
            mStartAuthButton.setEnabled(true);
            Toast.makeText(this, "Sorry, invalid token result", Toast.LENGTH_SHORT).show();
        }
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