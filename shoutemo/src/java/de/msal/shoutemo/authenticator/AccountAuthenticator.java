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

package de.msal.shoutemo.authenticator;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.io.IOException;

import de.msal.shoutemo.LoginActivity;
import de.msal.shoutemo.connector.Connection;

/**
 * @since 03.10.13
 */
public class AccountAuthenticator extends AbstractAccountAuthenticator {

    // public static final String AUTHTOKEN_TYPE = "de.msal.shoutemo.auth";
    public static final String ACCOUNT_TYPE = "de.msal.shoutemo.auth";

    private final Context context;

    public AccountAuthenticator(Context context) {
        super(context);
        this.context = context;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType,
            String authTokenType, String[] requiredFeatures, Bundle options) {
        AccountManager am = AccountManager.get(context);
        Account[] acc = am.getAccountsByType(ACCOUNT_TYPE);

        Bundle bundle = new Bundle(); //TODO: allow multiple accounts
        if (acc.length == 0) { // only allow a single instance of our account type
            Intent intent = new Intent(context, LoginActivity.class);
            intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
            bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        }
        return bundle;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account,
            Bundle options) throws NetworkErrorException {
        if (options != null && options.containsKey(AccountManager.KEY_PASSWORD)) {
            try {
                final String password = options.getString(AccountManager.KEY_PASSWORD);
                final boolean verified = Connection.isCredentialsCorrect(account.name, password);

                final Bundle result = new Bundle();
                result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, verified);
                return result;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // Launch LoginActivity to confirm credentials
        final Intent intent = new Intent(context, LoginActivity.class);
        intent.putExtra(LoginActivity.PARAM_USERNAME, account.name);
        intent.putExtra(LoginActivity.PARAM_CONFIRMCREDENTIALS, true);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response,
            String accountType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account,
            String authTokenType, Bundle options) throws NetworkErrorException {
        if (!authTokenType.equals(LoginActivity.PARAM_AUTHTOKEN_TYPE)) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ERROR_MESSAGE, "invalid authTokenType");
            return result;
        }
        final AccountManager am = AccountManager.get(context);

        final String password = am.getPassword(account);
        String authtoken = "";
        if (password != null) {
            boolean verified = false;
            try {
                verified = Connection.isCredentialsCorrect(account.name, password);
                authtoken = Connection.getToken(account.name, password);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (verified && authtoken != "") {
                final Bundle result = new Bundle();
                result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
                result.putString(AccountManager.KEY_ACCOUNT_TYPE, ACCOUNT_TYPE);
                result.putString(AccountManager.KEY_AUTHTOKEN, authtoken);
                return result;
            }
        }
        // the password was missing or incorrect, return an Intent to the LoginActivity
        final Intent intent = new Intent(context, LoginActivity.class);
        intent.putExtra(LoginActivity.PARAM_USERNAME, account.name);
        intent.putExtra(LoginActivity.PARAM_AUTHTOKEN_TYPE, authTokenType);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        return null;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account,
            String[] features) throws NetworkErrorException {
        final Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return result;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account,
            String authTokenType, Bundle options) throws NetworkErrorException {
        final Intent intent = new Intent(context, LoginActivity.class);
        intent.putExtra(LoginActivity.PARAM_USERNAME, account.name);
        intent.putExtra(LoginActivity.PARAM_AUTHTOKEN_TYPE, authTokenType);
        intent.putExtra(LoginActivity.PARAM_CONFIRMCREDENTIALS, false);

        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }


}
