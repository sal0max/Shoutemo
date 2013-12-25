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
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;

import de.msal.shoutemo.LoginActivity;
import de.msal.shoutemo.authenticator.AccountAuthenticator;

/**
 * Tries to send a new {@link de.msal.shoutemo.connector.model.Post} to the server. <b>Needs a
 * {@code message} passed as a single String to successfully run.</b>
 */
public class SendPostTask extends AsyncTask<String, Void, Integer> {

    private final Context context;

    public SendPostTask(Context context) {
        this.context = context;
    }

    @Override
    protected Integer doInBackground(String... message) {
        if (message.length != 1) {
            throw new IllegalArgumentException(
                    "Need to pass message to successfully call SendPostTask.");
        }
        try {
            return Connection.post(getAuthtoken(), message[0]);
        } catch (IOException e) {
            Log.e("SHOUTEMO", e.getMessage());
        }
        return -1;
    }

    @Override
    protected void onPostExecute(Integer ret) {
        if (ret != 200) {
            Log.e("SHOUTEMO", "Error posting the message. Returned code=" + ret);
        } else {
            /* restart GetPostsService, so that a sent message is directly shown without big delay */
            context.stopService(new Intent(context, GetPostsService.class));
            context.startService(new Intent(context, GetPostsService.class));
        }
    }

    /* Assume here that a account is already created. Everything else wouldn't make sense. */
    private String getAuthtoken() {
        AccountManager mAccountManager = AccountManager.get(context);
        Account[] acc = mAccountManager.getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE);

        if (acc.length == 0) {
            throw new IllegalStateException(
                    "No suitable account found, while trying to send a message. This shouldn't happen.");
        } else {
            Account mAccount = acc[0];
            AccountManagerFuture<Bundle> result = mAccountManager.getAuthToken(
                    mAccount,
                    LoginActivity.PARAM_AUTHTOKEN_TYPE,
                    null,
                    false,
                    null,
                    null
            );
            try {
                return result.getResult().getString(AccountManager.KEY_AUTHTOKEN);
            } catch (OperationCanceledException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (AuthenticatorException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

}
