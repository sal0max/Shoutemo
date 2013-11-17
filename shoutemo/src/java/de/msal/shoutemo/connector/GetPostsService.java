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

package de.msal.shoutemo.connector;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import de.msal.shoutemo.LoginActivity;
import de.msal.shoutemo.authenticator.AccountAuthenticator;
import de.msal.shoutemo.connector.model.Post;
import de.msal.shoutemo.db.ChatDb;

/**
 *
 */
public class GetPostsService extends Service {

    // constant
    private static final long INTERVAL = 2500; // 2.5 seconds
    // timer handling
    private Timer mTimer = null;
    // account handling
    private String mAuthToken;
    private AccountManager mAccountManager;
    private Account mAccount;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mAccountManager = AccountManager.get(this);

        assert mAccountManager != null;
        Account[] acc = mAccountManager.getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE);
        if (acc.length == 0) {
            Log.d("SHOUTEMO", "No suitable account found, directing user to add one");
            /* No account; push the user into adding one */
            mAccountManager.addAccount(
                    AccountAuthenticator.ACCOUNT_TYPE,
                    null,
                    null,
                    new Bundle(),
                    null,
                    new OnAccountAddComplete(),
                    null);
        } else {
            mAccount = acc[0]; // TODO: UI to pick account, for now we'll just take the first
            startAuthTokenFetch();
        }

    }

    @Override
    public void onDestroy() {
        if (mTimer != null) {
            mTimer.cancel();
        }
    }

    /**
     *
     */
    private void startAuthTokenFetch() {
        mAccountManager.getAuthToken(
                mAccount,
                LoginActivity.PARAM_AUTHTOKEN_TYPE,
                null,
                false,
                new OnAccountManagerComplete(),
                null
        );
    }

    /**
     *
     */
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
            Log.d("SHOUTEMO", "Received authentication token: " + mAuthToken);
            // now get messages!
            startRepeatingTask();
        }

        void startRepeatingTask() {
            // cancel if already existed
            if (mTimer != null) {
                mTimer.cancel();
            } else {
                // recreate new
                mTimer = new Timer();
            }
            // schedule task
            mTimer.scheduleAtFixedRate(new GetPostsTask(), 0, INTERVAL);
        }
    }

    /**
     *
     */
    private class GetPostsTask extends TimerTask {

        @Override
        public void run() {
            try {
                List<Post> posts = Connection.get(mAuthToken);
                if (posts.isEmpty()) {
                    Log.v("SHOUTEMO",
                            "Received empty data. Invalidating authtoken and fetching new one...");
                    stopRepeatingTask();
                    mAccountManager.invalidateAuthToken(AccountAuthenticator.ACCOUNT_TYPE,
                            mAuthToken);
                    startAuthTokenFetch();
                }

                for (Post post : posts) {
                    ContentValues values;

                    if (post.getAuthor() != null) {
                        values = new ContentValues();
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
                        values.put(ChatDb.Messages.COLUMN_NAME_TIMESTAMP,
                                post.getDate().getTime());
                    }
                    if (values.size() > 0) {
                        getContentResolver().insert(ChatDb.Messages.CONTENT_URI, values);
                    }
                }
            } catch (IOException e) {
                Log.e("SHOUTEMO", e.getMessage());
            }
        }

        void stopRepeatingTask() {
            if (mTimer != null) {
                mTimer.cancel();
            }
        }
    }

    /**
     *
     */
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

            /* no accounts saved, yet; ask the user for credentials */
            Intent launch = bundle.getParcelable(AccountManager.KEY_INTENT);
            if (launch != null) {
                launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launch);
                return;
            }

            mAccount = new Account(
                    bundle.getString(AccountManager.KEY_ACCOUNT_NAME),
                    bundle.getString(AccountManager.KEY_ACCOUNT_TYPE)
            );
            Log.d("SHOUTEMO", "Added account " + mAccount.name + "; now fetching new posts");
            startAuthTokenFetch();
        }
    }


}
