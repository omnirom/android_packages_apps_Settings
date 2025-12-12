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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import com.android.server.accessibility.Flags;
import com.android.settings.accessibility.AccessibilitySurveyNotificationJobService;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/** Tests for {@link SurveyNotificationReceiver}. */
@RunWith(RobolectricTestRunner.class)
public class SurveyNotificationReceiverTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    private AccessibilitySurveyNotificationJobService mJobService;
    @Mock
    private NotificationHelper mNotificationHelper;

    private final Context mContext = RuntimeEnvironment.getApplication();;
    private SurveyNotificationReceiver mReceiver;

    @Before
    public void setUp() {
        mReceiver = new SurveyNotificationReceiver(mJobService, mNotificationHelper);
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_LOW_VISION_HATS)
    public void onReceiveNotificationShown_disableHaTS_notScheduleJob() {
        final Intent intent = new Intent(ACTION_SCHEDULE_SURVEY_NOTIFICATION)
                .setPackage(mContext.getPackageName());

        mReceiver.onReceive(mContext, intent);

        verify(mJobService, never()).scheduleJob(any(Context.class), anyInt(), anyLong());
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LOW_VISION_HATS)
    public void onReceiveNotificationShown_withDarkUiSettingsPageId_scheduleJob() {
        int pageId = SettingsEnums.DARK_UI_SETTINGS;
        final Intent intent = new Intent(ACTION_SCHEDULE_SURVEY_NOTIFICATION)
                .setPackage(mContext.getPackageName())
                .putExtra(EXTRA_PAGE_ID, SettingsEnums.DARK_UI_SETTINGS);

        mReceiver.onReceive(mContext, intent);

        verify(mJobService).scheduleJob(any(Context.class), eq(pageId), anyLong());
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LOW_VISION_HATS)
    public void onReceiveNotificationShown_withoutPageId_notScheduleJob() {
        final Intent intent = new Intent(ACTION_SCHEDULE_SURVEY_NOTIFICATION)
                .setPackage(mContext.getPackageName());

        mReceiver.onReceive(mContext, intent);

        verify(mJobService, never()).scheduleJob(any(Context.class), anyInt(), anyLong());
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LOW_VISION_HATS)
    public void onReceiveNotificationShown_withUnknownPageId_notScheduleJob() {
        final Intent intent = new Intent(ACTION_SCHEDULE_SURVEY_NOTIFICATION)
                .setPackage(mContext.getPackageName())
                .putExtra(EXTRA_PAGE_ID, SettingsEnums.PAGE_UNKNOWN);

        mReceiver.onReceive(mContext, intent);

        verify(mJobService, never()).scheduleJob(any(Context.class), anyInt(), anyLong());
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LOW_VISION_HATS)
    public void onReceiveNotificationDismiss_withDarkUiSettingsPageId_cancelJobAndNotification() {
        int pageId = SettingsEnums.DARK_UI_SETTINGS;
        final Intent scheduleIntent = new Intent(ACTION_SCHEDULE_SURVEY_NOTIFICATION)
                .setPackage(mContext.getPackageName())
                .putExtra(EXTRA_PAGE_ID, pageId);
        mReceiver.onReceive(mContext, scheduleIntent);

        final Intent cancelIntent = new Intent(ACTION_CANCEL_SURVEY_NOTIFICATION)
                .setPackage(mContext.getPackageName())
                .putExtra(EXTRA_PAGE_ID, pageId);
        mReceiver.onReceive(mContext, cancelIntent);

        verify(mJobService).cancelJob(any(Context.class), eq(pageId));
        verify(mNotificationHelper).cancelNotification(eq(pageId));
    }
}
