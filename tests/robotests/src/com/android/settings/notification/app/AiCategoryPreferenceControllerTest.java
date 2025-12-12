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

package com.android.settings.notification.app;

import static android.service.notification.Adjustment.KEY_SUMMARIZATION;
import static android.service.notification.Adjustment.KEY_TYPE;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.Flags;
import android.app.NotificationChannel;
import android.content.Context;
import android.os.UserHandle;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import com.android.settings.notification.NotificationBackend;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@EnableFlags({Flags.FLAG_NOTIFICATION_CLASSIFICATION_UI, Flags.FLAG_NM_SUMMARIZATION})
@RunWith(RobolectricTestRunner.class)
public class AiCategoryPreferenceControllerTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private Context mContext;
    AiCategoryPreferenceController mController;
    @Mock
    NotificationBackend mBackend;
    private NotificationBackend.AppRow mAppRow;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new AiCategoryPreferenceController(mContext, mBackend);
        mAppRow = new NotificationBackend.AppRow();
        mAppRow.pkg = "pkg.name";
        mAppRow.uid = 12345;
        mAppRow.userId = UserHandle.getUserId(mAppRow.uid);
        when(mBackend.isNotificationBundlingSupported()).thenReturn(true);
        when(mBackend.isNotificationSummarizationSupported()).thenReturn(true);
        mController.onResume(mAppRow, null, null, null, null, null, null);
    }

    @Test
    public void isIncludedInFilter() {
        assertThat(mController.isIncludedInFilter()).isFalse();
    }

    @Test
    public void testIsAvailable_notIfAppBlocked() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.banned = true;
        mController.onResume(appRow, null, null, null, null, null, null);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void testIsAvailable_globalFeaturesOff() {
        when(mBackend.getAllowedAssistantAdjustments()).thenReturn(new ArrayList<>());
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void testIsAvailable_globalFeatureOneOne() {
        when(mBackend.getAllowedAssistantAdjustments()).thenReturn(List.of(KEY_TYPE));
        assertThat(mController.isAvailable()).isTrue();

        when(mBackend.hasSentValidMsg(anyString(), anyInt())).thenReturn(true);
        when(mBackend.getAllowedAssistantAdjustments()).thenReturn(List.of(KEY_SUMMARIZATION));
        assertThat(mController.isAvailable()).isTrue();

        when(mBackend.getAllowedAssistantAdjustments()).thenReturn(
                List.of(KEY_SUMMARIZATION, KEY_TYPE));
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void testIsAvailable_summarization_notMsgApp() {
        when(mBackend.getAllowedAssistantAdjustments()).thenReturn(List.of(KEY_SUMMARIZATION));
        when(mBackend.hasSentValidMsg(anyString(), anyInt())).thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }
}
