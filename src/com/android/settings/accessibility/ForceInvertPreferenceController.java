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

package com.android.settings.accessibility;

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import android.content.Context;
import android.content.res.Configuration;
import android.provider.Settings;
import android.view.accessibility.Flags;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.search.SearchIndexableRaw;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import java.util.List;

/** A toggle preference controller for force invert (force dark). */
// LINT.IfChange
public class ForceInvertPreferenceController extends BasePreferenceController
        implements SelectorWithWidgetPreference.OnClickListener {

    @VisibleForTesting
    static final String STANDARD_DARK_THEME_KEY = "standard_dark_theme";
    @VisibleForTesting
    static final String EXPANDED_DARK_THEME_KEY = "expanded_dark_theme";

    @Nullable
    private SelectorWithWidgetPreference mStandardDarkThemePreference;
    @Nullable
    private SelectorWithWidgetPreference mExpandedDarkThemePreference;

    public ForceInvertPreferenceController(
            @NonNull Context context, @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return Flags.forceInvertColor() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_accessibility;
    }

    @Override
    public void onRadioButtonClicked(@NonNull SelectorWithWidgetPreference preference) {
        boolean isForceInvertEnabled = preference.getKey().equals(EXPANDED_DARK_THEME_KEY);
        if (mExpandedDarkThemePreference == null
                || isForceInvertEnabled == mExpandedDarkThemePreference.isChecked()) {
            // User selects the same preference as before, we perform an early return to avoid
            // unnecessary UI updates and IO operations.
            return;
        }
        updateSelectorPreferenceStatus(isForceInvertEnabled);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_FORCE_INVERT_COLOR_ENABLED,
                isForceInvertEnabled ? ON : OFF);
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        // initialize the status of the radio buttons
        PreferenceCategory preferenceCategory = screen.findPreference(getPreferenceKey());
        mStandardDarkThemePreference =
                preferenceCategory.findPreference(STANDARD_DARK_THEME_KEY);
        mExpandedDarkThemePreference =
                preferenceCategory.findPreference(EXPANDED_DARK_THEME_KEY);

        // We want to support search with different title on the preference. To prevent search
        // indexes on the title in xml, we set the title in PreferenceController.
        mStandardDarkThemePreference.setTitle(R.string.accessibility_standard_dark_theme_title);
        mExpandedDarkThemePreference.setTitle(R.string.accessibility_expanded_dark_theme_title);
        mStandardDarkThemePreference.setOnClickListener(this);
        mExpandedDarkThemePreference.setOnClickListener(this);
        boolean isForceInvertEnabled = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_FORCE_INVERT_COLOR_ENABLED, OFF) != OFF;
        updateSelectorPreferenceStatus(isForceInvertEnabled);
    }

    private void updateSelectorPreferenceStatus(boolean isForceInvertEnabled) {
        if (mStandardDarkThemePreference == null || mExpandedDarkThemePreference == null) {
            return;
        }
        mStandardDarkThemePreference.setChecked(!isForceInvertEnabled);
        mExpandedDarkThemePreference.setChecked(isForceInvertEnabled);
    }

    private boolean isNightMode() {
        return (mContext.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    @Override
    public void updateRawDataToIndex(@NonNull List<SearchIndexableRaw> rawData) {
        super.updateRawDataToIndex(rawData);
        SearchIndexableRaw standard = new SearchIndexableRaw(mContext);
        standard.key = STANDARD_DARK_THEME_KEY;
        standard.title = mContext.getString(
                R.string.accessibility_standard_dark_theme_title_in_search);
        rawData.add(standard);

        SearchIndexableRaw expanded = new SearchIndexableRaw(mContext);
        expanded.key = EXPANDED_DARK_THEME_KEY;
        expanded.title = mContext.getString(
                R.string.accessibility_expanded_dark_theme_title_in_search);
        expanded.keywords = mContext.getString(R.string.keywords_expanded_dark_theme);
        rawData.add(expanded);
    }

    @Override
    public void updateNonIndexableKeys(@NonNull List<String> keys) {
        super.updateNonIndexableKeys(keys);
        if (!Flags.forceInvertColor()) {
            keys.add(STANDARD_DARK_THEME_KEY);
            keys.add(EXPANDED_DARK_THEME_KEY);
        }
    }
}
// LINT.ThenChange(/src/com/android/settings/display/darkmode/DarkModeSelectorPreference.kt)
