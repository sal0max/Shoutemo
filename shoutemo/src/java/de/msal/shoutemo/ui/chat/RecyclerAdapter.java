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
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import de.msal.shoutemo.R;
import de.msal.shoutemo.connector.model.Author;
import de.msal.shoutemo.connector.model.Message;
import de.msal.shoutemo.db.ChatDb;
import de.msal.shoutemo.helpers.TimeUtils;
import de.msal.shoutemo.helpers.UrlImageGetter;

/**
 * @since 08.11.14
 */
public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {

   private static final String TAG = "Shoutemo|MyAdapter";

   private Context mContext;
   private static Cursor mCursor;
   private static boolean mDataValid;
   private int mRowIdColumn;
   private NotifyingDataSetObserver mDataSetObserver;

   public static class ViewHolder extends RecyclerView.ViewHolder {

      private TextView mTvMessage, mTvTimestamp, mTvAuthor, mTvGlobalTitle;
      private ImageView mIvIcon;

      public ViewHolder(View itemView) {
         super(itemView);

            /* SHOUTS */
         if (mDataValid && getItemType(mCursor.getPosition()).equals(Message.Type.SHOUT)) {
            mTvMessage = (TextView) itemView.findViewById(R.id.listrow_shout_message);
            mTvTimestamp = (TextView) itemView.findViewById(R.id.listrow_shout_timestamp);
            mTvAuthor = (TextView) itemView.findViewById(R.id.listrow_shout_author);
         }
            /* EVENTS */
         else if (mDataValid) {
            mTvMessage = (TextView) itemView.findViewById(R.id.listrow_event_message);
            mTvTimestamp = (TextView) itemView.findViewById(R.id.listrow_event_timestamp);
            mTvGlobalTitle = (TextView) itemView.findViewById(R.id.listrow_event_global_title);
            mTvGlobalTitle.setVisibility(View.GONE);
            mIvIcon = (ImageView) itemView.findViewById(R.id.listrow_event_icon);
         }
      }
   }

   public RecyclerAdapter(Context context, Cursor c) {
      mContext = context;
      mCursor = c;

      mDataValid = (c != null);
      mRowIdColumn = mDataValid ? mCursor.getColumnIndex("_id") : -1;
      mDataSetObserver = new NotifyingDataSetObserver();
      if (mCursor != null) {
         mCursor.registerDataSetObserver(mDataSetObserver);
      }
   }

   @Override
   public long getItemId(int position) {
      if (mDataValid && mCursor != null && mCursor.moveToPosition(position)) {
         return mCursor.getLong(mRowIdColumn);
      }
      return 0;
   }

   @Override
   public void setHasStableIds(boolean hasStableIds) {
      super.setHasStableIds(true);
   }

   @Override
   public int getItemViewType(int position) {
      return getItemType(position).ordinal();
   }

   private static Message.Type getItemType(int position) {
      mCursor.moveToPosition(position);
      String message = mCursor.getString(mCursor.getColumnIndex(ChatDb.Messages.COLUMN_NAME_TYPE));
      return Message.Type.valueOf(message);
   }

   @Override
   public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
      LayoutInflater inflater = LayoutInflater.from(mContext);
      View view;

      if (getItemType(mCursor.getPosition()).equals(Message.Type.SHOUT)) {
         view = inflater.inflate(R.layout.listrow_shout, viewGroup, false);
      } else {
         view = inflater.inflate(R.layout.listrow_event, viewGroup, false);
      }

      return new ViewHolder(view);
   }

   @Override
   public void onBindViewHolder(ViewHolder viewHolder, int i) {

      String message = mCursor.getString(
            mCursor.getColumnIndex(ChatDb.Messages.COLUMN_NAME_MESSAGE_HTML));
      String author = mCursor.getString(
            mCursor.getColumnIndex(ChatDb.Messages.COLUMN_NAME_AUTHOR_NAME));
      long timestamp = mCursor.getLong(
            mCursor.getColumnIndex(ChatDb.Messages.COLUMN_NAME_TIMESTAMP));

        /* SHOUTS */
      if (getItemType(mCursor.getPosition()).equals(Message.Type.SHOUT)) {

            /* set text */
         viewHolder.mTvAuthor.setText(author);

            /* show the right tvAuthor color (mod/admin/member) */
         switch (Author.Type.valueOf(
               mCursor.getString(mCursor.getColumnIndex(ChatDb.Authors.COLUMN_NAME_TYPE)))) {
            case ADMIN:
               viewHolder.mTvAuthor.setTextColor(
                     mContext.getResources().getColor(R.color.autemo_blue));
               break;
            case MOD:
               viewHolder.mTvAuthor.setTextColor(mContext.getResources().getColor(R.color.autemo_green_secondary));
               break;
            default:
               viewHolder.mTvAuthor.setTextColor(
                     mContext.getResources().getColor(R.color.autemo_grey_bright));
               break;
         }
      }
        /* EVENTS */
      else {
         RelativeLayout parent = ((RelativeLayout) viewHolder.mTvMessage.getParent());

         switch (getItemType(mCursor.getPosition())) {
            case THREAD:
                /* styling */
               parent.setBackgroundColor(mContext.getResources().getColor(R.color.autemo_green_secondary));
               viewHolder.mTvMessage.setTextColor(
                     mContext.getResources().getColor(R.color.autemo_grey));
               viewHolder.mTvTimestamp.setTextColor(
                     mContext.getResources().getColor(R.color.autemo_grey_bright));
               viewHolder.mIvIcon.setImageResource(R.drawable.ic_event_thread);
                /* alter message */
               message = mContext.getResources().getString(R.string.thread_author, author) + message;
               break;
            case AWARD:
                /* styling */
               parent.setBackgroundColor(mContext.getResources().getColor(R.color.autemo_white_dirty));
               viewHolder.mTvMessage.setTextColor(
                     mContext.getResources().getColor(R.color.autemo_grey));
               viewHolder.mTvTimestamp.setTextColor(
                     mContext.getResources().getColor(R.color.autemo_grey_bright));
               viewHolder.mIvIcon.setImageResource(R.drawable.ic_event_award);
                /* alter message */
               message = mContext.getResources().getString(R.string.award_author, author) + message;
               break;
            case GLOBAL:
                /* styling */
               parent.setBackgroundColor(mContext.getResources().getColor(R.color.autemo_pink));
               viewHolder.mTvMessage.setTextAppearance(mContext,
                     android.R.style.TextAppearance_Medium);
               viewHolder.mTvMessage.setTypeface(viewHolder.mTvMessage.getTypeface(), Typeface.BOLD);
               viewHolder.mTvMessage.setTextColor(
                     mContext.getResources().getColor(R.color.autemo_grey));
               viewHolder.mTvMessage.setLinkTextColor(mContext.getResources().getColor(R.color.autemo_yellow_dark));
               viewHolder.mTvTimestamp.setTextColor(
                     mContext.getResources().getColor(R.color.autemo_trns_white));
               viewHolder.mIvIcon.setImageResource(R.drawable.ic_event_global);
                /* show wanted elements */
               viewHolder.mTvGlobalTitle.setVisibility(View.VISIBLE);
               break;
            case COMPETITION:
                /* styling */
               parent.setBackgroundColor(mContext.getResources().getColor(R.color.autemo_blue));
               viewHolder.mTvMessage.setTextColor(
                     mContext.getResources().getColor(R.color.autemo_grey));
               viewHolder.mTvMessage.setLinkTextColor(mContext.getResources().getColor(R.color.autemo_white_dirty));
               viewHolder.mTvTimestamp.setTextColor(
                     mContext.getResources().getColor(R.color.autemo_grey));
               viewHolder.mIvIcon.setImageResource(R.drawable.ic_event_competition);
                /* alter message */
               message = mContext.getResources().getString(R.string.competition_author, author) + message;
               break;
            case PROMOTION:
                /* styling */
               parent.setBackgroundColor(mContext.getResources().getColor(R.color.autemo_orange));
               viewHolder.mTvMessage.setTextColor(
                     mContext.getResources().getColor(R.color.autemo_white_dirty));
               viewHolder.mTvTimestamp.setTextColor(
                     mContext.getResources().getColor(R.color.autemo_white_dirty));
               viewHolder.mIvIcon.setImageResource(R.drawable.ic_event_promotion);
                /* alter message */
               message = mContext.getResources().getString(R.string.promotion, author) + message;
               break;
         }
      }

      UrlImageGetter imageGetter = new UrlImageGetter(mContext, viewHolder.mTvMessage);
      viewHolder.mTvMessage.setText(Html.fromHtml(message, imageGetter, null));

      // make links clickable (disables click of entire row, too)
      viewHolder.mTvMessage.setMovementMethod(LinkMovementMethod.getInstance());
      viewHolder.mTvTimestamp.setText(TimeUtils.getRelativeTime(mContext, timestamp));
   }

   @Override
   public int getItemCount() {
      if (mDataValid && mCursor != null) {
         return mCursor.getCount();
      }
      return 0;
   }

   public Cursor getCursor() {
      return mCursor;
   }

   /**
    * Change the underlying cursor to a new cursor. If there is an existing cursor it will be
    * closed.
    */
   public void changeCursor(Cursor cursor) {
      Cursor old = swapCursor(cursor);
      if (old != null) {
         old.close();
      }
   }

   /**
    * Swap in a new Cursor, returning the old Cursor. Unlike {@link #changeCursor(Cursor)}, the
    * returned old Cursor is <em>not</em> closed.
    */
   public Cursor swapCursor(Cursor newCursor) {
      if (newCursor == mCursor) {
         return null;
      }
      final Cursor oldCursor = mCursor;
      if (oldCursor != null && mDataSetObserver != null) {
         oldCursor.unregisterDataSetObserver(mDataSetObserver);
      }
      mCursor = newCursor;
      if (mCursor != null) {
         if (mDataSetObserver != null) {
            mCursor.registerDataSetObserver(mDataSetObserver);
         }
         mRowIdColumn = newCursor.getColumnIndexOrThrow("_id");
         mDataValid = true;
         notifyDataSetChanged();
      } else {
         mRowIdColumn = -1;
         mDataValid = false;
         notifyDataSetChanged();
         //There is no notifyDataSetInvalidated() method in RecyclerView.Adapter
      }
      return oldCursor;
   }

   private class NotifyingDataSetObserver extends DataSetObserver {

      @Override
      public void onChanged() {
         super.onChanged();
         mDataValid = true;
         notifyDataSetChanged();
      }

      @Override
      public void onInvalidated() {
         super.onInvalidated();
         mDataValid = false;
         notifyDataSetChanged();
         //There is no notifyDataSetInvalidated() method in RecyclerView.Adapter
      }
   }
}
