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

package com.android.settings.inputmethod;

import android.content.Context;
import android.hardware.input.InputSettings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceScreen;

import com.android.server.accessibility.Flags;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.ButtonPreference;

/**
 * Controller for resetting mouse keys settings button.
 */
public class MouseKeysResetController extends BasePreferenceController {

    @Nullable
    public ButtonPreference mPreference;

    public MouseKeysResetController(@NonNull Context context, @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return Flags.enableMouseKeyEnhancement() ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        if (mPreference != null) {
            mPreference.setOnClickListener(view -> {
                InputSettings.setAccessibilityMouseKeysMaxSpeed(
                        mContext, InputSettings.DEFAULT_MOUSE_KEYS_MAX_SPEED);
                InputSettings.setAccessibilityMouseKeysAcceleration(
                        mContext, InputSettings.DEFAULT_MOUSE_KEYS_ACCELERATION);
                InputSettings.setPrimaryKeysForMouseKeysEnabled(mContext, true);
            });
        }
    }
}
