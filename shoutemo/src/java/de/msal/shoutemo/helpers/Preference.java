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
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * A normal {@link android.preference.Preference} with one change: The view is disabled, to make it
 * not clickable and show now highlighting when clicked.
 *
 * One could also set {@code android:enabled="false"} via xml, but this changes how the view is
 * displayed. By usage of this class everything looks normal. Disabling it in xml is <b>not</b>
 * necessary.
 */
public class Preference extends android.preference.Preference {

    public Preference(Context context, AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);
    }

    public Preference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Preference(Context context) {
        super(context);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        parent.setEnabled(false); // Disable the view (= make it not clickable)
        return super.onCreateView(parent);
    }
}
