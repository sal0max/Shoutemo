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

package de.msal.shoutemo.adapters;

import com.bumptech.glide.Glide;

import android.content.Context;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

import de.msal.shoutemo.R;

public class EmoticonsAdapter extends BaseAdapter {

    private final Context context;
    private final OnEmoticonClickListener emoticonClickListener;
    private final List<Pair<Integer, String>> emoticons = new ArrayList<>();

    public EmoticonsAdapter(Context context, OnEmoticonClickListener emoticonClickListener) {
        this.context = context;
        this.emoticonClickListener = emoticonClickListener;

        /* read all emoticons to local cache */
        emoticons.add(Pair.create(R.drawable.smil_amaze, ":amaze:"));
        emoticons.add(Pair.create(R.drawable.smil_angel, ":angel:"));
        emoticons.add(Pair.create(R.drawable.smil_angry, ":angry:"));
        emoticons.add(Pair.create(R.drawable.smil_anime, "^_^"));
        emoticons.add(Pair.create(R.drawable.smil_awesome, ":awesome:"));
        emoticons.add(Pair.create(R.drawable.smil_bee, ":bee:"));
        emoticons.add(Pair.create(R.drawable.smil_blush, "^^;"));
        emoticons.add(Pair.create(R.drawable.smil_bye, ":bye:"));
        emoticons.add(Pair.create(R.drawable.smil_cheeky, ":cheeky:"));
        emoticons.add(Pair.create(R.drawable.smil_chicky, ":chicky:"));
        emoticons.add(Pair.create(R.drawable.smil_chimpy, ":{|)"));
        emoticons.add(Pair.create(R.drawable.smil_coffee, "~O)"));
        emoticons.add(Pair.create(R.drawable.smil_cool, ":cool:"));
        emoticons.add(Pair.create(R.drawable.smil_cool2, "B)"));
        emoticons.add(Pair.create(R.drawable.smil_cry, ":crazy:"));
        emoticons.add(Pair.create(R.drawable.smil_doh, ":doh:"));
        emoticons.add(Pair.create(R.drawable.smil_evil, ":evil:"));
        emoticons.add(Pair.create(R.drawable.smil_finger, ":upyours:"));
        emoticons.add(Pair.create(R.drawable.smil_frown, ":("));
        emoticons.add(Pair.create(R.drawable.smil_heart, "<3"));
        emoticons.add(Pair.create(R.drawable.smil_hmm, ":hmm:"));
        emoticons.add(Pair.create(R.drawable.smil_irritated, ":/"));
        emoticons.add(Pair.create(R.drawable.smil_j_boss, ":j_hui:"));
        emoticons.add(Pair.create(R.drawable.smil_kiss, ":kiss:"));
        emoticons.add(Pair.create(R.drawable.smil_laugh, "XD"));
        emoticons.add(Pair.create(R.drawable.smil_lookleft, ":lookleft:"));
        emoticons.add(Pair.create(R.drawable.smil_lookright, ":lookright:"));
        emoticons.add(Pair.create(R.drawable.smil_nerd, ":nerd:"));
        emoticons.add(Pair.create(R.drawable.smil_neutral, ":|"));
        emoticons.add(Pair.create(R.drawable.smil_ninja, ":ninja:"));
        emoticons.add(Pair.create(R.drawable.smil_omg, ":omg:"));
        emoticons.add(Pair.create(R.drawable.smil_rage, ":rage:"));
        emoticons.add(Pair.create(R.drawable.smil_rolleyes, ":roll:"));
        emoticons.add(Pair.create(R.drawable.smil_rose, "@};-"));
        emoticons.add(Pair.create(R.drawable.smil_sad, ":sad:"));
        emoticons.add(Pair.create(R.drawable.smil_shock, ":shock:"));
        emoticons.add(Pair.create(R.drawable.smil_sleepy, ":tired:"));
        emoticons.add(Pair.create(R.drawable.smil_sleepy2, ":zzz:"));
        emoticons.add(Pair.create(R.drawable.smil_smile, ":)"));
        emoticons.add(Pair.create(R.drawable.smil_smile2, ":D"));
        emoticons.add(Pair.create(R.drawable.smil_sneaky, "!)"));
        emoticons.add(Pair.create(R.drawable.smil_star, ":star:"));
        emoticons.add(Pair.create(R.drawable.smil_surprise, "=O"));
        emoticons.add(Pair.create(R.drawable.smil_teeth, ":mrteeth:"));
        emoticons.add(Pair.create(R.drawable.smil_thumb_down, ":-q"));
        emoticons.add(Pair.create(R.drawable.smil_thumb_up, ":-d"));
        emoticons.add(Pair.create(R.drawable.smil_tongue, ":P"));
        emoticons.add(Pair.create(R.drawable.smil_trollface, ":troll:"));
        emoticons.add(Pair.create(R.drawable.smil_wah, ";|"));
        emoticons.add(Pair.create(R.drawable.smil_walla, ":walla:"));
        emoticons.add(Pair.create(R.drawable.smil_whut, "O.o"));
        emoticons.add(Pair.create(R.drawable.smil_willis, ":willis:"));
        emoticons.add(Pair.create(R.drawable.smil_wink, ";)"));
        emoticons.add(Pair.create(R.drawable.smil_woot, ":woot:"));
        emoticons.add(Pair.create(R.drawable.smil_worry, ":S"));
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ImageView imageView;

        if (convertView == null) {
            imageView = new ImageView(context);
            int touchArea = (int) context.getResources().getDimension(R.dimen.emoticon_area);
            imageView.setLayoutParams(new GridView.LayoutParams(touchArea, touchArea));
            imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        } else {
            imageView = (ImageView) convertView;
        }

        Glide.with(context).fromResource()
                .asGif()
                .crossFade(0)
                .load(getItem(position))
                .into(imageView);

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                emoticonClickListener.onEmoticonClick(emoticons.get(position).second);
            }
        });

        return imageView;
    }

    @Override
    public int getCount() {
        return emoticons.size();
    }

    @Override
    public Integer getItem(int position) {
        return emoticons.get(position).first;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public interface OnEmoticonClickListener {

        void onEmoticonClick(String bbcode);
    }

}
