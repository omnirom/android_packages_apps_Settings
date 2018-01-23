/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.display;

import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.AbstractPreferenceController;

public class AmbientDisplayMusicController extends AbstractPreferenceController implements
        PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String KEY_AMBIENT_MEDIA = "force_ambient_for_media";

    public AmbientDisplayMusicController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_AMBIENT_MEDIA;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        ListPreference pref = (ListPreference) preference;
        int ambientMediaValue = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.FORCE_AMBIENT_FOR_MEDIA, 0);
        int valueIndex = pref.findIndexOfValue(String.valueOf(ambientMediaValue));
        pref.setValueIndex(valueIndex >= 0 ? valueIndex : 0);
        pref.setSummary(pref.getEntry());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String value = (String) newValue;
        Settings.System.putInt(mContext.getContentResolver(), Settings.System.FORCE_AMBIENT_FOR_MEDIA, Integer.valueOf(value));
        return true;
    }
}
