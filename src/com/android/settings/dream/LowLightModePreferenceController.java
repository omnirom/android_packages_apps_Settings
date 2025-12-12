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

import static android.provider.Settings.Secure.LOW_LIGHT_DISPLAY_BEHAVIOR_LOW_LIGHT_CLOCK_DREAM;
import static android.provider.Settings.Secure.LOW_LIGHT_DISPLAY_BEHAVIOR_SCREEN_OFF;
import static android.os.Flags.lowLightDreamBehavior;

import android.annotation.StringRes;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.dream.DreamBackend;

public class LowLightModePreferenceController extends TogglePreferenceController {
    public static final String PREF_KEY = "low_light_mode";

    private final DreamBackend mBackend;
    @Nullable
    private Preference mPreference;

    public LowLightModePreferenceController(@NonNull Context context, DreamBackend dreamBackend) {
        super(context, PREF_KEY);

        mBackend = dreamBackend;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public boolean isChecked() {
        return mBackend.getLowLightDisplayBehaviorEnabled();
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        mBackend.setLowLightDisplayBehaviorEnabled(isChecked);
        if (mPreference != null) {
            mPreference.setSummary(getSummary());
        }
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setSummary(getSummary());
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_display;
    }

    @Override
    public int getAvailabilityStatus() {
        return lowLightDreamBehavior()
                && !DreamUtils.getLowLightBehaviors(mContext.getResources()).isEmpty()
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public CharSequence getSummary() {
        return getSummaryTextFromDreamBackend(mContext, mBackend);
    }

    static CharSequence getSummaryTextFromDreamBackend(Context context, DreamBackend backend) {
        if (backend.getLowLightDisplayBehaviorEnabled()) {
            return context.getString(
                    R.string.low_light_display_behavior_summary_on,
                    context.getString(getSummaryResId(backend)));
        } else {
            return context.getString(R.string.low_light_display_behavior_summary_off);
        }
    }

    @StringRes
    static int getSummaryResId(DreamBackend backend) {
        switch (backend.getLowLightDisplayBehavior()) {
            case LOW_LIGHT_DISPLAY_BEHAVIOR_SCREEN_OFF:
                return R.string.low_light_display_behavior_screen_off_full_summary;
            case LOW_LIGHT_DISPLAY_BEHAVIOR_LOW_LIGHT_CLOCK_DREAM:
            default:
                return R.string.low_light_display_behavior_low_light_clock_dream_full_summary;
        }
    }
}
