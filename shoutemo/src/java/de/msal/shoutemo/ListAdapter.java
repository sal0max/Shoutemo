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
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import de.msal.shoutemo.connector.model.Author;
import de.msal.shoutemo.connector.model.Message;
import de.msal.shoutemo.db.ChatDb;
import de.msal.shoutemo.helpers.TimeUtils;

public class ListAdapter extends CursorAdapter {

    private Context context;

    public ListAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        this.context = context;
    }

    @Override
    public int getItemViewType(int position) {
        return getItemType(position).ordinal();
    }

    @Override
    public int getViewTypeCount() {
        return Message.Type.values().length;
    }

    private Message.Type getItemType(int position) {
        Cursor cursor = (Cursor) getItem(position);
        String message = cursor.getString(cursor.getColumnIndex(ChatDb.Messages.COLUMN_NAME_TYPE));
        Message.Type messageType = Message.Type.valueOf(message);
        return messageType;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = null;

        switch (getItemType(cursor.getPosition())) {
            case SHOUT:
                view = inflater.inflate(R.layout.listrow_shout, parent, false);

                view.setTag(R.id.listrow_shout_message,
                        view.findViewById(R.id.listrow_shout_message));
                view.setTag(R.id.listrow_shout_author,
                        view.findViewById(R.id.listrow_shout_author));
                view.setTag(R.id.listrow_shout_timestamp,
                        view.findViewById(R.id.listrow_shout_timestamp));
                break;
            case THREAD:
                view = inflater.inflate(R.layout.listrow_thread, parent, false);

                view.setTag(R.id.listrow_thread_message,
                        view.findViewById(R.id.listrow_thread_message));
                view.setTag(R.id.listrow_thread_timestamp,
                        view.findViewById(R.id.listrow_thread_timestamp));
                break;
            case AWARD:
                view = inflater.inflate(R.layout.listrow_award, parent, false);
                break;
            case GLOBAL:
                view = inflater.inflate(R.layout.listrow_global, parent, false);
                view.setTag(R.id.listrow_global_message,
                        view.findViewById(R.id.listrow_global_message));
                view.setTag(R.id.listrow_global_timestamp,
                        view.findViewById(R.id.listrow_global_timestamp));
                break;
        }
        return view;
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        TextView tvMessage, tvTimestamp, tvAuthor;
        String message, author;
        long timestamp = cursor.getLong(
                cursor.getColumnIndex(ChatDb.Messages.COLUMN_NAME_TIMESTAMP));

        // http://www.autemo.com/images/smileys/willis.jpg
        Html.ImageGetter imageGetter = new Html.ImageGetter() {
            public Drawable getDrawable(String source) {
                String smiley = "";
                try {
                    if (source.startsWith("http://www.autemo.com/images/smileys/")) {
                        smiley = source.substring(source.lastIndexOf('/') + 1,
                                source.lastIndexOf('.'));
                        int id = context.getResources().getIdentifier("smil_" + smiley,
                                "drawable", context.getPackageName());

                        Drawable d = context.getResources().getDrawable(id);
                        d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
                        return d;

                    } else {
                        Log.v("SHOUTEMO", "UNKNOWN IMAGE EMBEDDED: " + source);
                    }
                } catch (Resources.NotFoundException e) {
                    Log.e("SHOUTEMO", "UNKNOWN SMILEY SHOWED UP:" + smiley);
                }
                return context.getResources() // TODO: Better placeholder drawable
                        .getDrawable(android.R.drawable.ic_dialog_alert);
            }
        };

        switch (getItemType(cursor.getPosition())) {
            case SHOUT:
                tvMessage = (TextView) view.getTag(R.id.listrow_shout_message);
                tvTimestamp = (TextView) view.getTag(R.id.listrow_shout_timestamp);
                tvAuthor = (TextView) view.getTag(R.id.listrow_shout_author);

                message = cursor
                        .getString(cursor.getColumnIndex(ChatDb.Messages.COLUMN_NAME_MESSAGE_HTML));
                author = cursor.getString(
                        cursor.getColumnIndex(ChatDb.Messages.COLUMN_NAME_AUTHOR_NAME));

                tvMessage.setText(Html.fromHtml(message, imageGetter, null));
                tvAuthor.setText(author);
                tvTimestamp.setText(TimeUtils.getRelativeTime(context, timestamp));

                /* show the right tvAuthor color (mod/admin) */
                Author.Type authorType = Author.Type.valueOf(cursor.getString(
                        cursor.getColumnIndex(ChatDb.Authors.COLUMN_NAME_TYPE)));
                switch (authorType) {
                    case ADMIN:
                        tvAuthor.setTextColor(
                                this.context.getResources().getColor(R.color.autemo_blue));
                        break;
                    case MOD:
                        tvAuthor.setTextColor(
                                this.context.getResources()
                                        .getColor(R.color.autemo_green_secondary));
                        break;
                    default:
                        tvAuthor.setTextColor(
                                this.context.getResources().getColor(R.color.autemo_grey_bright));
                        break;
                }

                break;
            case THREAD:
                tvMessage = (TextView) view.getTag(R.id.listrow_thread_message);
                tvTimestamp = (TextView) view.getTag(R.id.listrow_thread_timestamp);

                message = cursor
                        .getString(cursor.getColumnIndex(ChatDb.Messages.COLUMN_NAME_MESSAGE_TEXT));

                tvMessage.setText(message);
                tvTimestamp.setText(TimeUtils.getRelativeTime(context, timestamp));

                break;
            case AWARD:
                // Nothing to show here, yet
                break;
            case GLOBAL:
                tvMessage = (TextView) view.getTag(R.id.listrow_global_message);
                tvTimestamp = (TextView) view.getTag(R.id.listrow_global_timestamp);

                message = cursor.getString(
                        cursor.getColumnIndex(ChatDb.Messages.COLUMN_NAME_MESSAGE_TEXT));

                tvMessage.setText(message);
                tvTimestamp.setText(TimeUtils.getRelativeTime(context, timestamp));

                break;
        }
    }

}
