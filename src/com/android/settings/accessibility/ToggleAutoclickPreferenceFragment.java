/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.accessibility;

import static com.android.internal.accessibility.AccessibilityShortcutController.AUTOCLICK_COMPONENT_NAME;

import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;

import androidx.annotation.NonNull;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

/**
 * Fragment for preference screen for settings related to Automatically click after mouse stops
 * feature.
 */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class ToggleAutoclickPreferenceFragment extends BaseSupportFragment {

    private static final String TAG = "AutoclickPrefFragment";

    @NonNull
    private ComponentName getFeatureComponentName() {
        return AUTOCLICK_COMPONENT_NAME;
    }

    @NonNull
    private CharSequence getFeatureName() {
        return getText(R.string.accessibility_autoclick_preference_title);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_TOGGLE_AUTOCLICK;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_autoclick;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.accessibility_autoclick_settings;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        // Set up delay controller.
        use(ToggleAutoclickDelayBeforeClickController.class).setFragment(this);

        // Set up the main switch controller.
        use(ToggleAutoclickMainSwitchPreferenceController.class).setFragment(this);

        ToggleShortcutPreferenceController shortcutPreferenceController =
                use(ToggleAutoclickShortcutPreferenceController.class);
        if (shortcutPreferenceController != null) {
            shortcutPreferenceController.initialize(
                    getFeatureComponentName(),
                    getChildFragmentManager(),
                    getFeatureName(),
                    getMetricsCategory()
            );
        }
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.accessibility_autoclick_settings);
}
