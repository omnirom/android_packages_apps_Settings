/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.display.darkmode;

import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.widget.MainSwitchPreference;

/**
 * Controller for activate/deactivate night mode button
 */
public class DarkModeActivationPreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    private final UiModeManager mUiModeManager;
    private final MetricsFeatureProvider mMetricsFeatureProvider;

    public DarkModeActivationPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mUiModeManager = context.getSystemService(UiModeManager.class);
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    @Override
    public final void updateState(Preference preference) {
        final boolean active = (mContext.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_YES) != 0;
        ((MainSwitchPreference) preference).setChecked(active);
    }

    @Override
    public CharSequence getSummary() {
        final boolean isActivated = (mContext.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_YES) != 0;
        return AutoDarkTheme.getStatus(mContext, isActivated);
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, @NonNull Object newValue) {
        mMetricsFeatureProvider.logClickedPreference(preference, getMetricsCategory());
        final boolean active = (mContext.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_YES) != 0;
        mUiModeManager.setNightModeActivated(!active);
        return true;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE_UNSEARCHABLE;
    }
}
