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

import android.accounts.Account;
import android.accounts.AccountManager;

/**
 * @since 16.10.13
 */
public class AutemoAccount {

    private final Account account;

    private final AccountManager manager;

    public AutemoAccount(final Account account, final AccountManager manager) {
        this.account = account;
        this.manager = manager;
    }

    /**
     * Get username
     *
     * @return username
     */
    public String getUsername() {
        return account.name;
    }

    /**
     * Get password
     *
     * @return password
     */
    public String getPassword() {
        return manager.getPassword(account);
    }

//    /**
//     * Get auth token
//     *
//     * @return token
//     */
//    public String getAuthToken() {
//        @SuppressWarnings("deprecation")
//        AccountManagerFuture<Bundle> future = manager.getAuthToken(account,
//                ACCOUNT_TYPE, false, null, null);
//
//        try {
//            Bundle result = future.getResult();
//            return result != null ? result.getString(KEY_AUTHTOKEN) : null;
//        } catch (AccountsException e) {
//            Log.e(TAG, "Auth token lookup failed", e);
//            return null;
//        } catch (IOException e) {
//            Log.e(TAG, "Auth token lookup failed", e);
//            return null;
//        }
//    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '[' + account.name + ']';
    }

}
