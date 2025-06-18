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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.Flags;
import android.app.INotificationManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.applications.ApplicationsState;

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

@RunWith(RobolectricTestRunner.class)
@EnableFlags({Flags.FLAG_NM_SUMMARIZATION_UI, Flags.FLAG_NM_SUMMARIZATION,
        Flags.FLAG_NOTIFICATION_CLASSIFICATION_UI})
public class AdjustmentExcludedAppsPreferenceControllerTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock
    private NotificationBackend mBackend;
    @Mock
    private ApplicationsState mApplicationState;
    private AdjustmentExcludedAppsPreferenceController mController;
    private Context mContext;
    @Mock
    INotificationManager mInm;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;

        mController = new AdjustmentExcludedAppsPreferenceController(mContext, "key");
        mController.onAttach(null, mock(Fragment.class), mBackend, KEY_SUMMARIZATION);
        PreferenceScreen screen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mController.mPreferenceCategory = new PreferenceCategory(mContext);
        screen.addPreference(mController.mPreferenceCategory);

        mController.mApplicationsState = mApplicationState;
        mController.mPrefContext = mContext;
    }

    @Test
    public void testIsAvailable() {
        when(mBackend.isNotificationBundlingSupported()).thenReturn(true);
        when(mBackend.isNotificationSummarizationSupported()).thenReturn(true);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_flagEnabledNasDoesNotSupport_shouldReturnFalse() throws Exception {
        when(mInm.getUnsupportedAdjustmentTypes()).thenReturn(List.of(KEY_SUMMARIZATION));
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void testUpdateAppList() throws Exception {
        when(mBackend.getAdjustmentDeniedPackages(KEY_SUMMARIZATION)).thenReturn(
                new String[] {"cannot", "cannot2"});

        // GIVEN there are four apps, and two have KEY_SUMMARIZATION off
        ApplicationsState.AppEntry canSummarize =
                mock(ApplicationsState.AppEntry.class);
        canSummarize.info = new ApplicationInfo();
        canSummarize.info.packageName = "canSummarize";
        canSummarize.info.uid = 0;

        ApplicationsState.AppEntry canSummarize2 = mock(ApplicationsState.AppEntry.class);
        canSummarize2.info = new ApplicationInfo();
        canSummarize2.info.packageName = "canSummarizeTwo";
        canSummarize2.info.uid = 0;

        ApplicationsState.AppEntry cannot =
                mock(ApplicationsState.AppEntry.class);
        cannot.info = new ApplicationInfo();
        cannot.info.packageName = "cannot";
        cannot.info.uid = 0;

        ApplicationsState.AppEntry cannot2 =
                mock(ApplicationsState.AppEntry.class);
        cannot2.info = new ApplicationInfo();
        cannot2.info.packageName = "cannot2";
        cannot2.info.uid = 0;

        List<ApplicationsState.AppEntry> appEntries = new ArrayList<>();
        appEntries.add(canSummarize);
        appEntries.add(canSummarize2);
        appEntries.add(cannot);
        appEntries.add(cannot2);

        // WHEN the controller updates the app list with the app entries
        mController.updateAppList(appEntries);

        // THEN only the 'cannot' entries make it to the app list
        assertThat(mController.mPreferenceCategory.getPreferenceCount()).isEqualTo(2);
        assertThat((Preference) mController.mPreferenceCategory.findPreference(
                AdjustmentExcludedAppsPreferenceController.getKey(
                        cannot.info.packageName,cannot.info.uid))).isNotNull();
        assertThat((Preference) mController.mPreferenceCategory.findPreference(
                AdjustmentExcludedAppsPreferenceController.getKey(
                        cannot2.info.packageName,cannot2.info.uid))).isNotNull();
    }

    @Test
    public void testUpdateAppList_nullApps() {
        mController.updateAppList(null);
        assertThat(mController.mPreferenceCategory.getPreferenceCount()).isEqualTo(0);
    }
}
