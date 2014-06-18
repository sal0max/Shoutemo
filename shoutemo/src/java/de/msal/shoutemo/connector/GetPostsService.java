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
import android.content.ContentProviderOperation;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.Time;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
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

    private static final String TAG = "Shoutemo|GetPostsService";
    // everything for showing the udpate status to the user
    private LocalBroadcastManager broadcaster;
    public static final String INTENT_UPDATE = "de.msal.shoutemo.GetPostsService.UPDATING";
    public static final String INTENT_UPDATE_ENABLED
            = "de.msal.shoutemo.GetPostsService.UPDATING_ENABLED";
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

        broadcaster = LocalBroadcastManager.getInstance(this);

        mAccountManager = AccountManager.get(this);
        Account[] acc = mAccountManager.getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE);

        /* No account; push the user into adding one */
        if (acc.length == 0) {
            Log.v(TAG, "No suitable account found, directing user to add one.");
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
                            Log.v(TAG,
                                    "Added account " + mAccount.name + "; now fetching new posts.");
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
                        Log.v(TAG, "Received authentication token=" + mAuthToken);
                        // now get messages!
                        if (worker == null || worker.isShutdown()) {
                            worker = Executors.newSingleThreadScheduledExecutor();
                        }
                        // fix (possible) wrong time-setting on autemo.com
                        worker.execute(new SetTimezoneTask());
                        // only now recieve messages (with right time)
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
            worker.scheduleAtFixedRate(new GetPostsTask(), 0, INTERVAL, TimeUnit.MILLISECONDS);
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
            setUpdatingNotification(true);

            try {
                posts = Connection.getPosts(mAuthToken);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }

            /* check if new posts can be received */
            if (posts.isEmpty()) {
                Log.v(TAG, "Received empty data. Invalidating authtoken & fetching new one.");
                stopGetPostsTask();
                mAccountManager.invalidateAuthToken(AccountAuthenticator.ACCOUNT_TYPE, mAuthToken);
                startGetPostsTask();
            }

            /* insert into DB */
            ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
            for (Post post : posts) {
                if (post.getAuthor() != null) {
                    ops.add(
                            ContentProviderOperation.newInsert(ChatDb.Authors.CONTENT_URI)
                                    .withValue(ChatDb.Authors.COLUMN_NAME_NAME,
                                            post.getAuthor().getName())
                                    .withValue(ChatDb.Authors.COLUMN_NAME_TYPE,
                                            post.getAuthor().getType().name())
                                    .withYieldAllowed(true)
                                    .build()
                    );
                }
                if (post.getMessage() != null) {
                    ops.add(
                            ContentProviderOperation.newInsert(ChatDb.Messages.CONTENT_URI)
                                    .withValue(ChatDb.Messages.COLUMN_NAME_AUTHOR_NAME,
                                            post.getAuthor() == null ? null : post.getAuthor().getName())
                                    .withValue(ChatDb.Messages.COLUMN_NAME_MESSAGE_HTML,
                                            post.getMessage().getHtml())
                                    .withValue(ChatDb.Messages.COLUMN_NAME_MESSAGE_TEXT,
                                            post.getMessage().getText())
                                    .withValue(ChatDb.Messages.COLUMN_NAME_TYPE,
                                            post.getMessage().getType().name())
                                    .withValue(ChatDb.Messages.COLUMN_NAME_TIMESTAMP,
                                            post.getDate().getTime())
                                    .withYieldAllowed(true)
                                    .build()
                    );
                }
            }
            try {
                getContentResolver().applyBatch(ChatDb.AUTHORITY, ops);
            } catch (RemoteException e) {
                Log.e(TAG, "Error: " + e.getMessage());
            } catch (OperationApplicationException e) {
                Log.e(TAG, "Error: " + e.getMessage());
            }

            /* dynamically alter the refresh rate */
            Cursor c = getContentResolver().query(ChatDb.Messages.CONTENT_URI,
                    new String[]{ChatDb.Messages.COLUMN_NAME_TIMESTAMP}, null, null,
                    ChatDb.Messages.COLUMN_NAME_TIMESTAMP + " DESC LIMIT 1");
            c.moveToFirst();
            long newestPostTimestamp = c.getLong(0);
            c.close();
            Time now = new Time();
            now.setToNow();
            long timeSinceLastPost = now.toMillis(false) - newestPostTimestamp;
            setIntervall(timeSinceLastPost);

            setUpdatingNotification(false);
        }
    }

    private class SetTimezoneTask extends Thread {

        @Override
        public void run() {
           /* have to add the dst savings, else during dst the time is off by
              another hour! */
           double offsetinHours =
                 (TimeZone.getDefault().getOffset(new Date().getTime())
                       + TimeZone.getDefault().getDSTSavings()) / 1000.0 / 60
                       / 60;
            int returnCode = -2;
            try {
                returnCode = Connection.setUserTimezone(mAuthToken, offsetinHours);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
            if (returnCode != 200) {
                Log.e(TAG, "Error setting the timezone. Returned code=" + returnCode);
            }
        }
    }

    /**
     * Sends a {@code Intent} with the only data included whether this Service is currently trying
     * to get new {@code Post}s from the server.
     *
     * @param enabled Set to {@code true} when it is currently trying, to false when the Service is
     *                idling.
     */
    private void setUpdatingNotification(boolean enabled) {
        Intent intent = new Intent(INTENT_UPDATE);
        intent.putExtra(INTENT_UPDATE_ENABLED, enabled);
        broadcaster.sendBroadcast(intent);
    }

}
