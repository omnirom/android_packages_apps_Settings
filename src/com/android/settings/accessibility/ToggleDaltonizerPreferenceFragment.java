/*
 * Copyright (C) 2013 The Android Open Source Project
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

import static com.android.internal.accessibility.AccessibilityShortcutController.DALTONIZER_COMPONENT_NAME;

import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.accessibility.colorcorrection.ui.ColorCorrectionScreen;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

/** Settings for daltonizer. */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class ToggleDaltonizerPreferenceFragment extends BaseSupportFragment {

    private static final String TAG = "ToggleDaltonizerPreferenceFragment";

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (!Flags.catalystDaltonizer()) {
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
            use(FeedbackButtonPreferenceController.class).initialize(
                    new FeedbackManager(context, getMetricsCategory()));
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final View rootView = getActivity().getWindow().peekDecorView();
        if (rootView != null) {
            rootView.setAccessibilityPaneTitle(getString(com.android.settingslib.R
                    .string.accessibility_display_daltonizer_preference_title));
        }
    }

    @NonNull
    private CharSequence getFeatureName() {
        return getText(com.android.settingslib.R
                .string.accessibility_display_daltonizer_preference_title);
    }

    @NonNull
    private ComponentName getFeatureComponentName() {
        return DALTONIZER_COMPONENT_NAME;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_TOGGLE_DALTONIZER;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_color_correction;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.accessibility_daltonizer_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public @Nullable String getPreferenceScreenBindingKey(
            @NonNull Context context) {
        return ColorCorrectionScreen.KEY;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(
                    Flags.catalystDaltonizer()
                            && com.android.settings.flags.Flags.catalystSettingsSearch() ? 0 :
                            R.xml.accessibility_daltonizer_settings);
}
