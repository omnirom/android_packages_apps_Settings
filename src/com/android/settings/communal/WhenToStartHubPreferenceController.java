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

package com.android.settings.communal;

import static android.provider.Settings.Secure.GLANCEABLE_HUB_START_CHARGING;
import static android.provider.Settings.Secure.GLANCEABLE_HUB_START_CHARGING_UPRIGHT;
import static android.provider.Settings.Secure.GLANCEABLE_HUB_START_DOCKED;
import static android.provider.Settings.Secure.GLANCEABLE_HUB_START_NEVER;
import static android.provider.Settings.Secure.WHEN_TO_START_GLANCEABLE_HUB;

import android.annotation.StringRes;
import android.content.Context;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;

/**
 * A preference controller that is responsible for showing the "when to auto start hub" setting in
 * hub settings.
 */
public class WhenToStartHubPreferenceController extends BasePreferenceController implements
        PreferenceControllerMixin {
    public WhenToStartHubPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        preference.setSummary(getSummaryResId());
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        return mContext.getString(getSummaryResId());
    }

    @StringRes
    private int getSummaryResId() {
        final int setting = Settings.Secure.getInt(
                mContext.getContentResolver(),
                WHEN_TO_START_GLANCEABLE_HUB,
                GLANCEABLE_HUB_START_NEVER);

        switch (setting) {
            case GLANCEABLE_HUB_START_CHARGING:
                return R.string.when_to_show_hubmode_charging;
            case GLANCEABLE_HUB_START_DOCKED:
                return R.string.when_to_show_hubmode_docked;
            case GLANCEABLE_HUB_START_CHARGING_UPRIGHT:
                return R.string.when_to_show_hubmode_charging_and_upright;
            case GLANCEABLE_HUB_START_NEVER:
            default:
                return R.string.when_to_show_hubmode_never;
        }
    }
}
