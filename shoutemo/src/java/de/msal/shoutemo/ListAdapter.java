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
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import de.msal.shoutemo.connector.model.Author;
import de.msal.shoutemo.connector.model.Message;
import de.msal.shoutemo.db.ChatDb;
import de.msal.shoutemo.helpers.TimeUtils;
import de.msal.shoutemo.helpers.UrlImageGetter;

public class ListAdapter extends CursorAdapter {

    private static final String TAG = "Shoutemo|ListAdapter";
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
        return Message.Type.valueOf(message);
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

                view.setTag(R.id.listrow_award_message,
                        view.findViewById(R.id.listrow_award_message));
                view.setTag(R.id.listrow_award_timestamp,
                        view.findViewById(R.id.listrow_award_timestamp));
                break;
            case GLOBAL:
                view = inflater.inflate(R.layout.listrow_global, parent, false);
                view.setTag(R.id.listrow_global_message,
                        view.findViewById(R.id.listrow_global_message));
                view.setTag(R.id.listrow_global_timestamp,
                        view.findViewById(R.id.listrow_global_timestamp));
                break;
            case COMPETITION:
                view = inflater.inflate(R.layout.listrow_competition, parent, false);
                view.setTag(R.id.listrow_competition_message,
                        view.findViewById(R.id.listrow_competition_message));
                view.setTag(R.id.listrow_competition_timestamp,
                        view.findViewById(R.id.listrow_competition_timestamp));
                break;
            case PROMOTION:
                view = inflater.inflate(R.layout.listrow_promotion, parent, false);
                view.setTag(R.id.listrow_promotion_message,
                        view.findViewById(R.id.listrow_promotion_message));
                view.setTag(R.id.listrow_promotion_timestamp,
                        view.findViewById(R.id.listrow_promotion_timestamp));
                break;
        }
        return view;
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        TextView tvMessage, tvTimestamp, tvAuthor;
        UrlImageGetter imageGetter;

        String author;
        String message = cursor
                .getString(cursor.getColumnIndex(ChatDb.Messages.COLUMN_NAME_MESSAGE_HTML));
        long timestamp = cursor.getLong(
                cursor.getColumnIndex(ChatDb.Messages.COLUMN_NAME_TIMESTAMP));

        switch (getItemType(cursor.getPosition())) {
            case SHOUT:
                tvMessage = (TextView) view.getTag(R.id.listrow_shout_message);
                tvTimestamp = (TextView) view.getTag(R.id.listrow_shout_timestamp);
                tvAuthor = (TextView) view.getTag(R.id.listrow_shout_author);
                imageGetter = new UrlImageGetter(context, tvMessage);

                // make links clickable (disables click of entire row, too)
                tvMessage.setMovementMethod(LinkMovementMethod.getInstance());

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
                imageGetter = new UrlImageGetter(context, tvMessage);

                // make links clickable (disables click of entire row, too)
                tvMessage.setMovementMethod(LinkMovementMethod.getInstance());

                author = cursor.getString(
                        cursor.getColumnIndex(ChatDb.Messages.COLUMN_NAME_AUTHOR_NAME));
                message = this.context.getResources().getString(R.string.thread_author, author)
                        + message;

                tvMessage.setText(Html.fromHtml(message, imageGetter, null));
                tvTimestamp.setText(TimeUtils.getRelativeTime(context, timestamp));

                break;
            case AWARD:
                tvMessage = (TextView) view.getTag(R.id.listrow_award_message);
                tvTimestamp = (TextView) view.getTag(R.id.listrow_award_timestamp);
                imageGetter = new UrlImageGetter(context, tvMessage);

                // make links clickable (disables click of entire row, too)
                tvMessage.setMovementMethod(LinkMovementMethod.getInstance());

                author = cursor.getString(
                        cursor.getColumnIndex(ChatDb.Messages.COLUMN_NAME_AUTHOR_NAME));
                message = this.context.getResources().getString(R.string.award_author, author)
                        + message;

                tvMessage.setText(Html.fromHtml(message, imageGetter, null));
                tvTimestamp.setText(TimeUtils.getRelativeTime(context, timestamp));

                break;
            case GLOBAL:
                tvMessage = (TextView) view.getTag(R.id.listrow_global_message);
                tvTimestamp = (TextView) view.getTag(R.id.listrow_global_timestamp);
                imageGetter = new UrlImageGetter(context, tvMessage);

                // make links clickable (disables click of entire row, too)
                tvMessage.setMovementMethod(LinkMovementMethod.getInstance());

                tvMessage.setText(Html.fromHtml(message, imageGetter, null));
                tvTimestamp.setText(TimeUtils.getRelativeTime(context, timestamp));

                break;
            case COMPETITION:
                tvMessage = (TextView) view.getTag(R.id.listrow_competition_message);
                tvTimestamp = (TextView) view.getTag(R.id.listrow_competition_timestamp);
                imageGetter = new UrlImageGetter(context, tvMessage);

                // make links clickable (disables click of entire row, too)
                tvMessage.setMovementMethod(LinkMovementMethod.getInstance());

                author = cursor.getString(
                        cursor.getColumnIndex(ChatDb.Messages.COLUMN_NAME_AUTHOR_NAME));
                message = this.context.getResources().getString(R.string.competition_author, author)
                        + message;

                tvMessage.setText(Html.fromHtml(message, imageGetter, null));
                tvTimestamp.setText(TimeUtils.getRelativeTime(context, timestamp));

                break;
            case PROMOTION:
                tvMessage = (TextView) view.getTag(R.id.listrow_promotion_message);
                tvTimestamp = (TextView) view.getTag(R.id.listrow_promotion_timestamp);

                author = cursor.getString(
                        cursor.getColumnIndex(ChatDb.Messages.COLUMN_NAME_AUTHOR_NAME));
                message = this.context.getResources().getString(R.string.promotion, author);

                tvMessage.setText(message);
                tvTimestamp.setText(TimeUtils.getRelativeTime(context, timestamp));
                break;
        }
    }

}
