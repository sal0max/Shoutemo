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

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;

/**
 * Tries to send a new {@link de.msal.shoutemo.connector.model.Post} to the server. <b>Needs the two
 * String {@code authToken} and {@code message} passed to successfully run.</b>
 */
public class SendPostTask extends AsyncTask<String, Void, Integer> {

    protected Integer doInBackground(String... message) {
        if (message.length != 2) {
            throw new IllegalArgumentException(
                    "Need to pass authToken and message to successfully call SendPostTask.");
        }
        try {
            return Connection.post(message[0], message[1]);
        } catch (IOException e) {
            Log.e("SHOUTEMO", e.getMessage());
        }
        return -1;
    }

    protected void onPostExecute(int ret) {
        if (ret != 200) {
            Log.e("SHOUTEMO", "Error posting the message. Returned code=" + ret);
        }
    }
}