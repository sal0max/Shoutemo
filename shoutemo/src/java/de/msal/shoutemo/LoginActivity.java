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
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import de.msal.shoutemo.authenticator.AccountAuthenticator;
import de.msal.shoutemo.connector.Connection;

/**
 * Activity which displays a login screen to the user
 */
public class LoginActivity extends AccountAuthenticatorActivity {

    public static final String PARAM_AUTHTOKEN_TYPE = "authtokenType";
    public static final String PARAM_CONFIRMCREDENTIALS = "confirmCredentials";
    public static final String PARAM_USERNAME = "username";
    private static final String TAG = "Shoutemo|LoginActivity";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,4}$", Pattern.CASE_INSENSITIVE);
    private String mEmail, mPassword;
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mLoginFormView, mLoginStatusView;
    private UserLoginTask mAuthTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
        mPasswordView = (EditText) findViewById(R.id.password);
        mLoginFormView = findViewById(R.id.login_form);
        mLoginStatusView = findViewById(R.id.login_status);

        /* Auto complete for email */
        Account[] accounts = AccountManager.get(this).getAccounts();
        Set<String> emailSet = new HashSet<String>();
        for (Account account : accounts) {
            if (EMAIL_PATTERN.matcher(account.name).matches()) {
                emailSet.add(account.name);
            }
        }
        mEmailView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<String>(emailSet)));

        /* link description to autemo.com/signup */
        ((TextView) findViewById(R.id.sign_in_desc_head_to_autemo_com))
                .setMovementMethod(LinkMovementMethod.getInstance());
        ((TextView) findViewById(R.id.sign_in_desc_head_to_autemo_com)).setText(
                Html.fromHtml(getResources().getString(R.string.login_head_to_autemo_com)));

        /* show title */
        getActionBar().setDisplayUseLogoEnabled(false);
        getActionBar().setDisplayShowTitleEnabled(true);

        findViewById(R.id.sign_in_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptLogin();
                /* Hides keyboard when login button is clicked */
                InputMethodManager inputManager = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
            }
        });

        mEmail = getIntent().getStringExtra(PARAM_USERNAME);
        if (mEmail != null) {
            mEmailView.setText(mEmail);
        }
    }

//    private void login() {
//        mEmail = mEmailView.getText().toString();
//        mPassword = mPasswordView.getText().toString();
//
//        if (TextUtils.isEmpty(mEmail)) { // Email empty?
//            mEmailView.setError(getString(R.string.error_field_required));
//            return;
//        }
//        if (TextUtils.isEmpty(mPassword)) { // Password empty?
//            mEmailView.setError(getString(R.string.error_field_required));
//            return;
//        }
//
//        showProgress(true);
//
//        Handler loginHandler = new Handler() {
//            @Override
//            public void handleMessage(Message msg) {
//                if (msg.what == 0) { //NetworkUtil.ERR) {
//                    showProgress(false);
//                    Log.d(TAG, "Login Failed");
//                } else if (msg.what == 1) { //NetworkUtil.OK) {
//                    handleLoginResponse();
//                }
//            }
//        };
//
//        // NetworkUtil.login(getString(R.string.baseurl), mEmail, mPassword, loginHandler);
//        Message m = new Message();
//        m.what = 1;
//        loginHandler.dispatchMessage(m);
//
//    }
//
//    private void handleLoginResponse() {
//        showProgress(false);
//
//        final Account account = new Account(mEmail, AccountAuthenticator.ACCOUNT_TYPE);
//        if (getIntent().getStringExtra(PARAM_USERNAME) == null) {
//            AccountManager.get(this).addAccountExplicitly(account, mPassword, null);
//        } else {
//            AccountManager.get(this).setPassword(account, mPassword);
//        }
//
//        Intent intent = new Intent();
//        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, mEmail);
//        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, AccountAuthenticator.ACCOUNT_TYPE);
//        intent.putExtra(AccountManager.KEY_AUTHTOKEN, mPassword);
//
//        setAccountAuthenticatorResult(intent.getExtras());
//        setResult(RESULT_OK, intent);
//        finish();
//    }

    /**
     * Attempts to sign in the account specified by the login form. If there are form errors
     * (invalid email, missing fields, etc.), the errors are presented and no actual login attempt
     * is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

      /* reset errors */
        mEmailView.setError(null);
        mPasswordView.setError(null);

      /* store values at the time of the login attempt */
        mEmail = mEmailView.getText().toString();
        mPassword = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;
      /* check for a valid password */
        if (TextUtils.isEmpty(mPassword)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        }
      /* Check for a valid email address */
        if (TextUtils.isEmpty(mEmail)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first form field with an error.
            focusView.requestFocus();
        } else {
            // show a progress spinner
            showProgress(true);
            // kick off a background task to perform the user login attempt
            mAuthTask = new UserLoginTask();
            mAuthTask.execute((Void) null);
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    private void showProgress(final boolean show) {
        // fade-in the progress spinner
        int shortAnimTime = getResources().getInteger(
                android.R.integer.config_shortAnimTime);

        mLoginStatusView.setVisibility(View.VISIBLE);
        mLoginStatusView.animate()
                .setDuration(shortAnimTime)
                .alpha(show ? 1 : 0)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mLoginStatusView.setVisibility(
                                show ? View.VISIBLE : View.GONE);
                    }
                });

        mLoginFormView.setVisibility(View.VISIBLE);
        mLoginFormView.animate()
                .setDuration(shortAnimTime)
                .alpha(show ? 0 : 1)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mLoginFormView.setVisibility(
                                show ? View.GONE : View.VISIBLE);
                    }
                });
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {
            Log.v(TAG, "Started authenticating.");
            try {
                return Connection.isCredentialsCorrect(mEmail, mPassword);
            } catch (IOException e) {
                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                final Account account = new Account(mEmail, AccountAuthenticator.ACCOUNT_TYPE);
                if (getIntent().getStringExtra(PARAM_USERNAME) == null) {
                    AccountManager.get(getApplicationContext())
                            .addAccountExplicitly(account, mPassword, null);
                } else {
                    AccountManager.get(getApplicationContext()).setPassword(account, mPassword);
                }

                Intent intent = new Intent();
                intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, mEmail);
                intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, AccountAuthenticator.ACCOUNT_TYPE);
                intent.putExtra(AccountManager.KEY_AUTHTOKEN, mPassword);

                setAccountAuthenticatorResult(intent.getExtras());
                setResult(RESULT_OK, intent);
                finish();
            } else {
                mPasswordView.setError(getString(R.string.error_invalid_email_or_password));
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }

}
