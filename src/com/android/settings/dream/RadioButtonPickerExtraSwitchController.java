/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.settings.dream;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.RestrictedSwitchPreference;

/**
 * Controller for a switch preference that can be added to a
 * {@link com.android.settings.widget.RadioButtonPickerFragment}.
 */
public class RadioButtonPickerExtraSwitchController implements OnPreferenceChangeListener {
    private static final String PREFERENCE_KEY = "restricted_to_wireless_charging";

    private final RestrictedSwitchPreference mPreference;
    private final PreferenceAccessor mPreferenceAccessor;

    /**
     * Interface for an object that is responsible for setting and getting the preference for which
     * this controller is responsible.
     */
    public interface PreferenceAccessor {
        /** Set the preference to the given value. */
        void setValue(boolean value);

        /** Get the current value of the preference. */
        boolean getValue();
    }

    public RadioButtonPickerExtraSwitchController(
            @NonNull Context context,
            int titleResId,
            @NonNull PreferenceAccessor preferenceAccessor) {
        mPreferenceAccessor = preferenceAccessor;

        mPreference = new RestrictedSwitchPreference(context);
        if (titleResId != 0) {
            mPreference.setTitle(titleResId);
        }
        mPreference.setChecked(mPreferenceAccessor.getValue());
        mPreference.setKey(PREFERENCE_KEY);
        mPreference.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, @NonNull Object newValue) {
        mPreferenceAccessor.setValue((Boolean) newValue);
        return true;
    }

    /** Add this controller's preference to the given {@link PreferenceScreen}. */
    public void addToScreen(@NonNull PreferenceScreen screen) {
        screen.addPreference(mPreference);
    }
}
