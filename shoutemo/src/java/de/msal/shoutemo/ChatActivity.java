/*
 * Copyright 2013 Maximilian Salomon.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */

package de.msal.shoutemo;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.ActionBar;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageButton;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import de.msal.shoutemo.authenticator.AccountAuthenticator;
import de.msal.shoutemo.connector.Connection;
import de.msal.shoutemo.connector.model.Post;
import de.msal.shoutemo.db.ChatDb;
import de.msal.shoutemo.helpers.TypeFacespan;

public class ChatActivity extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private final static int LOADER_ID_MESSAGES = 0;

    private Timer myTimer; // move to Service (but not running in ui thread!)

    private String mAccountType, mAuthToken;

    private AccountManager mAccountManager;

    private Account mAccount;

    private ListAdapter listAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        /* set custom action bar font */
        SpannableString s = new SpannableString(getString(R.string.app_name).toLowerCase());
        s.setSpan(new TypeFacespan(this, "BIRDMAN_.TTF"), 0, s.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setTitle(s);
        }

        /* */
        final EditText editText = (EditText) findViewById(R.id.et_input);
        ImageButton btnSend = (ImageButton) findViewById(R.id.ib_send);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!TextUtils.isEmpty(editText.getText()) && !TextUtils.isEmpty(mAuthToken)) {
                    new SendTask().execute(editText.getText().toString());
                    editText.setText("");
                } else {
                    Log.e("!?!?!?!?",
                            "TextUtils.isEmpty(editText.getText()="
                                    + TextUtils.isEmpty(editText.getText())
                                    + "TextUtils.isEmpty(mAuthToken)="
                                    + TextUtils.isEmpty(mAuthToken));
                }
            }
        });

        /* Account stuff  */
        mAccountType = AccountAuthenticator.ACCOUNT_TYPE;
        mAccountManager = AccountManager.get(this);

        // TODO: UI to pick account, for now we'll just take the first
        Account[] acc = mAccountManager.getAccountsByType(mAccountType);
        if (acc.length == 0) {
            Log.d("main", "No suitable account found, directing user to add one");
            /* No account, push the user into adding one. use addAccount rather than an Intent so
               that we can specify our own account type */
            mAccountManager.addAccount(
                    mAccountType,
                    null,
                    null,
                    new Bundle(),
                    this,
                    new OnAccountAddComplete(),
                    null);
        } else {
            mAccount = acc[0];
            startAuthTokenFetch();
        }

        /* list stuff */
        this.listAdapter = new ListAdapter(this, null,
                CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        setListAdapter(this.listAdapter);

        getLoaderManager().initLoader(LOADER_ID_MESSAGES, null, this);
    }

    private void startAuthTokenFetch() {
        Bundle options = new Bundle();
        mAccountManager.getAuthToken(
                mAccount,
                LoginActivity.PARAM_AUTHTOKEN_TYPE,
                options,
                this,
                new OnAccountManagerComplete(),
                null //new Handler(new OnError())
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.chat, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAuthToken != null) {
            startRepeatingTask();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopRepeatingTask();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ID_MESSAGES:
                return new CursorLoader(this, ChatDb.Posts.CONTENT_URI, null, null, null, null);
            default:
                return null;
        }

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        listAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        listAdapter.swapCursor(null);
    }

    void startRepeatingTask() {
        myTimer = new Timer("MyTimer", true);
        myTimer.scheduleAtFixedRate(new getPostsTask(), 0, 2500); // every 2.5s
    }

    void stopRepeatingTask() {
        if (myTimer != null) {
            myTimer.cancel();
        }
    }

    private class OnAccountAddComplete implements AccountManagerCallback<Bundle> {

        @Override
        public void run(AccountManagerFuture<Bundle> result) {
            Bundle bundle;
            try {
                bundle = result.getResult();
            } catch (OperationCanceledException e) {
                e.printStackTrace();
                return;
            } catch (AuthenticatorException e) {
                e.printStackTrace();
                return;
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            mAccount = new Account(
                    bundle.getString(AccountManager.KEY_ACCOUNT_NAME),
                    bundle.getString(AccountManager.KEY_ACCOUNT_TYPE)
            );
            Log.d("main", "Added account " + mAccount.name + ", fetching");
            startAuthTokenFetch();
        }
    }

    private class getPostsTask extends TimerTask {

        @Override
        public void run() {
            try {
                List<Post> posts = Connection.get(mAuthToken);
                if (posts.isEmpty()) {
                    Log.v("!!!!!", "invalidating authtoken and fetch new one");
                    stopRepeatingTask();
                    mAccountManager
                            .invalidateAuthToken(AccountAuthenticator.ACCOUNT_TYPE, mAuthToken);
                    startAuthTokenFetch();
                }

                for (Post post : posts) {
                    ContentValues values = new ContentValues();

                    if (post.getAuthor() != null) {
                        values.put(ChatDb.Authors.COLUMN_NAME_NAME,
                                post.getAuthor().getName());
                        values.put(ChatDb.Authors.COLUMN_NAME_TYPE,
                                post.getAuthor().getType().name());
                        getContentResolver().insert(ChatDb.Authors.CONTENT_URI, values);
                    }

                    values = new ContentValues();
                    values.put(ChatDb.Messages.COLUMN_NAME_AUTHOR_NAME,
                            post.getAuthor() == null ? null : post.getAuthor().getName());
                    if (post.getMessage() != null) {
                        values.put(ChatDb.Messages.COLUMN_NAME_MESSAGE_HTML,
                                post.getMessage().getHtml());
                        values.put(ChatDb.Messages.COLUMN_NAME_MESSAGE_TEXT,
                                post.getMessage().getText());
                        values.put(ChatDb.Messages.COLUMN_NAME_TYPE,
                                post.getMessage().getType().name());
                        values.put(ChatDb.Messages.COLUMN_NAME_TIMESTAMP, post.getDate().getTime());
                    }
                    if (values.size() > 0) {
                        getContentResolver().insert(ChatDb.Messages.CONTENT_URI, values);
                    }
                }
            } catch (IOException e) {
                Log.e("shoutemo", e.getMessage());
            }
        }
    }

    private class SendTask extends AsyncTask<String, Void, Integer> {

        protected Integer doInBackground(String... message) {
            try {
                return Connection.post(mAuthToken, message[0]);
            } catch (IOException e) {
                Log.e("shoutemo:SendTask", e.getMessage());
            }
            return -1;
        }

        protected void onPostExecute(int ret) {
            if (ret != 200) {
                Log.e("shoutemo:SendTask", "Error posting the message. Returned code=" + ret);
            }
        }
    }

    private class OnAccountManagerComplete implements AccountManagerCallback<Bundle> {

        @Override
        public void run(AccountManagerFuture<Bundle> result) {
            Bundle bundle;
            try {
                bundle = result.getResult();
            } catch (OperationCanceledException e) {
                e.printStackTrace();
                return;
            } catch (AuthenticatorException e) {
                e.printStackTrace();
                return;
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            mAuthToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
            Log.d("main", "Received authentication token " + mAuthToken);

            // now get messages!
            startRepeatingTask();
        }
    }
}
