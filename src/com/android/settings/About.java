/*
 * Copyright (C) 2014 AnimeROM
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

    private static final String ANIME_REVIEW = "https://github.com/AnimeROM"; //Github of AnimeROM PUT HERE
    private static final String ANIME_TWITTER = "https://github.com/AnimeROM"; // twiter or any ??
    private static final String XPERIAFAN13 = "https://www.facebook.com/miguelangel.sanchezbravo"; 
    private static final String KLOZZ = "https://twitter.com/klozz_"; 
    private static final String MACK = ""; // Any???

    Preference mReviewUrl;
    Preference mxperiafan13;
    Preference mklozz;
    Preference mtdm;
    Preference mMack;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.about_anime);
        addPreferencesFromResource(R.xml.prefs_about);

        mReviewUrl = findPreference("anime_gerrit");
        mxperiafan13 = findPreference("xperiafan13");
        mklozz = findPreference("klozz");
        mMack = findPreference("Mack");

    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mReviewUrl) {
            launchUrl(ANIME_REVIEW);
            return true;
        } else if (preference == mxperiafan13) {
            launchUrl(XPERIAFAN13);
            return true;
        } else if (preference == mklozz) {
            launchUrl(KLOZZ);
            return true;
        } else if (preference == mtdm) {
            launchUrl(ANIME_TWITTER);
            return true;
        } else if (preference == mMack) {
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
