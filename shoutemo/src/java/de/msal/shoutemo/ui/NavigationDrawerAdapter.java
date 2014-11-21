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

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import de.msal.shoutemo.R;

/**
 * @since 21.11.14
 */
public class NavigationDrawerAdapter extends ArrayAdapter {

    private final String[] mEntries;
    private final int[] mDrawables;

    public NavigationDrawerAdapter(Context context, int resource) {
        super(context, resource);
        mEntries = new String[]{
                context.getString(R.string.menu_chat),
                context.getString(R.string.menu_users_online),
                null,
                context.getString(R.string.menu_prefs)
        };
        mDrawables = new int[]{
                R.drawable.ic_whatshot_white_24dp,
                R.drawable.ic_people_white_24dp,
                0,
                R.drawable.ic_settings_white_24dp
        };
    }

    @Override
    public int getCount() {
        return mEntries.length;
    }

    @Override
    public Object getItem(int position) {
        return mEntries[position];
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.listrow_navigationdrawer, parent, false);
        }
        ImageView ivIcon = (ImageView) convertView.findViewById(R.id.listrow_navigationdrawer__icon);
        TextView tvText = (TextView) convertView.findViewById(R.id.listrow_navigationdrawer__text);

        // normal, clickable list entry
        if (getItem(position) != null) {
            ivIcon.setImageDrawable(getContext().getResources().getDrawable(mDrawables[position]));
            tvText.setText(mEntries[position]);
        }
        // separator line
        else {
            tvText.setVisibility(View.GONE);
            ivIcon.setImageDrawable(getContext().getResources().getDrawable(R.color.autemo_grey_bright));

            ViewGroup.LayoutParams layoutParams = ivIcon.getLayoutParams();
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            layoutParams.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getContext().getResources().getDisplayMetrics());
            ivIcon.setLayoutParams(layoutParams);
        }
        return convertView;
    }

    @Override
    public boolean isEnabled(int position) {
        return getItem(position) != null;
    }
}
