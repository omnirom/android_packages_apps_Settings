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

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.view.Display;

import com.android.server.display.feature.flags.Flags;

public class HdrBrightnessUtils {
    static int getAvailabilityStatus(Context context) {
        if (!Flags.hdrBrightnessSetting()) {
            return UNSUPPORTED_ON_DEVICE;
        }
        DisplayManager dm = context.getSystemService(DisplayManager.class);
        for (Display display : dm.getDisplays(
                DisplayManager.DISPLAY_CATEGORY_ALL_INCLUDING_DISABLED)) {
            if (display.isHdr() && display.isHdrSdrRatioAvailable()) {
                return AVAILABLE;
            }
        }
        return UNSUPPORTED_ON_DEVICE;
    }
}
