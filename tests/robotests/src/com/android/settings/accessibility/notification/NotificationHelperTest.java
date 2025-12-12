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

import static com.android.internal.accessibility.common.NotificationConstants.EXTRA_SOURCE;
import static com.android.internal.accessibility.common.NotificationConstants.SOURCE_START_SURVEY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.robolectric.Shadows.shadowOf;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowNotification;
import org.robolectric.shadows.ShadowNotificationManager;
import org.robolectric.shadows.ShadowPendingIntent;

/** Tests for {@link NotificationHelper}. */
@RunWith(RobolectricTestRunner.class)
public class NotificationHelperTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private final Context mContext = spy(RuntimeEnvironment.getApplication());
    private NotificationHelper mNotificationHelper;
    private ShadowNotificationManager mShadowNotificationManager;

    @Before
    public void setUp() {
        mNotificationHelper = new NotificationHelper(mContext);
        NotificationManager notificationManager =
                mContext.getSystemService(NotificationManager.class);
        mShadowNotificationManager = shadowOf(notificationManager);
    }

    @Test
    public void constructor_createNotificationChannel() {
        assertThat(mShadowNotificationManager.getNotificationChannels()).isNotEmpty();
        assertThat(mShadowNotificationManager.getNotificationChannels().getFirst().getId())
                .isEqualTo(NotificationHelper.NOTIFICATION_CHANNEL_ID);
    }

    @Test
    public void showSurveyNotification_withDarkUiSettingsPageId_createAndPostNotification() {
        final int pageId = SettingsEnums.DARK_UI_SETTINGS;
        final String expectedAction = Settings.ACTION_DARK_THEME_SETTINGS;
        final String expectedTitle =
                mContext.getString(R.string.dark_theme_survey_notification_title);
        final String expectedActionTitle =
                mContext.getString(R.string.dark_theme_survey_notification_action);

        mNotificationHelper.showSurveyNotification(pageId);

        Notification notification = getNotificationAndVerifyExistence(pageId);
        verifyNotificationInfo(notification, expectedTitle, expectedActionTitle);
        verifyNotificationIntents(notification, expectedAction);
    }

    @Test
    public void cancelNotification_withDarkUiSettingsPageId_cancelExpectedNotification() {
        mNotificationHelper.showSurveyNotification(SettingsEnums.DARK_UI_SETTINGS);
        int notificationId = mNotificationHelper.getNotificationId(SettingsEnums.DARK_UI_SETTINGS);

        mNotificationHelper.cancelNotification(SettingsEnums.DARK_UI_SETTINGS);

        assertThat(mShadowNotificationManager.getNotification(notificationId)).isNull();
    }


    @Test
    public void showSurveyNotification_withMagnificationSettingsPageId_createAndPostNotification() {
        final int pageId = SettingsEnums.ACCESSIBILITY_TOGGLE_SCREEN_MAGNIFICATION;
        final String expectedAction = Settings.ACTION_SCREEN_MAGNIFICATION_SETTINGS;
        final String expectedTitle =
                mContext.getString(R.string.magnification_survey_notification_title);
        final String expectedActionTitle =
                mContext.getString(R.string.magnification_survey_notification_action);

        mNotificationHelper.showSurveyNotification(pageId);

        Notification notification = getNotificationAndVerifyExistence(pageId);
        verifyNotificationInfo(notification, expectedTitle, expectedActionTitle);
        verifyNotificationIntents(notification, expectedAction);
    }

    @Test
    public void getNotificationId_returnCorrectId() {
        int pageId = 123;
        // NOTIFICATION_ID_BASE from NotificationHelper is 751131
        int expectedNotificationId = 751131 + pageId;

        assertThat(mNotificationHelper.getNotificationId(pageId)).isEqualTo(expectedNotificationId);
    }

    private Notification getNotificationAndVerifyExistence(int pageId) {
        int notificationId = mNotificationHelper.getNotificationId(pageId);
        Notification notification = mShadowNotificationManager.getNotification(notificationId);
        assertThat(notification).isNotNull();
        return notification;
    }

    private void verifyNotificationInfo(Notification notification, String expectedTitle,
            String expectedActionTitle) {
        ShadowNotification shadowNotification = shadowOf(notification);
        assertThat(shadowNotification.getContentTitle().toString()).isEqualTo(expectedTitle);
        assertThat(notification.actions.length).isEqualTo(1);
        assertThat(notification.actions[0].title.toString()).isEqualTo(expectedActionTitle);
    }

    private void verifyNotificationIntents(Notification notification, String expectedAction) {
        verifyPendingIntent(notification.contentIntent, expectedAction);
        verifyPendingIntent(notification.actions[0].actionIntent, expectedAction);
    }

    private void verifyPendingIntent(PendingIntent pendingIntent, String expectedAction) {
        ShadowPendingIntent shadowPendingIntent = shadowOf(pendingIntent);
        Intent intent = shadowPendingIntent.getSavedIntent();
        assertThat(intent.getAction()).isEqualTo(expectedAction);
        assertThat(intent.getPackage()).isEqualTo(mContext.getPackageName());
        assertThat(intent.getStringExtra(EXTRA_SOURCE)).isEqualTo(SOURCE_START_SURVEY);
        assertThat(intent.getFlags()).isEqualTo(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    }
}

