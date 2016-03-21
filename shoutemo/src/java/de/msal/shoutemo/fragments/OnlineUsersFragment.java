/*
 * Copyright 2016 Maximilian Salomon.
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

package de.msal.shoutemo.fragments;

import android.app.Fragment;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import de.msal.shoutemo.R;
import de.msal.shoutemo.adapters.OnlineUsersAdapter;
import de.msal.shoutemo.connector.Connection;
import de.msal.shoutemo.connector.model.Author;
import de.msal.shoutemo.db.ChatDb;

/**
 * @since 13.06.14
 */
public class OnlineUsersFragment extends Fragment {

    private final static String INSTANCESTATE_TITLE = "INSTANCE_TITLE";
    private final static String INSTANCESTATE_AUTHORS = "INSTANCE_AUTHORS";

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private OnlineUsersAdapter mAdapter;
    private ArrayList<Author> mAuthors;
    private MenuItem mMenuItemRefresh;
    private CharSequence mTitle = new SpannableStringBuilder("...");

    private static boolean refreshTriggeredBySwipe = false;

    public static OnlineUsersFragment newInstance() {
        return new OnlineUsersFragment();
    }

    public OnlineUsersFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onlineusers, container, false);

        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.onlineusers_swipe);
        mSwipeRefreshLayout.setColorSchemeResources(
                R.color.autemo_pink,
                R.color.autemo_yellow_bright,
                R.color.autemo_green_secondary,
                R.color.autemo_blue);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshTriggeredBySwipe = true;
                new GetOnlineUsersTask().execute();
            }
        });

        mAdapter = new OnlineUsersAdapter(getActivity(), new LinkedList<Author>());
        mAdapter.setOnUserClickListener(new OnlineUseresClickListener());
        RecyclerView recyclerView = (RecyclerView) view.findViewById(android.R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(mAdapter);

        if (savedInstanceState != null) {
            mTitle = savedInstanceState.getCharSequence(INSTANCESTATE_TITLE);
            mAuthors = savedInstanceState.getParcelableArrayList(INSTANCESTATE_AUTHORS);
            mAdapter.addAll(mAuthors);
        } else {
            new GetOnlineUsersTask().execute();
        }

        getActivity().setTitle(mTitle);
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequence(INSTANCESTATE_TITLE, mTitle);
        outState.putParcelableArrayList(INSTANCESTATE_AUTHORS, (mAuthors));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.onlineusers, menu);
        mMenuItemRefresh = menu.findItem(R.id.action_onlineusers_refresh);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_onlineusers_refresh:
                new GetOnlineUsersTask().execute();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private class GetOnlineUsersTask extends AsyncTask<Void, Void, List<Author>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (!refreshTriggeredBySwipe) {
                mSwipeRefreshLayout.setProgressViewOffset(false, 0,
                        (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24,
                                getResources().getDisplayMetrics()));
            }
            mSwipeRefreshLayout.setRefreshing(true);
            if (mMenuItemRefresh != null) {
                mMenuItemRefresh.setEnabled(false);
            }
        }

        @Override
        protected List<Author> doInBackground(Void... params) {
            try {
                long start = System.nanoTime();
                mAuthors = new ArrayList<>(Connection.getOnlineUsers());

                // also persist the users in the database, while we're at it...
                ContentValues values;
                for (Author author : mAuthors) {
                    values = new ContentValues();
                    values.put(ChatDb.Authors.COLUMN_NAME_NAME, author.getName());
                    values.put(ChatDb.Authors.COLUMN_NAME_TYPE, author.getType().name());
                    getActivity().getContentResolver().insert(ChatDb.Authors.CONTENT_URI, values);
                }
                // if refreshing is fastern than 1s, then sleep for 500ms
                //noinspection PointlessArithmeticExpression
                if (System.nanoTime() - start < 1L * 1000 * 1000 * 1000) {
                    Thread.sleep(500);
                }
            } catch (IOException | InterruptedException ignored) {}
            return mAuthors;
        }

        @Override
        protected void onPostExecute(List<Author> authors) {
            super.onPostExecute(authors);
            mAdapter.clear();
            mAdapter.addAll(authors);

            mTitle = Html.fromHtml(getResources().getQuantityString(
                    R.plurals.title_users_online,
                    authors.size(),
                    authors.size()));
            getActivity().setTitle(mTitle);

            mSwipeRefreshLayout.setRefreshing(false);
            if (mMenuItemRefresh != null) {
                mMenuItemRefresh.setEnabled(true);
            }
            refreshTriggeredBySwipe = false;
        }
    }

    /**
     * open the users profile page in the browser, when clicked
     */
    private class OnlineUseresClickListener implements OnlineUsersAdapter.OnUserClickListener {

        @Override
        public void onUserClick(Author author) {
            String url = "http://www.autemo.com/profiles/?id=";

            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url + author));
            startActivity(i);

        }
    }
}
