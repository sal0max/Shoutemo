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

package de.msal.shoutemo.helpers;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Html.ImageGetter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.TextView;

import de.msal.shoutemo.R;

/**
 * @since 30.12.13
 */
public class UrlImageGetter implements ImageGetter {

    private static final String TAG = "Shoutemo|UrlImageGetter";
    /**/
    private Context context;
    private TextView container;
    /**/
    private UrlDrawable urlDrawable;
    private MyTarget target;


    public UrlImageGetter(Context context, TextView view) {
        this.context = context;
        this.container = view;
    }

    public Drawable getDrawable(String source) {
        urlDrawable = new UrlDrawable();

        /* === Emoticons === */
        if (source.startsWith("images/smileys/")) {
            try {
                String smiley = source.substring(source.lastIndexOf('/') + 1,
                        source.lastIndexOf('.'));
                int id = context.getResources().getIdentifier("smil_" + smiley,
                        "drawable", context.getPackageName());

                Drawable d = context.getResources().getDrawable(id);
                if (d != null) {
                    d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
                    return d;
                }
            } catch (Resources.NotFoundException e) {
                Log.w(TAG, "Unkown emoticon showed up");
            }
        } else {
            /* === "real" images === */
            Log.v(TAG, "now load the image");
            target = new MyTarget();

            /* limit bitmap size to prevent OOM errors */
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);

            int displayWidth = size.x;
            int displayHeigt = size.y;
            int maxSize = 600;
            if (displayWidth < maxSize) maxSize = displayWidth;
            if (displayHeigt < maxSize) maxSize = displayHeigt;

            /* use proper scaling for different display densities */
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inDensity = DisplayMetrics.DENSITY_MEDIUM;
            options.inTargetDensity = context.getResources().getDisplayMetrics().densityDpi;
            options.inScaled = true;

            Picasso.with(context)
                    .load(source)
                    .noFade()
                    .resize(maxSize, maxSize, true)
                    .centerInside()
                    .withOptions(options)
                    .placeholder(R.drawable.ic_missing_image)
                    .error(R.drawable.ic_missing_image)
                    .into(target);
        }
        // return reference to URLDrawable where it will change with actual image from the src tag
        return urlDrawable;
    }

    @SuppressWarnings("deprecation")
    private class UrlDrawable extends BitmapDrawable {

        // the drawable that needs to get set
        protected Drawable drawable;

        @Override
        public void draw(Canvas canvas) {
            // override the draw to facilitate refresh function later
            if (drawable != null) {
                drawable.draw(canvas);
            }
        }
    }

    private class MyTarget implements Target {

        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
            Log.v(TAG, "bitmap loaded (from " + loadedFrom + ")");

            Drawable d = new BitmapDrawable(context.getResources(), bitmap);
            d.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
            urlDrawable.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
            urlDrawable.drawable = d;
            // redraw the image by invalidating the container
            container.invalidate();
            container.setText(container.getText());
        }

        @Override
        public void onBitmapFailed(Drawable drawable) {
            Log.v(TAG, "loading of the bitmap failed");
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            urlDrawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            urlDrawable.drawable = drawable;
            container.invalidate();
            container.setText(container.getText());
        }

        @Override
        public void onPrepareLoad(Drawable drawable) {
            Log.v(TAG, "preparing to load");
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            urlDrawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            urlDrawable.drawable = drawable;
            container.invalidate();
            container.setText(container.getText());
        }
    }
}
