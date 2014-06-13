/*
 * Copyright 2014 Maximilian Salomon.
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

import android.app.ListActivity;
import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import de.msal.shoutemo.connector.Connection;
import de.msal.shoutemo.connector.model.Author;
import de.msal.shoutemo.db.ChatDb;

/**
 * @since 13.06.14
 */
public class OnlineUsersActivity extends ListActivity {

    private OnlineUsersAdapter mAdapter;

    private ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayUseLogoEnabled(false);
        getActionBar().setDisplayShowTitleEnabled(true);

        mAdapter = new OnlineUsersAdapter(getApplicationContext(), new LinkedList<Author>());
        mListView = getListView();

        new GetOnlineUsersTask().execute();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class GetOnlineUsersTask extends AsyncTask<Void, Void, List<Author>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            setProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected List<Author> doInBackground(Void... params) {
            List<Author> authors = null;
            try {
                authors = Connection.getOnlineUsers();

                // also persist the users in the database, while we're at it...
                ContentValues values;
                for (Author author : authors) {
                    values = new ContentValues();
                    values.put(ChatDb.Authors.COLUMN_NAME_NAME, author.getName());
                    values.put(ChatDb.Authors.COLUMN_NAME_TYPE, author.getType().name());
                    getContentResolver().insert(ChatDb.Authors.CONTENT_URI, values);
                }
            } catch (IOException ignored) {
            }
            return authors;
        }

        @Override
        protected void onPostExecute(List<Author> authors) {
            super.onPostExecute(authors);
            mAdapter.clear();
            mAdapter.addAll(authors);
            mListView.setAdapter(mAdapter);

            getActionBar().setTitle(getActionBar().getTitle() + ": " + authors.size());

            setProgressBarIndeterminateVisibility(false);
        }
    }

    private class OnlineUsersAdapter extends ArrayAdapter<Author> {

        private OnlineUsersAdapter(Context context, List<Author> objects) {
            super(context, android.R.layout.simple_list_item_1, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Author author = getItem(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(android.R.layout.simple_list_item_1, parent, false);
            }

            TextView tvAuthor = (TextView) convertView.findViewById(android.R.id.text1);
            tvAuthor.setText(author.getName());
            /* show the right tvAuthor color (mod/admin/member) */
            switch (author.getType()) {
                case ADMIN:
                    tvAuthor.setTextColor(
                            getContext().getResources().getColor(R.color.autemo_blue));
                    break;
                case MOD:
                    tvAuthor.setTextColor(
                            getContext().getResources().getColor(R.color.autemo_green_secondary));
                    break;
            }

            return convertView;
        }
    }
}
