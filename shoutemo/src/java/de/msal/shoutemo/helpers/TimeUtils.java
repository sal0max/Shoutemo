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
import android.text.format.DateUtils;

import de.msal.shoutemo.R;

import static android.text.format.DateUtils.FORMAT_NUMERIC_DATE;
import static android.text.format.DateUtils.FORMAT_SHOW_DATE;

/**
 * @since 08.11.13
 */
public class TimeUtils {

    /**
     * Get relative time for date
     *
     * @return relative time
     */
    public static CharSequence getRelativeTime(final Context context, final long timestamp) {
        long now = System.currentTimeMillis();
        if (Math.abs(now - timestamp) > 60000) {
            return DateUtils.getRelativeDateTimeString(context, timestamp,
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.DAY_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE | FORMAT_SHOW_DATE | FORMAT_NUMERIC_DATE);
        } else {
            return context.getResources().getString(R.string.time_just_now);
        }
    }

}
