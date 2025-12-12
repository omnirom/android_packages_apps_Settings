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

package com.android.settings.fuelgauge;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.IllustrationPreference;
import com.android.settingslib.widget.SettingsThemeHelper;

public class AdaptiveBatteryIllustrationPreferenceController extends BasePreferenceController {
    private static final String TAG = "AdaptiveBatteryIllustrationPreferenceController";
    private static final String ILLUSTRATION_PREFERENCE_KEY = "auto_awesome_battery";

    public AdaptiveBatteryIllustrationPreferenceController(@NonNull Context context) {
        super(context, ILLUSTRATION_PREFERENCE_KEY);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (SettingsThemeHelper.isExpressiveTheme(mContext)) {
            final IllustrationPreference illustration = (IllustrationPreference) preference;
            illustration.setLottieAnimationResId(R.raw.auto_awesome_battery_expressive_lottie);
            illustration.applyDynamicColor();
        }
    }
}
