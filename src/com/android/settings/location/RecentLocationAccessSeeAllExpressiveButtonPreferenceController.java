/*
 * Copyright 2021 The Android Open Source Project
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
package com.android.settings.location;

import android.content.Context;

import androidx.preference.PreferenceScreen;

import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.widget.SectionButtonPreference;
import com.android.settingslib.widget.SettingsThemeHelper;

import kotlin.Unit;

import org.jetbrains.annotations.Nullable;

/**
 * Preference controller that handles the "See All" button for recent location access.
 */
public class RecentLocationAccessSeeAllExpressiveButtonPreferenceController extends
        LocationBasePreferenceController {

    private @Nullable SectionButtonPreference mPreference;

    /**
     * Constructor of {@link RecentLocationAccessSeeAllExpressiveButtonPreferenceController}.
     */
    public RecentLocationAccessSeeAllExpressiveButtonPreferenceController(
            Context context, String key) {
        super(context, key);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = (SectionButtonPreference) screen.findPreference(getPreferenceKey());
        if (mPreference != null) {
            mPreference.setOnClickListener(v -> {
                SubSettingLauncher launcher = new SubSettingLauncher(mFragment.getContext())
                        .setDestination(RecentLocationAccessSeeAllFragment.class.getName())
                        .setSourceMetricsCategory(mFragment.getMetricsCategory());
                launcher.launch();
                return Unit.INSTANCE;
            });
        }
        mLocationEnabler.refreshLocationMode();
    }

    @Override
    public void onLocationModeChanged(int mode, boolean restricted) {
        boolean enabled = mLocationEnabler.isEnabled(mode);
        if (mPreference != null) {
            mPreference.setVisible(enabled && SettingsThemeHelper.isExpressiveTheme(mContext));
        }
    }
}
