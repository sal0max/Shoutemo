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

package de.msal.shoutemo.ui;

import android.app.FragmentManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import de.msal.shoutemo.R;
import de.msal.shoutemo.ui.chat.ChatFragment;
import de.msal.shoutemo.ui.onlineusers.OnlineUsersFragment;
import de.msal.shoutemo.ui.preference.PreferenceFragment;


public class MainActivity extends ActionBarActivity {

   private DrawerLayout mDrawerLayout;
   private ListView mDrawerList;
   private ActionBarDrawerToggle mDrawerToggle;

   private static boolean drawerOpen = false; // start with an closed drawer

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);

      Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
      setSupportActionBar(toolbar);

      mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
      mDrawerList = (ListView) findViewById(R.id.navigation_drawer);
      mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, 0, 0) {
         public void onDrawerClosed(View view) {
            drawerOpen = false;
         }

         public void onDrawerOpened(View drawerView) {
            drawerOpen = true;
         }
      };

      mDrawerLayout.setDrawerListener(mDrawerToggle);

      mDrawerList.setAdapter(new ArrayAdapter<String>(
            this,
            android.R.layout.simple_list_item_activated_1,
            android.R.id.text1,
            new String[] {"Chat",
                  getString(R.string.menu_users_online),
                  getString(R.string.menu_prefs)
            })
      );
      mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
      if (savedInstanceState == null) {
         mDrawerList.performItemClick(mDrawerList, 0, mDrawerList.getAdapter().getItemId(0)); //preselect on start
      }
      if (drawerOpen) {
         mDrawerLayout.openDrawer(mDrawerList);
      }
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
         switch (position) {
            case 0: // Chat
               fragmentManager.beginTransaction()
                     .replace(R.id.container, ChatFragment.newInstance())
                     .commit();
               break;
            case 1: // UsersOnline
               fragmentManager.beginTransaction()
                     .replace(R.id.container, OnlineUsersFragment.newInstance())
                     .commit();
               break;
            case 2: // Settings
               fragmentManager.beginTransaction()
                     .replace(R.id.container, PreferenceFragment.newInstance())
                     .commit();
               break;
         }
         // Highlight the selected item and close the drawer
         mDrawerList.setItemChecked(position, true);
         mDrawerLayout.closeDrawer(mDrawerList);
      }
   }

}
