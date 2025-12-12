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

package com.android.settings.accessibility.notification;

import static com.android.internal.accessibility.common.NotificationConstants.ACTION_CANCEL_SURVEY_NOTIFICATION;
import static com.android.internal.accessibility.common.NotificationConstants.ACTION_SCHEDULE_SURVEY_NOTIFICATION;
import static com.android.internal.accessibility.common.NotificationConstants.EXTRA_PAGE_ID;

import android.app.settings.SettingsEnums;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.server.accessibility.Flags;
import com.android.settings.accessibility.AccessibilitySurveyNotificationJobService;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Service to display survey notifications.
 */
public class SurveyNotificationReceiver extends BroadcastReceiver {

    private static final long SCHEDULE_TIME = TimeUnit.DAYS.toMillis(7);

    @NonNull
    private final AccessibilitySurveyNotificationJobService mJobService;

    @Nullable
    private final NotificationHelper mNotificationHelper;

    /**
     * Default constructor for {@link SurveyNotificationReceiver}.
     */
    public SurveyNotificationReceiver() {
        this(new AccessibilitySurveyNotificationJobService(), /* notificationHelper= */ null);
    }

    /**
     * Constructor for {@link SurveyNotificationReceiver} for testing purposes.
     *
     * @param jobService The {@link AccessibilitySurveyNotificationJobService} instance to use.
     * @param notificationHelper The {@link NotificationHelper} instance to use, or {@code null} if
     * it should be initialized lazily.
     */
    @VisibleForTesting
    public SurveyNotificationReceiver(
            @NonNull AccessibilitySurveyNotificationJobService jobService,
            @Nullable NotificationHelper notificationHelper) {
        mJobService = jobService;
        mNotificationHelper = notificationHelper;
    }

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        if (!Flags.enableLowVisionHats()) {
            return;
        }

        int pageId = intent.getIntExtra(EXTRA_PAGE_ID, SettingsEnums.PAGE_UNKNOWN);
        if (pageId == SettingsEnums.PAGE_UNKNOWN) {
            return;
        }

        if (ACTION_SCHEDULE_SURVEY_NOTIFICATION.equals(intent.getAction())) {
            mJobService.scheduleJob(context, pageId, SCHEDULE_TIME);
        } else if (ACTION_CANCEL_SURVEY_NOTIFICATION.equals(intent.getAction())) {
            mJobService.cancelJob(context, pageId);
            Objects.requireNonNullElseGet(mNotificationHelper,
                    () -> new NotificationHelper(context)).cancelNotification(pageId);
        }
    }
}
