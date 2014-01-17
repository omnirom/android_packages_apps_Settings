/*
 * Copyright (C) 2014 ANimeROM
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.ArrayList;
import java.util.Collections;

public class About extends SettingsPreferenceFragment {

    public static final String TAG = "About";

    private static final String ANIME_REVIEW = ""; //Github of AnimeROM PUT HERE
    private static final String ANIME_TWITTER = "https://twitter.com/AnimeRom"; // twiter ??
    private static final String XPERIAFAN13 = ""; //here your direction
    private static final String KLOZZ = ""; // here your direction

    Preference mReviewUrl;
    Preference mxperiafan13;
    Preference mklozz;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.about_Gummy);
        addPreferencesFromResource(R.xml.prefs_about);

        mReviewUrl = findPreference("anime_gerrit");
        mxperiafan13 = findPreference("xperiafan13");
        mklozz = findPreference("klozz");

    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mReviewUrl) {
            launchUrl(ANIME_REVIEW);
            return true;
        } else if (preference == mxperiafan13) {
            launchUrl(CPHELPS76);
            return true;
        } else if (preference == mklozz) {
            launchUrl(KEJAR31);
            return true;
        } else if (preference == mtdm) {
            launchUrl(ANIME_TWITTER);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void launchUrl(String url) {
        Uri uriUrl = Uri.parse(url);
        Intent whatever = new Intent(Intent.ACTION_VIEW, uriUrl);
        getActivity().startActivity(whatever);
    }
}
