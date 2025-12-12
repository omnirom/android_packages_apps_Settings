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
import android.os.UserHandle;
import android.os.UserManager;
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
    @Mock
    private UserManager mUm;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;

        mController = new AdjustmentExcludedAppsPreferenceController(mContext, "key");
        mController.setUserManager(mUm);
        mController.onAttach(null, mock(Fragment.class), mBackend, KEY_SUMMARIZATION);
        PreferenceScreen screen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mController.mPreferenceCategory = new PreferenceCategory(mContext);
        screen.addPreference(mController.mPreferenceCategory);

        mController.mApplicationsState = mApplicationState;
        mController.mPrefContext = mContext;

        when(mUm.getUserProfiles()).thenReturn(List.of(new UserHandle(0), new UserHandle(10)));
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
        when(mBackend.getAdjustmentDeniedPackages(/* userId= */ 0, KEY_SUMMARIZATION)).thenReturn(
                List.of("cannot", "cannot2"));
        when(mBackend.getAdjustmentDeniedPackages(/* userId= */ 10, KEY_SUMMARIZATION)).thenReturn(
                List.of("profileCannot"));

        // GIVEN there are four apps for user 0, and two for user 10, and
        //   - for user 0, two have KEY_SUMMARIZATION off
        //   - for user 10, one has KEY_SUMMARIZATION off, and the other has the same package name
        //     as one of the disallowed packages for the main user
        ApplicationsState.AppEntry canSummarize =
                mock(ApplicationsState.AppEntry.class);
        canSummarize.info = new ApplicationInfo();
        canSummarize.info.packageName = "canSummarize";
        canSummarize.info.uid = UserHandle.getUid(0, 1);

        ApplicationsState.AppEntry canSummarize2 = mock(ApplicationsState.AppEntry.class);
        canSummarize2.info = new ApplicationInfo();
        canSummarize2.info.packageName = "canSummarizeTwo";
        canSummarize2.info.uid = UserHandle.getUid(0, 2);

        ApplicationsState.AppEntry canOnlyOnProfile = mock(ApplicationsState.AppEntry.class);
        canOnlyOnProfile.info = new ApplicationInfo();
        canOnlyOnProfile.info.packageName = "cannot";  // same package, different user
        canOnlyOnProfile.info.uid = UserHandle.getUid(10, 3);

        ApplicationsState.AppEntry cannot = mock(ApplicationsState.AppEntry.class);
        cannot.info = new ApplicationInfo();
        cannot.info.packageName = "cannot";
        cannot.info.uid = UserHandle.getUid(0, 3);

        ApplicationsState.AppEntry cannot2 = mock(ApplicationsState.AppEntry.class);
        cannot2.info = new ApplicationInfo();
        cannot2.info.packageName = "cannot2";
        cannot2.info.uid = UserHandle.getUid(0, 4);

        ApplicationsState.AppEntry profileCannot = mock(ApplicationsState.AppEntry.class);
        profileCannot.info = new ApplicationInfo();
        profileCannot.info.packageName = "profileCannot";
        profileCannot.info.uid = UserHandle.getUid(10, 5);

        List<ApplicationsState.AppEntry> appEntries = new ArrayList<>();
        appEntries.add(canSummarize);
        appEntries.add(canSummarize2);
        appEntries.add(canOnlyOnProfile);
        appEntries.add(cannot);
        appEntries.add(cannot2);
        appEntries.add(profileCannot);

        // WHEN the controller updates the app list with the app entries
        mController.updateAppList(appEntries);

        // THEN only the 'cannot' entries make it to the app list
        assertThat(mController.mPreferenceCategory.getPreferenceCount()).isEqualTo(3);
        assertThat((Preference) mController.mPreferenceCategory.findPreference(
                AdjustmentExcludedAppsPreferenceController.getKey(
                        cannot.info.packageName, cannot.info.uid))).isNotNull();
        assertThat((Preference) mController.mPreferenceCategory.findPreference(
                AdjustmentExcludedAppsPreferenceController.getKey(
                        cannot2.info.packageName, cannot2.info.uid))).isNotNull();
        assertThat((Preference) mController.mPreferenceCategory.findPreference(
                AdjustmentExcludedAppsPreferenceController.getKey(
                        profileCannot.info.packageName, profileCannot.info.uid))).isNotNull();
    }

    @Test
    public void testUpdateAppList_nullApps() {
        mController.updateAppList(null);
        assertThat(mController.mPreferenceCategory.getPreferenceCount()).isEqualTo(0);
    }
}
