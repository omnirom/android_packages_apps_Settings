/*
 * Copyright (C) 2023 The Android Open Source Project
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

import android.content.Context;
import android.util.FeatureFlagUtils;

import com.android.settings.R;
import com.android.settings.accessibility.FlashNotificationsUtil.State;
import com.android.settings.core.BasePreferenceController;

/**
 * Controller for flash notifications.
 */
public class FlashNotificationsPreferenceController extends BasePreferenceController {

    public FlashNotificationsPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        boolean isFeatureOn = FeatureFlagUtils.isEnabled(mContext,
                FeatureFlagUtils.SETTINGS_FLASH_NOTIFICATIONS);
        return isFeatureOn ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        final int res = switch (FlashNotificationsUtil.getFlashNotificationsState(mContext)) {
            case State.CAMERA, State.SCREEN, State.CAMERA_SCREEN ->
                    R.string.flash_notifications_summary_on;
            case State.OFF -> R.string.flash_notifications_summary_off;
            default -> R.string.flash_notifications_summary_off;
        };

        return mContext.getString(res);
    }
}
