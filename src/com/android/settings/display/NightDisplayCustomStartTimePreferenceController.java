/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.content.Context;
import android.hardware.display.ColorDisplayManager;
import androidx.preference.Preference;
import com.android.settings.core.BasePreferenceController;

import java.time.LocalTime;

public class NightDisplayCustomStartTimePreferenceController extends BasePreferenceController {

    private ColorDisplayManager mColorDisplayManager;
    private NightDisplayTimeFormatter mTimeFormatter;

    public NightDisplayCustomStartTimePreferenceController(Context context, String key) {
        super(context, key);

        mColorDisplayManager = context.getSystemService(ColorDisplayManager.class);
        mTimeFormatter = new NightDisplayTimeFormatter(context);
    }

    @Override
    public int getAvailabilityStatus() {
        return ColorDisplayManager.isNightDisplayAvailable(mContext) ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public final void updateState(Preference preference) {
        int mode = mColorDisplayManager.getNightDisplayAutoMode();
        preference
                .setVisible(mode == ColorDisplayManager.AUTO_MODE_CUSTOM_TIME ||
                        mode == ColorDisplayManager.AUTO_MODE_TWILIGHT);
        if (mode == ColorDisplayManager.AUTO_MODE_CUSTOM_TIME) {
            preference.setSummary(mTimeFormatter.getFormattedTimeString(
                    mColorDisplayManager.getNightDisplayCustomStartTime()));
            preference.setSelectable(true);
        } else if (mode == ColorDisplayManager.AUTO_MODE_TWILIGHT) {
            LocalTime t = mColorDisplayManager.getNightDisplayAutoStartTime();
            if (t != null) {
                preference.setSummary(mTimeFormatter.getFormattedTimeString(t));
            } else {
                preference.setSummary("");
            }
            preference.setSelectable(false);
        }
    }
}
