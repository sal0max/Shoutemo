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
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.text.format.Time;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.msal.shoutemo.LoginActivity;
import de.msal.shoutemo.authenticator.AccountAuthenticator;
import de.msal.shoutemo.connector.model.Post;
import de.msal.shoutemo.db.ChatDb;

/**
 *
 */
public class GetPostsService extends Service {

    // repeating task (get posts)
    private static long INTERVAL = 2500; // default: 2.5s
    private ScheduledExecutorService worker;
    // account handling
    private String mAuthToken;
    private AccountManager mAccountManager;
    private Account mAccount;

    @Override
    public void onCreate() {
        super.onCreate();

        mAccountManager = AccountManager.get(this);
        Account[] acc = mAccountManager.getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE);

        /* No account; push the user into adding one */
        if (acc.length == 0) {
            Log.d("SHOUTEMO", "No suitable account found, directing user to add one");
            mAccountManager.addAccount(
                    AccountAuthenticator.ACCOUNT_TYPE,
                    null,
                    null,
                    new Bundle(),
                    null,
                    new AccountManagerCallback<Bundle>() {
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
                            Log.d("SHOUTEMO",
                                    "Added account " + mAccount.name + "; now fetching new posts");
                            startGetPostsTask();
                        }
                    },
                    null);
        } else {
            mAccount = acc[0];
            startGetPostsTask();
        }
    }

    @Override
    public void onDestroy() {
        stopGetPostsTask();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     *
     */
    private void startGetPostsTask() {
        mAccountManager.getAuthToken(
                mAccount,
                LoginActivity.PARAM_AUTHTOKEN_TYPE,
                null,
                false,
                new AccountManagerCallback<Bundle>() {
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
                        if (worker == null || worker.isShutdown()) {
                            worker = Executors.newSingleThreadScheduledExecutor();
                        }
                        worker.scheduleAtFixedRate(new GetPostsTask(), 0, INTERVAL,
                                TimeUnit.MILLISECONDS);
                    }
                },
                null
        );
    }

    /**
     *
     */
    private void stopGetPostsTask() {
        if (worker != null) {
            worker.shutdown();
        }
    }

    /**
     * Sets the {@code INTERVAL} of this service in which the refresh calls should occur. This
     * {@code INTERVAL} is dependant on the given {@code timeSinceLastPost}. If the latest post was
     * some hours ago, it is not necessary to update every some seconds, but maybe every half a
     * minute. <br/> After the new refresh rate is altered, the task will be stoped and restartet at
     * its new rate.
     *
     * @param timeSinceLastPost the time in ms between the last post and the current time on the
     *                          phone.
     * @return the {@code INTERVAL} which was set, in ms.
     */
    private long setIntervall(long timeSinceLastPost) {
        long oldInterval = INTERVAL;

        if (timeSinceLastPost < 120000) { // < 2min
            INTERVAL = 2500; //   2.5s
        } else if (timeSinceLastPost < 300000) { // 2min - 5min
            INTERVAL = 5000; //   5.0s
        } else if (timeSinceLastPost < 600000) { // 5min - 10min
            INTERVAL = 10000; // 10.0s
        } else { // > 10min
            INTERVAL = 15000; // 15.0s
        }

        if (INTERVAL != oldInterval && worker != null && !worker.isShutdown()) {
            worker.shutdown();
            worker = Executors.newSingleThreadScheduledExecutor();
            worker.scheduleAtFixedRate(new GetPostsTask(), 0, INTERVAL,
                    TimeUnit.MILLISECONDS);
        }

        return INTERVAL;
    }

    /**
     *
     */
    private class GetPostsTask extends Thread {

        List<Post> posts = null;

        @Override
        public void run() {
            try {
                posts = Connection.get(mAuthToken);
            } catch (IOException e) {
                Log.e("SHOUTEMO", e.getMessage());
            }

            /* check if new posts can be received */
            if (posts.isEmpty()) {
                Log.v("SHOUTEMO",
                        "Received empty data. Invalidating authtoken and fetching new one...");
                stopGetPostsTask();
                mAccountManager.invalidateAuthToken(AccountAuthenticator.ACCOUNT_TYPE,
                        mAuthToken);
                startGetPostsTask();
            }

            /* insert into DB */
            for (Post post : posts) {
                ContentValues values;

                /* add a new person to the authors table if one is found */
                if (post.getAuthor() != null) {
                    values = new ContentValues();
                    values.put(ChatDb.Authors.COLUMN_NAME_NAME,
                            post.getAuthor().getName());
                    values.put(ChatDb.Authors.COLUMN_NAME_TYPE,
                            post.getAuthor().getType().name());
                    getContentResolver().insert(ChatDb.Authors.CONTENT_URI, values);
                }

                /* add a new post to the posts table */
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

                // if (values.size() > 0) {
                getContentResolver().insert(ChatDb.Messages.CONTENT_URI, values);
                // }
            }

            /* dynamically alter the refresh rate */
            //TODO: query only the newest post - everything else is a waste of resources
            //TODO: CAUTION! ONLY WORKS IF TIME-SETTINGS ON PHONE MATCH SETTINGS ON WEBSITE! WARN USER!?
            Cursor c = getContentResolver().query(ChatDb.Messages.CONTENT_URI,
                    new String[]{ChatDb.Messages.COLUMN_NAME_TIMESTAMP}, null, null,
                    ChatDb.Messages.COLUMN_NAME_TIMESTAMP + " DESC");
            c.moveToFirst();
            long newestPostTimestamp = c.getLong(0);
            c.close();
            Time now = new Time();
            now.setToNow();
            long timeSinceLastPost = now.toMillis(false) - newestPostTimestamp;
            // Log.d("SHOUTEMO", "current diff: " + timeSinceLastPost / 1000 + "s");
            setIntervall(timeSinceLastPost);
        }
    }

}
