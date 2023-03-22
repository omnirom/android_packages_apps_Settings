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

package com.android.settings.development.transcode;

import android.content.Context;
import android.os.SystemProperties;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.development.DevelopmentSettingsEnabler;

/**
 * The controller for the "Enabling transcoding for all apps" switch on the transcode settings
 * screen.
 */
public class TranscodeGlobalTogglePreferenceController extends TogglePreferenceController {

    private static final String TRANSCODE_ENABLED_PROP_KEY = "persist.sys.fuse.transcode_enabled";

    public TranscodeGlobalTogglePreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        if (DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(mContext)) {
            return AVAILABLE;
        } else {
            return CONDITIONALLY_UNAVAILABLE;
        }
    }

    @Override
    public boolean isChecked() {
        return SystemProperties.getBoolean(TRANSCODE_ENABLED_PROP_KEY, true);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        SystemProperties.set(TRANSCODE_ENABLED_PROP_KEY, String.valueOf(isChecked));
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_system;
    }
}
