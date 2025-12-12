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

package com.android.settings.inputmethod;

import android.annotation.Nullable;
import android.content.Context;
import android.hardware.input.InputSettings;
import android.net.Uri;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

/**
 * Controller class that controls primary keys settings for Mouse Keys.
 *
 * When primary key setting is on, the user can use primary keyboard keys instead of numpad keys
 * to control the mouse key.
 */
public class MouseKeysPrimaryKeysController extends
        InputSettingPreferenceController {

    @Nullable
    private TwoStatePreference mTwoStatePreference;

    public MouseKeysPrimaryKeysController(@NonNull Context context,
            @NonNull String key) {
        super(context, key);
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mTwoStatePreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public boolean isChecked() {
        return InputSettings.isPrimaryKeysForMouseKeysEnabled(mContext);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        InputSettings.setPrimaryKeysForMouseKeysEnabled(mContext, isChecked);
        return true;
    }

    @Override
    protected void onInputSettingUpdated() {
        if (mTwoStatePreference != null) {
            mTwoStatePreference.setChecked(
                    InputSettings.isPrimaryKeysForMouseKeysEnabled(mContext));
        }
    }

    @Override
    protected Uri getSettingUri() {
        return Settings.Secure.getUriFor(
                Settings.Secure.ACCESSIBILITY_MOUSE_KEYS_USE_PRIMARY_KEYS);
    }

    @Override
    public int getAvailabilityStatus() {
        return com.android.server.accessibility.Flags.enableMouseKeyEnhancement()
                ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }
}
