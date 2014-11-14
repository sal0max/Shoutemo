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

package de.msal.shoutemo.ui.onlineusers;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import de.msal.shoutemo.R;
import de.msal.shoutemo.connector.model.Author;

/**
 * @since 14.11.14
 */
class OnlineUsersAdapter extends ArrayAdapter<Author> {

   OnlineUsersAdapter(Context context, List<Author> objects) {
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