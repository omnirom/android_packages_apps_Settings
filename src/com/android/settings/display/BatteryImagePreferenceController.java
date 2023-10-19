/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.settings.display;

import static org.omnirom.omnilib.utils.OmniSettings.OMNI_SHOW_BATTERY_IMAGE;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.internal.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;

/**
 * A controller to manage the switch for showing battery image in the status bar.
 */

public class BatteryImagePreferenceController extends BasePreferenceController implements
        PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    public BatteryImagePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        int setting = Settings.System.getInt(mContext.getContentResolver(),
                OMNI_SHOW_BATTERY_IMAGE, 1);

        ((SwitchPreference) preference).setChecked(setting == 1);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean showImage = (Boolean) newValue;
        Settings.System.putInt(mContext.getContentResolver(), OMNI_SHOW_BATTERY_IMAGE,
                showImage ? 1 : 0);
        return true;
    }
}
