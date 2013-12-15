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
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.util.Date;
import java.util.TimeZone;

import de.msal.shoutemo.LoginActivity;
import de.msal.shoutemo.authenticator.AccountAuthenticator;

/**
 * @since 14.12.13
 */
public class SetUserTimezoneTask extends AsyncTask<Void, Void, Integer> {

    private Context context;

    public SetUserTimezoneTask(Context context) {
        this.context = context;
    }

    @Override
    protected Integer doInBackground(Void... params) {
        /* the offset includes daylight savings time if currently being within the dst period */
        double offsetinHours = TimeZone.getDefault().getOffset(new Date().getTime()) / 1000.0 / 60 / 60;

        try {
            return Connection.setUserTimezone(getAuthtoken(), offsetinHours);
        } catch (IOException e) {
            Log.e("SHOUTEMO", e.getMessage());
        }
        return -1;
    }

    @Override
    protected void onPostExecute(Integer ret) {
        if (ret != 200) {
            Log.e("SHOUTEMO", "Error setting the timezone. Returned code=" + ret);
        }
    }

    // TODO merge with SendPostTask.getAuthtoken()
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
