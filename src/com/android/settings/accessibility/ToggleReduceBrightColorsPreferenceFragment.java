/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static com.android.internal.accessibility.AccessibilityShortcutController.REDUCE_BRIGHT_COLORS_COMPONENT_NAME;

import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.hardware.display.ColorDisplayManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.accessibility.extradim.ui.ExtraDimScreen;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

/** Settings for reducing brightness. */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class ToggleReduceBrightColorsPreferenceFragment extends BaseSupportFragment {
    private static final String TAG = "ToggleReduceBrightColorsPreferenceFragment";

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (!Flags.catalystExtraDim()) {
            ToggleShortcutPreferenceController shortcutPreferenceController =
                    use(ToggleShortcutPreferenceController.class);
            if (shortcutPreferenceController != null) {
                shortcutPreferenceController.initialize(
                        getFeatureComponentName(),
                        getChildFragmentManager(),
                        getFeatureName(),
                        getMetricsCategory()
                );
            }
        }
    }

    @NonNull
    public CharSequence getFeatureName() {
        return getString(R.string.reduce_bright_colors_preference_title);
    }

    @NonNull
    private ComponentName getFeatureComponentName() {
        return REDUCE_BRIGHT_COLORS_COMPONENT_NAME;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.REDUCE_BRIGHT_COLORS_SETTINGS;
    }

    @Override
    public int getHelpResource() {
        // TODO(b/170973645): Link to help support page
        return 0;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.accessibility_extra_dim_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public @Nullable String getPreferenceScreenBindingKey(@NonNull Context context) {
        return ExtraDimScreen.KEY;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(
                    Flags.catalystExtraDim()
                            && com.android.settings.flags.Flags.catalystSettingsSearch()
                            ? 0 : R.xml.accessibility_extra_dim_settings) {

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return ColorDisplayManager.isReduceBrightColorsAvailable(context);
                }
            };
}
