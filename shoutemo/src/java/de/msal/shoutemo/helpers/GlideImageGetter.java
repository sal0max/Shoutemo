/*
 * Copyright 2015 Maximilian Salomon.
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

package de.msal.shoutemo.helpers;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.ViewTarget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.util.Log;
import android.widget.TextView;

import de.msal.shoutemo.R;

/**
 * Implements {@link android.text.Html.ImageGetter} in order to show images in the TextView.
 * <p/>
 * Uses {@link com.bumptech.glide.request.target.ViewTarget} to make an asynchronous HTTP GET to
 * load the image.
 *
 * @since 24.03.15
 */
public final class GlideImageGetter implements Html.ImageGetter, Drawable.Callback {

    private static final String TAG = "Shoutemo|UrlImageGetter";

    private final Context mContext;

    private final TextView mTextView;

    public GlideImageGetter(Context context, TextView textView) {
        this.mContext = context;
        this.mTextView = textView;
        // save Drawable.Callback in TextView and get back when finish fetching image from Internet
        mTextView.setTag(R.id.callback, this);
    }

    /**
     * We download image depends on settings and Wi-Fi status, but download image from server
     * (emoticons or something others) at any time
     */
    @Override
    public Drawable getDrawable(String url) {
        UrlDrawable urlDrawable = new UrlDrawable();

        /* === Emoticons === */
        if (url.startsWith("images/smileys/")) {
            try {
                String smiley = url.substring(url.lastIndexOf('/') + 1, url.lastIndexOf('.'));
                int id = mContext.getResources().getIdentifier("smil_" + smiley,
                        "drawable", mContext.getPackageName());
                Glide.with(mContext)
                        .fromResource()
                        .load(id)
                        .crossFade(0)
                        .into(new ImageGetterViewTarget(mTextView, urlDrawable));
            } catch (Resources.NotFoundException e) {
                Log.w(TAG, "Unkown emoticon showed up");
            }
        } else {
            /* === "real" images === */
            Log.v(TAG, "now load the image");
            Glide.with(mContext)
                    .load(url)
                    .crossFade(0)
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(new ImageGetterViewTarget(mTextView, urlDrawable));
        }

        return urlDrawable;
    }

    /**
     * Implements {@link Drawable.Callback} in order to redraw the TextView which contains the
     * animated GIFs.
     */
    @Override
    public void invalidateDrawable(Drawable who) {
        mTextView.invalidate();
    }

    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when) {

    }

    @Override
    public void unscheduleDrawable(Drawable who, Runnable what) {

    }

    private static class ImageGetterViewTarget extends ViewTarget<TextView, GlideDrawable> {

        private final UrlDrawable mDrawable;

        private ImageGetterViewTarget(TextView view, UrlDrawable drawable) {
            super(view);

            this.mDrawable = drawable;
        }

        @Override
        public void onResourceReady(GlideDrawable resource,
                GlideAnimation<? super GlideDrawable> glideAnimation) {
            // resize this drawable's width & height to fit its container
            final int resWidth = resource.getIntrinsicWidth();
            final int resHeight = resource.getIntrinsicHeight();
            int width, height;
            if (getView().getWidth() >= resWidth) {
                width = resWidth;
                height = resHeight;
            } else {
                width = getView().getWidth();
                height = (int) (resHeight / (1.0 * resWidth / width));
            }

            Rect rect = new Rect(0, 0, width, height);
            resource.setBounds(rect);

            mDrawable.setBounds(rect);
            mDrawable.setDrawable(resource);

            if (resource.isAnimated()) {
                // set callback to drawable in order to
                // signal its container to be redrawn
                // to show the animated GIF
                mDrawable.setCallback((Drawable.Callback) getView().getTag(R.id.callback));
                resource.setLoopCount(GlideDrawable.LOOP_FOREVER);
                resource.start();
            }

//            getView().invalidate();
            getView().setText(getView().getText());
        }

        /**
         * See https://github.com/bumptech/glide/issues/256
         *
         * @see com.bumptech.glide.GenericRequestBuilder#into(com.bumptech.glide.request.target.Target)
         */
        @Override
        public Request getRequest() {
            return null;
        }

        @Override
        public void setRequest(Request request) {

        }
    }
}
