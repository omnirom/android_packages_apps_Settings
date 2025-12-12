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
package com.android.settings.display;

import static com.android.systemui.shared.Flags.ambientAod;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.display.AmbientDisplayConfiguration;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

public class AmbientDisplayAlwaysOnPreferenceScreenController extends 
        AmbientDisplayAlwaysOnPreferenceController {

    public AmbientDisplayAlwaysOnPreferenceScreenController(Context context, String key) {
        super(context, key);
    }

    /**
     * Assists in migrating the AOD setting to the Display subpage.
     */
    @Override
    protected boolean ambientAodMigration() {
        return ambientAod();
    }
}
