/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.notification.app;

import android.app.NotificationChannel;
import android.content.Context;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;

public class NotificationsOffPreferenceController extends NotificationPreferenceController
        implements PreferenceControllerMixin {

    private static final String KEY_BLOCKED_DESC = "block_desc";

    public NotificationsOffPreferenceController(Context context) {
        super(context, null);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_BLOCKED_DESC;
    }

    @Override
    public boolean isAvailable() {
        if (mAppRow == null) {
            return false;
        }
        if (mPreferenceFilter != null && !isIncludedInFilter()) {
            return false;
        }
        // Available only when other controllers are unavailable - this UI replaces the UI that
        // would give more detailed notification controls.
        return !super.isAvailable();
    }

    @Override
    boolean isIncludedInFilter() {
        return mPreferenceFilter.contains(NotificationChannel.EDIT_IMPORTANCE);
    }

    public void updateState(Preference preference) {
        if (mAppRow != null) {
            if (mChannel != null) {
                preference.setTitle(R.string.channel_notifications_off_desc);
            } else if (mChannelGroup != null) {
                preference.setTitle(R.string.channel_group_notifications_off_desc);
            } else if (mAppRow.permissionStateLocked) {
                preference.setTitle(R.string.app_notifications_not_send_desc);
            } else {
                preference.setTitle(R.string.app_notifications_off_desc);
            }
        }
        preference.setSelectable(false);
    }
}
