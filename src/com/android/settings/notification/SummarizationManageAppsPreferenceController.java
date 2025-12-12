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

package com.android.settings.notification;

import static android.service.notification.Adjustment.KEY_SUMMARIZATION;

import android.app.Flags;
import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class SummarizationManageAppsPreferenceController extends
        BasePreferenceController {

    NotificationBackend mBackend;
    private Context mContext;
    private UserManager mUserManager;

    public SummarizationManageAppsPreferenceController(@NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);
        mContext = context;
        mBackend = new NotificationBackend();
        mUserManager = context.getSystemService(UserManager.class);
    }

    @Override
    public int getAvailabilityStatus() {
        if ((Flags.nmSummarization() || Flags.nmSummarizationUi())
                && mBackend.isNotificationSummarizationSupported()) {
            return AVAILABLE;
        }
        return CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        // If there are no apps that are currently excluded from summarization, add help text;
        // otherwise, no summary text needed
        super.updateState(preference);

        boolean hasDeniedPackages = false;
        for (UserHandle userHandle : mUserManager.getUserProfiles()) {
            int userId = userHandle.getIdentifier();
            if (!mBackend.getAdjustmentDeniedPackages(userId, KEY_SUMMARIZATION).isEmpty()) {
                hasDeniedPackages = true;
                break;
            }
        }

        if (!hasDeniedPackages) {
            preference.setSummary(
                    mContext.getString(R.string.notification_summarization_no_apps_desc));
        } else {
            preference.setSummary("");
        }
    }
}
