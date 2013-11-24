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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

public class EmoticonsAdapter extends BaseAdapter {

    private final Context mContext;
    private final Drawable[] emoticons;


    public EmoticonsAdapter(Context context, Drawable[] emoticons) {
        this.mContext = context;
        this.emoticons = emoticons;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;

        if (convertView == null) {
            imageView = new ImageView(mContext);
            int touchArea = (int) mContext.getResources().getDimension(R.dimen.emoticon_area);
            imageView.setLayoutParams(
                    new GridView.LayoutParams(touchArea, touchArea));
            imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        } else {
            imageView = (ImageView) convertView;
        }

        final Drawable emoticon = emoticons[position];
        imageView.setImageDrawable(emoticon);

//        imageView.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mKeyClickListener.keyClickedIndex(emoticon);
//            }
//        });

        return imageView;
    }

    @Override
    public int getCount() {
        return emoticons.length;
    }

    @Override
    public Drawable getItem(int position) {
        return emoticons[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

}
