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

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import de.msal.shoutemo.connector.Connection;
import de.msal.shoutemo.connector.model.Author;
import de.msal.shoutemo.db.ChatDb;

/**
 * @since 13.06.14
 */
public class OnlineUsersActivity extends Activity {
    private final static String INSTANCESTATE_TITLE = "INSTANCE_TITLE";
    private final static String INSTANCESTATE_AUTHORS = "INSTANCE_AUTHORS";

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private OnlineUsersAdapter mAdapter;
    private ListView mListView;
    private ArrayList<Author> mAuthors;
    private MenuItem mMenuItemRefresh;

    private static boolean refreshTriggeredBySwipe = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onlineusers);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayUseLogoEnabled(false);
        getActionBar().setDisplayShowTitleEnabled(true);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.onlineusers_swipe);
        mSwipeRefreshLayout.setColorSchemeResources(
                R.color.autemo_pink,
                R.color.autemo_yellow_bright,
                R.color.autemo_green_secondary);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshTriggeredBySwipe = true;
                new GetOnlineUsersTask().execute();
            }
        });

        mAdapter = new OnlineUsersAdapter(getApplicationContext(), new LinkedList<Author>());
        mListView = (ListView) findViewById(android.R.id.list);
        mListView.setOnItemClickListener(new OnlineUseresClickListener());

        if(savedInstanceState != null) {
            getActionBar().setTitle(savedInstanceState.getCharSequence(INSTANCESTATE_TITLE));
            mAuthors = savedInstanceState.getParcelableArrayList(INSTANCESTATE_AUTHORS);
            mAdapter.addAll(mAuthors);
            mListView.setAdapter(mAdapter);
        } else {
            new GetOnlineUsersTask().execute();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequence(INSTANCESTATE_TITLE, getActionBar().getTitle());
        outState.putParcelableArrayList(INSTANCESTATE_AUTHORS, (mAuthors));
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.onlineusers, menu);
        mMenuItemRefresh = menu.findItem(R.id.action_onlineusers_refresh);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_onlineusers_refresh:
                new GetOnlineUsersTask().execute();
                break;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    private class GetOnlineUsersTask extends AsyncTask<Void, Void, List<Author>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (!refreshTriggeredBySwipe)
                mSwipeRefreshLayout.setRefreshing(true);
            if(mMenuItemRefresh != null) {
                mMenuItemRefresh.setEnabled(false);
            }
        }

        @Override
        protected List<Author> doInBackground(Void... params) {
            try {
                long start = System.nanoTime();
                mAuthors = new ArrayList<Author>(Connection.getOnlineUsers());

                // also persist the users in the database, while we're at it...
                ContentValues values;
                for (Author author : mAuthors) {
                    values = new ContentValues();
                    values.put(ChatDb.Authors.COLUMN_NAME_NAME, author.getName());
                    values.put(ChatDb.Authors.COLUMN_NAME_TYPE, author.getType().name());
                    getContentResolver().insert(ChatDb.Authors.CONTENT_URI, values);
                }
                // if refreshing is fastern than 1s, then sleep for 2s
                if (System.nanoTime() - start < 1L * 1000 * 1000 * 1000) {
                    Thread.sleep(2000);
                }
                // else if refreshing is fastern than 2s, then sleep for 1s
                else if (System.nanoTime() - start < 2L * 1000 * 1000 * 1000) {
                    Thread.sleep(1000);
                }
            } catch (IOException ignored) {
            } catch (InterruptedException ignored) {
            }
            return mAuthors;
        }

        @Override
        protected void onPostExecute(List<Author> authors) {
            super.onPostExecute(authors);
            mAdapter.clear();
            mAdapter.addAll(authors);
            mListView.setAdapter(mAdapter);

            getActionBar().setTitle(Html.fromHtml(getResources().getQuantityString(
                            R.plurals.title_users_online,
                            authors.size(),
                            authors.size()))
            );

            mSwipeRefreshLayout.setRefreshing(false);
            if(mMenuItemRefresh != null) {
                mMenuItemRefresh.setEnabled(true);
            }
            refreshTriggeredBySwipe = false;
        }
    }

    private class OnlineUsersAdapter extends ArrayAdapter<Author> {

        private OnlineUsersAdapter(Context context, List<Author> objects) {
            super(context, android.R.layout.simple_list_item_1, objects);
        }

        @Override
        public int getItemViewType(int position) {
            return getItem(position).getType().ordinal();
        }

        @Override
        public int getViewTypeCount() {
            return Author.Type.values().length;
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

    /**
     * open the users profile page in the browser, when clicked
     */
    private class OnlineUseresClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String author = mAdapter.getItem(position).getName();
            String url = "http://www.autemo.com/profiles/?id=";

            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url + author));
            startActivity(i);
        }
    }
}
