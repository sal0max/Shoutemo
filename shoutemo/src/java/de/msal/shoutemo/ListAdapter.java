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
import android.database.Cursor;
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

    private final int SHOUT = 0;

    private final int THREAD = 1;

    private final int AWARD = 2;

    private final int GLOBAL = 3;

    private Context context;

    public ListAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        this.context = context;
    }

    private int getItemViewType(Cursor cursor) {
        String messageType = cursor
                .getString(cursor.getColumnIndex(ChatDb.Messages.COLUMN_NAME_TYPE));

        if (messageType.equals(Message.Type.SHOUT.name())) {
            return SHOUT;
        } else if (messageType.equals(Message.Type.THREAD.name())) {
            return THREAD;
        } else if (messageType.equals(Message.Type.AWARD.name())) {
            return AWARD;
        } else if (messageType.equals(Message.Type.GLOBAL.name())) {
            return GLOBAL;
        } else {
            return -1;
        }
    }

    @Override
    public int getItemViewType(int position) {
        Cursor cursor = (Cursor) getItem(position);

        return getItemViewType(cursor);
    }

    @Override
    public int getViewTypeCount() {
        return 5;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = null;

        switch (getItemViewType(cursor.getPosition())) {
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
    public void bindView(View view, Context context, Cursor cursor) {
        TextView message, textTime, author;
        long timestamp = cursor.getLong(cursor
                .getColumnIndex(ChatDb.Messages.COLUMN_NAME_TIMESTAMP));

        switch (getItemViewType(cursor.getPosition())) {
            case SHOUT:
                message = (TextView) view.getTag(R.id.listrow_shout_message);
                textTime = (TextView) view.getTag(R.id.listrow_shout_timestamp);
                author = (TextView) view.getTag(R.id.listrow_shout_author);

                message.setText(cursor.getString(
                        cursor.getColumnIndex(ChatDb.Messages.COLUMN_NAME_MESSAGE_TEXT)));
                author.setText(cursor.getString(
                        cursor.getColumnIndex(ChatDb.Messages.COLUMN_NAME_AUTHOR_NAME)));
                textTime.setText(TimeUtils.getRelativeTime(context, timestamp));

            /* show the right author color (mod/admin) */
                String authorType = cursor
                        .getString(cursor.getColumnIndex(ChatDb.Authors.COLUMN_NAME_TYPE));
                if (authorType != null && authorType.equals(Author.Type.MOD.name())) {
                    author.setTextColor(
                            this.context.getResources().getColor(R.color.autemo_green_secondary));
                } else if (authorType != null && authorType.equals(Author.Type.ADMIN.name())) {
                    author.setTextColor(this.context.getResources().getColor(R.color.autemo_blue));
                } else {
                    author.setTextColor(
                            this.context.getResources().getColor(R.color.autemo_grey_bright));
                }
                break;
            case THREAD:
                message = (TextView) view.getTag(R.id.listrow_thread_message);
                textTime = (TextView) view.getTag(R.id.listrow_thread_timestamp);

                message.setText(cursor.getString(
                        cursor.getColumnIndex(ChatDb.Messages.COLUMN_NAME_MESSAGE_TEXT)));
                textTime.setText(TimeUtils.getRelativeTime(context, timestamp));

                break;
            case AWARD:
                break;
            case GLOBAL:
                message = (TextView) view.getTag(R.id.listrow_global_message);
                textTime = (TextView) view.getTag(R.id.listrow_global_timestamp);

                message.setText(cursor.getString(
                        cursor.getColumnIndex(ChatDb.Messages.COLUMN_NAME_MESSAGE_TEXT)));
                textTime.setText(TimeUtils.getRelativeTime(context, timestamp));

                break;
        }
    }

}
