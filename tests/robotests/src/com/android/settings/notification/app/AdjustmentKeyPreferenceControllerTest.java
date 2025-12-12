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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Flags;
import android.content.Context;
import android.os.UserHandle;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.service.notification.Adjustment;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.PrimarySwitchPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class AdjustmentKeyPreferenceControllerTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private Context mContext;
    private NotificationBackend.AppRow mAppRow;
    @Mock
    private NotificationBackend mBackend;
    private PrimarySwitchPreference mSwitch;

    private AdjustmentKeyPreferenceController mPrefController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();
        mSwitch = new PrimarySwitchPreference(mContext);
        new PreferenceManager(mContext).createPreferenceScreen(mContext).addPreference(mSwitch);
        when(mBackend.hasSentValidMsg(anyString(), anyInt())).thenReturn(true);
        when(mBackend.getAllowedAssistantAdjustments()).thenReturn(List.of(KEY_TYPE));
        when(mBackend.isNotificationBundlingSupported()).thenReturn(true);
        when(mBackend.isNotificationSummarizationSupported()).thenReturn(true);

        mPrefController = new AdjustmentKeyPreferenceController(mContext, mBackend, KEY_TYPE);

        mAppRow = new NotificationBackend.AppRow();
        mAppRow.pkg = "pkg.name";
        mAppRow.uid = 12345;
        mAppRow.userId = UserHandle.getUserId(mAppRow.uid);
        mPrefController.onResume(mAppRow, null, null, null, null, null, null);
    }

    @Test
    @DisableFlags({Flags.FLAG_NM_SUMMARIZATION, Flags.FLAG_NM_SUMMARIZATION_UI,
            Flags.FLAG_NOTIFICATION_CLASSIFICATION_UI})
    public void testIsAvailable_flagOff() {
        assertThat(mPrefController.isAvailable()).isFalse();
    }

    @Test
    @EnableFlags({Flags.FLAG_NM_SUMMARIZATION, Flags.FLAG_NM_SUMMARIZATION_UI,
            Flags.FLAG_NOTIFICATION_CLASSIFICATION_UI})
    public void testIsAvailable_globalFeatureOff() {
        when(mBackend.getAllowedAssistantAdjustments()).thenReturn(new ArrayList<>());
        assertThat(mPrefController.isAvailable()).isFalse();
    }

    @Test
    @EnableFlags({Flags.FLAG_NM_SUMMARIZATION, Flags.FLAG_NM_SUMMARIZATION_UI,
            Flags.FLAG_NOTIFICATION_CLASSIFICATION_UI})
    public void testIsAvailable_flagOn() {
        assertThat(mPrefController.isAvailable()).isTrue();
    }

    @Test
    @EnableFlags({Flags.FLAG_NM_SUMMARIZATION, Flags.FLAG_NM_SUMMARIZATION_UI,
            Flags.FLAG_NOTIFICATION_CLASSIFICATION_UI})
    public void testIsAvailable_summarization_notMsgApp() {
        when(mBackend.hasSentValidMsg(anyString(), anyInt())).thenReturn(false);

        mPrefController = new AdjustmentKeyPreferenceController(
                mContext, mBackend, KEY_SUMMARIZATION);
        mPrefController.onResume(mAppRow, null, null, null, null, null, null);

        assertThat(mPrefController.isAvailable()).isFalse();
    }

    @Test
    @EnableFlags({Flags.FLAG_NOTIFICATION_CLASSIFICATION_UI})
    public void testIsAvailable_bundle_NasNotSupported() {
        when(mBackend.isNotificationBundlingSupported()).thenReturn(false);

        mPrefController = new AdjustmentKeyPreferenceController(mContext, mBackend, KEY_TYPE);
        mPrefController.onResume(mAppRow, null, null, null, null, null, null);

        assertThat(mPrefController.isAvailable()).isFalse();
    }

    @Test
    @EnableFlags({Flags.FLAG_NM_SUMMARIZATION, Flags.FLAG_NM_SUMMARIZATION_UI})
    public void testIsAvailable_summarization_NasNotSupported() {
        when(mBackend.isNotificationSummarizationSupported()).thenReturn(false);

        mPrefController = new AdjustmentKeyPreferenceController(
                mContext, mBackend, KEY_SUMMARIZATION);
        mPrefController.onResume(mAppRow, null, null, null, null, null, null);

        assertThat(mPrefController.isAvailable()).isFalse();
    }

    @Test
    @EnableFlags({Flags.FLAG_NM_SUMMARIZATION, Flags.FLAG_NM_SUMMARIZATION_UI,
            Flags.FLAG_NOTIFICATION_CLASSIFICATION_UI})
    public void testChecked_adjustmentAllowed() {
        when(mBackend.isAdjustmentSupportedForPackage(mAppRow.userId, KEY_TYPE,
                mAppRow.pkg)).thenReturn(true);
        mPrefController.onResume(mAppRow, null, null, null, null, null, null);

        mPrefController.updateState(mSwitch);
        assertThat(mSwitch.getCheckedState()).isTrue();
    }

    @Test
    @EnableFlags({Flags.FLAG_NM_SUMMARIZATION, Flags.FLAG_NM_SUMMARIZATION_UI,
            Flags.FLAG_NOTIFICATION_CLASSIFICATION_UI})
    public void testChecked_adjustmentNotAllowed() {
        when(mBackend.isAdjustmentSupportedForPackage(mAppRow.userId, KEY_TYPE,
                mAppRow.pkg)).thenReturn(false);
        mPrefController.onResume(mAppRow, null, null, null, null, null, null);

        mPrefController.updateState(mSwitch);
        assertThat(mSwitch.getCheckedState()).isFalse();
    }

    @Test
    @EnableFlags({Flags.FLAG_NM_SUMMARIZATION, Flags.FLAG_NM_SUMMARIZATION_UI,
            Flags.FLAG_NOTIFICATION_CLASSIFICATION_UI})
    public void testOnPreferenceChange_changeOnAndOff() {
        when(mBackend.isAdjustmentSupportedForPackage(mAppRow.userId, KEY_TYPE,
                mAppRow.pkg)).thenReturn(true);
        mPrefController.onResume(mAppRow, null, null, null, null, null, null);

        // when the switch value changes to false
        mPrefController.onPreferenceChange(mSwitch, false);

        verify(mBackend, times(1)).setAdjustmentSupportedForPackage(eq(mAppRow.userId),
                eq(KEY_TYPE), eq(mAppRow.pkg), eq(false));

        // same as above but now from false -> true
        mPrefController.onPreferenceChange(mSwitch, true);
        verify(mBackend, times(1)).setAdjustmentSupportedForPackage(eq(mAppRow.userId),
                eq(KEY_TYPE), eq(mAppRow.pkg), eq(true));
    }

    @Test
    @EnableFlags({Flags.FLAG_NM_SUMMARIZATION, Flags.FLAG_NM_SUMMARIZATION_UI,
            Flags.FLAG_NOTIFICATION_CLASSIFICATION_UI})
    public void testHandlePreferenceTreeClick_wrongPrefKey() {
        Preference pref = mock(Preference.class);
        when(pref.getKey()).thenReturn("some_key_that_is_not_relevant");
        assertThat(mPrefController.handlePreferenceTreeClick(pref)).isFalse();

        when(pref.getKey()).thenReturn(Adjustment.KEY_SUMMARIZATION);
        assertThat(mPrefController.handlePreferenceTreeClick(pref)).isFalse();

        // If the pref key actually matches, then this will attempt to launch an intent via
        // SubSettingLauncher, which may not work well from inside the test environment, so this
        // test only tests that we do nothing on the non-matching cases.
    }
}
