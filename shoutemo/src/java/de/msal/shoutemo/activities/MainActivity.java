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

package de.msal.shoutemo.activities;

import android.app.FragmentManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import de.msal.shoutemo.R;
import de.msal.shoutemo.adapters.NavigationDrawerAdapter;
import de.msal.shoutemo.fragments.ChatFragment;
import de.msal.shoutemo.fragments.OnlineUsersFragment;
import de.msal.shoutemo.fragments.PreferenceFragment;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout mDrawerLayout;
    private View mNavigationDrawer;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;

    private static boolean drawerOpen = false; // start with an closed drawer

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupToolbar(savedInstanceState);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            // update the main content by replacing fragments
            FragmentManager fragmentManager = getFragmentManager();
            /**
             * @see NavigationDrawerAdapter.mEntries
             */
            switch (position) {
                case 0: // Chat
                    fragmentManager.beginTransaction()
                            .replace(R.id.container, ChatFragment.newInstance(), "CHAT")
                            .commit();
                    break;
                case 1: // UsersOnline
                    fragmentManager.beginTransaction()
                            .replace(R.id.container, OnlineUsersFragment.newInstance())
                            .commit();
                    break;
                case 3: // Settings
                    fragmentManager.beginTransaction()
                            .replace(R.id.container, PreferenceFragment.newInstance())
                            .commit();
                    break;
            }
            // Highlight the selected item and close the drawer
            mDrawerList.setItemChecked(position, true);
            mDrawerLayout.closeDrawer(mNavigationDrawer);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mDrawerLayout.isDrawerOpen(mNavigationDrawer)) {
                mDrawerLayout.closeDrawer(mNavigationDrawer);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Setup Navigation Drawer width as per Material Design guidelines: calculate the Toolbar height.
     */
    private void setupToolbar(Bundle savedInstanceState) {
        Toolbar toolBar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolBar);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolBar, 0, 0) {
            public void onDrawerClosed(View view) {
                drawerOpen = false;
            }
            public void onDrawerOpened(View drawerView) {
                drawerOpen = true;
            }
        };
        mNavigationDrawer = findViewById(R.id.navigation_drawer);

        TypedValue tv = new TypedValue();
        int toolbarHeight;
        if (getTheme().resolveAttribute(R.attr.actionBarSize, tv, true)) {
            toolbarHeight = (int) (tv.getDimension(getResources().getDisplayMetrics()) / getResources().getDisplayMetrics().density);
            if (toolbarHeight > 0) {
                int drawerMaxWidth = (int) (getResources().getDimension(R.dimen.navigation_drawer_max_width) / getResources().getDisplayMetrics().density);
                Configuration configuration = getResources().getConfiguration();
                int screenWidthDp = configuration.screenWidthDp;
                int drawerWidth = screenWidthDp - toolbarHeight;

                ViewGroup.LayoutParams layout_description = mNavigationDrawer.getLayoutParams();
                layout_description.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                        drawerWidth < drawerMaxWidth ? drawerWidth : drawerMaxWidth ,
                        getResources().getDisplayMetrics());
                mNavigationDrawer.setLayoutParams(layout_description);
            }
        }

        mDrawerLayout.addDrawerListener(mDrawerToggle);

        mDrawerList = (ListView) mNavigationDrawer.findViewById(android.R.id.list);
        mDrawerList.setAdapter(new NavigationDrawerAdapter(getApplicationContext(), 0));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        if (savedInstanceState == null) {
            mDrawerList.performItemClick(mDrawerList, 0,
                    mDrawerList.getAdapter().getItemId(0)); //preselect on start
        }
        if (drawerOpen) {
            mDrawerLayout.openDrawer(mNavigationDrawer);
        }
    }

}
