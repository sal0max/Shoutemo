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

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.util.SortedListAdapterCallback;
import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import de.msal.shoutemo.R;
import de.msal.shoutemo.connector.model.Message;
import de.msal.shoutemo.connector.model.Post;
import de.msal.shoutemo.helpers.GlideImageGetter;
import de.msal.shoutemo.helpers.TimeUtils;
import de.msal.shoutemo.widgets.ExtendedSortedList;

/**
 * @since 10.02.16
 */
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    /**
     * Interface definition for a callback to be invoked when an item has been longclicked.
     */
    public interface OnItemLongClickListener {
        void onItemLongClicked(int position, Post post);
    }

    /**
     * listener that receives notifications when an item is clicked
     */
    private OnItemLongClickListener mOnItemLongClickListener;
    private Context mContext;
    private ExtendedSortedList<Post> mPosts;

    public ChatAdapter(Context context, List<Post> posts) {
        mContext = context;
        mPosts = new ExtendedSortedList<>(Post.class, new SortedListAdapterCallback<Post>(this) {
            @Override
            public int compare(Post o1, Post o2) {
                return o1.getDate().compareTo(o2.getDate());
            }

            @Override
            public boolean areContentsTheSame(Post oldItem, Post newItem) {
                return oldItem.equals(newItem);
            }

            @Override
            public boolean areItemsTheSame(Post item1, Post item2) {
                return item1.getDate().equals(item2.getDate());
            }
        }, posts.size());
        mPosts.addAll(posts);
    }

    @Override
    public ChatAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View itemView;

        if (viewType == Message.Type.SHOUT.ordinal()) {
            itemView = inflater.inflate(R.layout.listrow_shout, parent, false);
            return new ViewHolder(itemView, Message.Type.SHOUT);
        } else {
            itemView = inflater.inflate(R.layout.listrow_event, parent, false);
            return new ViewHolder(itemView, Message.Type.GLOBAL);
        }
    }

    @Override
    public int getItemViewType(int position) {
        Message.Type type = getItemType(position);
        return type.ordinal();

    }

    private Message.Type getItemType(int position) {
        return mPosts.get(position).getMessage().getType();
    }

    @Override
    public void onBindViewHolder(ChatAdapter.ViewHolder holder, int position) {
        Post post = mPosts.get(position);

        long timestamp = post.getDate().getTime();
        String message = post.getMessage().getHtml();
        String author = null;
        if (post.getAuthor() != null) {
            author = post.getAuthor().getName();
        }

        /* SHOUTS */
        if (getItemType(position).equals(Message.Type.SHOUT)) {
            /* set text */
            holder.tvAuthor.setText(author);

            /* show the right tvAuthor color (mod/admin/member) */
            switch (post.getAuthor().getType()) {
                case ADMIN:
                    holder.tvAuthor.setTextColor(ContextCompat.getColor(mContext, R.color.autemo_blue));
                    break;
                case MOD:
                    holder.tvAuthor.setTextColor(ContextCompat.getColor(mContext, R.color.autemo_green_secondary));
                    break;
                default:
                    holder.tvAuthor.setTextColor(ContextCompat.getColor(mContext, R.color.autemo_grey_bright));
                    break;
            }
        }
        /* EVENTS */
        else {
            switch (post.getMessage().getType()) {
                case THREAD:
                /* styling */
                    holder.itemView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.autemo_green_secondary));
                    holder.tvMessage.setTextColor(ContextCompat.getColor(mContext, R.color.autemo_grey));
                    holder.tvTimestamp.setTextColor(ContextCompat.getColor(mContext, R.color.autemo_grey_bright));
                    holder.ivIcon.setImageResource(R.drawable.ic_event_thread);
                    holder.tvGlobalTitle.setVisibility(View.GONE);
                /* alter message */
                    message = mContext.getResources().getString(R.string.thread_author, author) + message;
                    break;
                case AWARD:
                /* styling */
                    holder.itemView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.autemo_white_dirty));
                    holder.tvMessage.setTextColor(ContextCompat.getColor(mContext, R.color.autemo_grey));
                    holder.tvTimestamp.setTextColor(ContextCompat.getColor(mContext, R.color.autemo_grey_bright));
                    holder.ivIcon.setImageResource(R.drawable.ic_event_award);
                    holder.tvGlobalTitle.setVisibility(View.GONE);
                /* alter message */
                    message = mContext.getResources().getString(R.string.award_author, author) + message;
                    break;
                case GLOBAL:
                /* styling */
                    holder.itemView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.autemo_pink));
                    holder.tvMessage.setTextAppearance(mContext, android.R.style.TextAppearance_Medium);
                    holder.tvMessage.setTypeface(holder.tvMessage.getTypeface(), Typeface.BOLD);
                    holder.tvMessage.setTextColor(ContextCompat.getColor(mContext, R.color.autemo_grey));
                    holder.tvMessage.setLinkTextColor(ContextCompat.getColor(mContext, R.color.autemo_yellow_dark));
                    holder.tvTimestamp.setTextColor(ContextCompat.getColor(mContext, R.color.autemo_trns_white));
                    holder.ivIcon.setImageResource(R.drawable.ic_event_global);
                /* show wanted elements */
                    holder.tvGlobalTitle.setVisibility(View.VISIBLE);
                    break;
                case COMPETITION:
                /* styling */
                    holder.itemView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.autemo_blue));
                    holder.tvMessage.setTextColor(ContextCompat.getColor(mContext, R.color.autemo_grey));
                    holder.tvMessage.setLinkTextColor(ContextCompat.getColor(mContext, R.color.autemo_white_dirty));
                    holder.tvTimestamp.setTextColor(ContextCompat.getColor(mContext, R.color.autemo_grey));
                    holder.ivIcon.setImageResource(R.drawable.ic_event_competition);
                    holder.tvGlobalTitle.setVisibility(View.GONE);
                /* alter message */
                    message = this.mContext.getResources().getString(R.string.competition_author, author) + message;
                    break;
                case PROMOTION:
                /* styling */
                    holder.itemView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.autemo_orange));
                    holder.tvMessage.setTextColor(ContextCompat.getColor(mContext, R.color.autemo_white_dirty));
                    holder.tvTimestamp.setTextColor(ContextCompat.getColor(mContext, R.color.autemo_white_dirty));
                    holder.ivIcon.setImageResource(R.drawable.ic_event_promotion);
                    holder.tvGlobalTitle.setVisibility(View.GONE);
                /* alter message */
                    message = this.mContext.getResources().getString(R.string.promotion, author) + message;
                    break;
            }
        }

        // message
        GlideImageGetter imageGetter = new GlideImageGetter(mContext, holder.tvMessage);
        holder.tvMessage.setText(Html.fromHtml(message, imageGetter, null));
        // time
        holder.tvTimestamp.setText(TimeUtils.getRelativeTime(mContext, timestamp));

    }

    @Override
    public int getItemCount() {
        return mPosts.size();
    }

    /**
     * Swap in the new posts. All old posts that aren't still present in the given post set will be
     * removed.
     *
     * @param posts The new posts to be used.
     * @return true if the total amount of items in this adapter is different then it was before the
     * insert
     */
    public boolean swap(List<Post> posts) {
        int postsCount = mPosts.size();
        mPosts.beginBatchedUpdates();
        // first remove all items of mPosts, that aren't also present in posts, but keeping the same ones
        mPosts.retainAll(posts);
        // then add all (there could be completely new ones!)
        mPosts.addAll(posts);
        mPosts.endBatchedUpdates();
        return mPosts.size() != postsCount;
    }

    /**
     * Adds/updates all new posts, and also keeping the old ones
     *
     * @param posts The new posts to be added.
     * @return true if the total amount of items in this adapter is different then it was before the
     * insert
     */
    public boolean addAll(List<Post> posts) {
        int postsCount = mPosts.size();
        mPosts.beginBatchedUpdates();
        for (Post post : posts) {
            mPosts.add(post);
        }
        mPosts.endBatchedUpdates();
        return mPosts.size() != postsCount;
    }

    /**
     * Register a callback to be invoked when this view is clicked.
     *
     * @param l The callback that will run
     */
    public void setOnItemLongClickListener(OnItemLongClickListener l) {
        mOnItemLongClickListener = l;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener {

        TextView tvMessage, tvTimestamp, tvGlobalTitle, tvAuthor;
        ImageView ivIcon;
        View itemView;

        public ViewHolder(View itemView, Message.Type messageType) {
            super(itemView);
            if (messageType == Message.Type.SHOUT) {
                tvMessage = (TextView) itemView.findViewById(R.id.listrow_shout_message);
                tvAuthor = (TextView) itemView.findViewById(R.id.listrow_shout_author);
                tvTimestamp = (TextView) itemView.findViewById(R.id.listrow_shout_timestamp);
            } else {
                tvGlobalTitle = (TextView) itemView.findViewById(R.id.listrow_event_global_title);
                ivIcon = (ImageView) itemView.findViewById(R.id.listrow_event_icon);
                tvMessage = (TextView) itemView.findViewById(R.id.listrow_event_message);
                tvTimestamp = (TextView) itemView.findViewById(R.id.listrow_event_timestamp);
            }
            this.itemView = itemView;

            // row longclick
            itemView.setOnLongClickListener(this);

            // show a nice background during onclick
            int[] attrs = new int[]{R.attr.selectableItemBackground};
            TypedArray typedArray = mContext.obtainStyledAttributes(attrs);
            int backgroundResource = typedArray.getResourceId(0, 0);
            itemView.setBackgroundResource(backgroundResource);
            itemView.setClickable(true);
            typedArray.recycle();

            // make links clickable
            tvMessage.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    boolean ret = false;
                    CharSequence text = ((TextView) v).getText();
                    Spannable stext = Spannable.Factory.getInstance().newSpannable(text);
                    TextView widget = (TextView) v;
                    int action = event.getAction();

                    if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
                        int x = (int) event.getX();
                        int y = (int) event.getY();

                        x -= widget.getTotalPaddingLeft();
                        y -= widget.getTotalPaddingTop();

                        x += widget.getScrollX();
                        y += widget.getScrollY();

                        Layout layout = widget.getLayout();
                        int line = layout.getLineForVertical(y);
                        int off = layout.getOffsetForHorizontal(line, x);

                        ClickableSpan[] link = stext.getSpans(off, off, ClickableSpan.class);

                        if (link.length != 0) {
                            if (action == MotionEvent.ACTION_UP) {
                                link[0].onClick(widget);
                            }
                            ret = true;
                        }
                    }
                    return ret;
                }
            });
        }

        @Override
        public boolean onLongClick(View v) {
            int position = getAdapterPosition();
            Post post = mPosts.get(position);
            if (post.getMessage().getType().equals(Message.Type.SHOUT) && mOnItemLongClickListener != null) {
                mOnItemLongClickListener.onItemLongClicked(position, post);
                return true;
            } else {
                return false;
            }
        }
    }
}
