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

import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

/** Controls the "widgets on lock screen" preferences (under "Display & touch"). */
public class WidgetsOnLockscreenPreferenceController extends BasePreferenceController {
    public WidgetsOnLockscreenPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return isAvailable(mContext) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    /**
     * Returns whether "widgets on lock screen" preferences are available.
     */
    public static boolean isAvailable(Context context) {
        if (!isMainUser(context)) {
            return false;
        }

        return com.android.systemui.Flags.glanceableHubV2()
                && (context.getResources().getBoolean(R.bool.config_show_communal_settings)
                    || context.getResources().getBoolean(
                            R.bool.config_show_communal_settings_mobile));
    }

    private static boolean isMainUser(Context context) {
        final UserManager userManager = context.getSystemService(UserManager.class);
        return userManager.getUserInfo(UserHandle.myUserId()).isMain();
    }
}
