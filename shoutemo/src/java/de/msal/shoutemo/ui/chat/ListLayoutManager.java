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

package de.msal.shoutemo.ui.chat;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

/**
 * @since 10.11.14
 */
public class ListLayoutManager extends LinearLayoutManager {

   public ListLayoutManager(Context context) {
      super(context, VERTICAL, false);
      setStackFromEnd(true);
      setSmoothScrollbarEnabled(true);
   }

   @Override
   public void onItemsChanged(RecyclerView recyclerView) {
      super.onItemsChanged(recyclerView);
   }

}
