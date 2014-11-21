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

package de.msal.shoutemo.ui.preference;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Calendar;

import de.msal.shoutemo.R;
import de.msal.shoutemo.ui.TitleSetListener;

public class PreferenceFragment extends android.preference.PreferenceFragment implements
        Preference.OnPreferenceClickListener {

    private TitleSetListener mCallback;

    /**
     * Use this factory method to create a new instance of this fragment using the provided
     * parameters.
     *
     * @return A new instance of fragment LightMeterFragment.
     */
    public static PreferenceFragment newInstance() {
        PreferenceFragment fragment = new PreferenceFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public PreferenceFragment() {}

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallback = (TitleSetListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnHeadlineSelectedListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.pref_about);

        findPreference("notices").setOnPreferenceClickListener(this);
        try { /* show correct version name & copyright year */
            findPreference("about").setSummary(getString(R.string.pref_about_about_summary,
                    getActivity().getPackageManager()
                            .getPackageInfo(getActivity().getPackageName(), 0).versionName,
                    Calendar.getInstance().get(Calendar.YEAR)));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mCallback.setTitle(getString(R.string.menu_prefs));
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference.getKey().equals("notices")) {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse("http://bogo-d.deviantart.com"));
            startActivity(i);
        }
        return true;
    }

}
