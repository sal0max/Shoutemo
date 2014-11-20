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

package de.msal.shoutemo.ui.chat;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import de.msal.shoutemo.R;
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
      View view;

      if (getItemType(cursor.getPosition()).equals(Message.Type.SHOUT)) {
         view = inflater.inflate(R.layout.listrow_shout, parent, false);

         view.setTag(R.id.listrow_shout_message, view.findViewById(R.id.listrow_shout_message));
         view.setTag(R.id.listrow_shout_author, view.findViewById(R.id.listrow_shout_author));
         view.setTag(R.id.listrow_shout_timestamp,
               view.findViewById(R.id.listrow_shout_timestamp));
      } else {
         view = inflater.inflate(R.layout.listrow_event, parent, false);

         view.setTag(R.id.listrow_event_global_title,
               view.findViewById(R.id.listrow_event_global_title));
         view.setTag(R.id.listrow_event_icon, view.findViewById(R.id.listrow_event_icon));
         view.setTag(R.id.listrow_event_message, view.findViewById(R.id.listrow_event_message));
         view.setTag(R.id.listrow_event_timestamp,
               view.findViewById(R.id.listrow_event_timestamp));
      }

      return view;
   }

   @Override
   public void bindView(View view, final Context context, Cursor cursor) {

      TextView tvMessage, tvTimestamp, tvGlobalTitle;
      ImageView ivIcon;

      String message = cursor
            .getString(cursor.getColumnIndex(ChatDb.Messages.COLUMN_NAME_MESSAGE_HTML));
      String author = cursor
            .getString(cursor.getColumnIndex(ChatDb.Messages.COLUMN_NAME_AUTHOR_NAME));
      long timestamp = cursor
            .getLong(cursor.getColumnIndex(ChatDb.Messages.COLUMN_NAME_TIMESTAMP));

        /* SHOUTS */
      if (getItemType(cursor.getPosition()).equals(Message.Type.SHOUT)) {
         tvMessage = (TextView) view.getTag(R.id.listrow_shout_message);
         TextView tvAuthor = (TextView) view.getTag(R.id.listrow_shout_author);
         tvTimestamp = (TextView) view.getTag(R.id.listrow_shout_timestamp);

            /* set text */
         tvAuthor.setText(author);

            /* show the right tvAuthor color (mod/admin/member) */
         switch (Author.Type.valueOf(
               cursor.getString(cursor.getColumnIndex(ChatDb.Authors.COLUMN_NAME_TYPE)))) {
            case ADMIN:
               tvAuthor.setTextColor(
                     this.context.getResources().getColor(R.color.autemo_blue));
               break;
            case MOD:
               tvAuthor.setTextColor(
                     this.context.getResources().getColor(R.color.autemo_green_secondary));
               break;
            default:
               tvAuthor.setTextColor(
                     this.context.getResources().getColor(R.color.autemo_grey_bright));
               break;
         }
      }
        /* EVENTS */
      else {
         tvMessage = (TextView) view.getTag(R.id.listrow_event_message);
         tvTimestamp = (TextView) view.getTag(R.id.listrow_event_timestamp);
         tvGlobalTitle = (TextView) view.getTag(R.id.listrow_event_global_title);
         tvGlobalTitle.setVisibility(View.GONE);
         ivIcon = (ImageView) view.getTag(R.id.listrow_event_icon);

         RelativeLayout parent = ((RelativeLayout) tvMessage.getParent());

         switch (getItemType(cursor.getPosition())) {
            case THREAD:
                /* styling */
               parent.setBackgroundColor(
                     context.getResources().getColor(R.color.autemo_green_secondary));
               tvMessage.setTextColor(context.getResources().getColor(R.color.autemo_grey));
               tvTimestamp.setTextColor(
                     context.getResources().getColor(R.color.autemo_grey_bright));
               ivIcon.setImageResource(R.drawable.ic_event_thread);
                /* alter message */
               message = context.getResources().getString(R.string.thread_author, author)
                     + message;
               break;
            case AWARD:
                /* styling */
               parent.setBackgroundColor(
                     this.context.getResources().getColor(R.color.autemo_white_dirty));
               tvMessage.setTextColor(context.getResources().getColor(R.color.autemo_grey));
               tvTimestamp.setTextColor(
                     context.getResources().getColor(R.color.autemo_grey_bright));
               ivIcon.setImageResource(R.drawable.ic_event_award);
                /* alter message */
               message = this.context.getResources().getString(R.string.award_author, author)
                     + message;
               break;
            case GLOBAL:
                /* styling */
               parent.setBackgroundColor(
                     this.context.getResources().getColor(R.color.autemo_pink));
               tvMessage.setTextAppearance(context, android.R.style.TextAppearance_Medium);
               tvMessage.setTypeface(tvMessage.getTypeface(), Typeface.BOLD);
               tvMessage.setTextColor(
                     this.context.getResources().getColor(R.color.autemo_grey));
               tvMessage.setLinkTextColor(
                     this.context.getResources().getColor(R.color.autemo_yellow_dark));
               tvTimestamp.setTextColor(
                     this.context.getResources().getColor(R.color.autemo_trns_white));
               ivIcon.setImageResource(R.drawable.ic_event_global);
                /* show wanted elements */
               tvGlobalTitle.setVisibility(View.VISIBLE);
               break;
            case COMPETITION:
                /* styling */
               parent.setBackgroundColor(
                     this.context.getResources().getColor(R.color.autemo_blue));
               tvMessage.setTextColor(context.getResources().getColor(R.color.autemo_grey));
               tvMessage.setLinkTextColor(
                     context.getResources().getColor(R.color.autemo_white_dirty));
               tvTimestamp.setTextColor(
                     context.getResources().getColor(R.color.autemo_grey));
               ivIcon.setImageResource(R.drawable.ic_event_competition);
                /* alter message */
               message = this.context.getResources().getString(R.string.competition_author,
                     author) + message;
               break;
            case PROMOTION:
                /* styling */
               parent.setBackgroundColor(
                     this.context.getResources().getColor(R.color.autemo_orange));
               tvMessage.setTextColor(
                     context.getResources().getColor(R.color.autemo_white_dirty));
               tvTimestamp.setTextColor(
                     context.getResources().getColor(R.color.autemo_white_dirty));
               ivIcon.setImageResource(R.drawable.ic_event_promotion);
                /* alter message */
               message = this.context.getResources().getString(R.string.promotion, author)
                     + message;
               break;
         }
      }

      UrlImageGetter imageGetter = new UrlImageGetter(context, tvMessage);
      tvMessage.setText(Html.fromHtml(message, imageGetter, null));

      // make links clickable (disables click of entire row, too)
      tvMessage.setMovementMethod(LinkMovementMethod.getInstance());
      tvTimestamp.setText(TimeUtils.getRelativeTime(context, timestamp));
   }

}
