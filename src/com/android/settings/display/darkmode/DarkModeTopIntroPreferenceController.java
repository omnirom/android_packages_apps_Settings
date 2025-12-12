/*
 * Copyright 2025 The Android Open Source Project
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

package com.android.settings.display.darkmode;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.TopIntroPreference;

/**
 * Controller of the top info preference in the Dark Mode settings page.
 *
 * This should be removed after the flag android.view.accessibility.force_invert_color is launched.
 */
// LINT.IfChange
public class DarkModeTopIntroPreferenceController extends BasePreferenceController {
    @Nullable private TopIntroPreference mPreference;

    public DarkModeTopIntroPreferenceController(
            @NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);

    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE_UNSEARCHABLE;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        if (android.view.accessibility.Flags.forceInvertColor()) {
            mPreference.setTitle(R.string.dark_ui_text_force_invert);
        } else {
            mPreference.setTitle(R.string.dark_ui_text);
        }
    }
}
// LINT.ThenChange(DarkModeTopIntroPreference.kt)
