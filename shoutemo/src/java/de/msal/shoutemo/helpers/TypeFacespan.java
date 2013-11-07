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

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;
import android.util.LruCache;


/**
 * Style a {@link android.text.Spannable} with a custom {@link Typeface}.
 *
 * @author Tristan Waddington
 */
public class TypeFacespan extends MetricAffectingSpan {

    /**
     * An <code>LruCache</code> for previously loaded typefaces.
     */
    private static LruCache<String, Typeface> sTypefaceCache = new LruCache<String, Typeface>(12);

    private Typeface mTypeface;

    /**
     * Load the {@link Typeface} and apply to a {@link android.text.Spannable}.
     */
    public TypeFacespan(Context context, String typefaceName) {
        mTypeface = sTypefaceCache.get(typefaceName);
        context = context.getApplicationContext();

        if (mTypeface == null && context != null) {
            mTypeface = Typeface
                    .createFromAsset(context.getApplicationContext().getAssets(), typefaceName);

            // Cache the loaded Typeface
            sTypefaceCache.put(typefaceName, mTypeface);
        }
    }

    @Override
    public void updateMeasureState(TextPaint p) {
        p.setTypeface(mTypeface);
        // Note: This flag is required for proper typeface rendering
        p.setFlags(p.getFlags() | Paint.SUBPIXEL_TEXT_FLAG);
    }

    @Override
    public void updateDrawState(TextPaint tp) {
        tp.setTypeface(mTypeface);
        // Note: This flag is required for proper typeface rendering
        tp.setFlags(tp.getFlags() | Paint.SUBPIXEL_TEXT_FLAG);
    }
}