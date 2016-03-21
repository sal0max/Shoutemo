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
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.util.SortedListAdapterCallback;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import de.msal.shoutemo.R;
import de.msal.shoutemo.connector.model.Author;
import de.msal.shoutemo.helpers.CircleTransformation;
import de.msal.shoutemo.widgets.ExtendedSortedList;

/**
 * @since 14.11.14
 */
public class OnlineUsersAdapter extends RecyclerView.Adapter<OnlineUsersAdapter.ViewHolder> {

    OnUserClickListener mOnUserClickListener;

    Context mContext;
    ExtendedSortedList<Author> mAuthors;

    /**
     * Interface definition for a callback to be invoked when a item is clicked.
     */
    public interface OnUserClickListener {
        /**
         * Called when a view has been clicked.
         *
         * @param author The {@link Author} that was clicked.
         */
        void onUserClick(Author author);
    }

    public OnlineUsersAdapter(Context context, List<Author> authors) {
        mContext = context;
        mAuthors = new ExtendedSortedList<>(Author.class,
                new SortedListAdapterCallback<Author>(this) {
                    @Override
                    public int compare(Author o1, Author o2) {
                        return o1.getName().compareTo(o2.getName());
                    }

                    @Override
                    public boolean areContentsTheSame(Author oldItem, Author newItem) {
                        return oldItem.equals(newItem);
                    }

                    @Override
                    public boolean areItemsTheSame(Author item1, Author item2) {
                        return item1.equals(item2);
                    }
                }, authors.size());
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View itemView = inflater.inflate(R.layout.listrow_users, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Author author = mAuthors.get(position);
        holder.tvUsername.setText(author.getName());
        if (author.getAvatar() != null) {
            Glide.with(mContext)
                    .load(author.getAvatar())
                    .asBitmap()
                    .transform(new CircleTransformation(Glide.get(mContext).getBitmapPool()))
                    .into(holder.ivIcon);
        }
      /* show the right tvAuthor color (mod/admin/member) */
        switch (author.getType()) {
            case USER:
                holder.tvUsername.setTextColor(ContextCompat.getColor(mContext, android.R.color.secondary_text_dark));
                break;
            case ADMIN:
                holder.tvUsername.setTextColor(ContextCompat.getColor(mContext, R.color.autemo_blue));
                break;
            case MOD:
                holder.tvUsername.setTextColor(ContextCompat.getColor(mContext, R.color.autemo_green_secondary));
                break;
        }
    }

    @Override
    public int getItemCount() {
        return mAuthors.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mAuthors.get(position).getType().ordinal();
    }

    public void clear() {
        mAuthors.clear();
    }

    public void addAll(List<Author> authors) {
        mAuthors.beginBatchedUpdates();
        for (Author author : authors) {
            mAuthors.add(author);
        }
        mAuthors.endBatchedUpdates();
    }

    /**
     * Register a callback to be invoked when a user is clicked.
     *
     * @param l The callback that will run
     */
    public void setOnUserClickListener(OnUserClickListener l) {
        mOnUserClickListener = l;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        ImageView ivIcon;
        TextView tvUsername;

        public ViewHolder(View itemView) {
            super(itemView);
            ivIcon = (ImageView) itemView.findViewById(R.id.listrow_users__avatar);
            tvUsername = (TextView) itemView.findViewById(R.id.listrow_users__text);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            if (mOnUserClickListener != null) {
                mOnUserClickListener.onUserClick(mAuthors.get(position));
            }
        }
    }

}