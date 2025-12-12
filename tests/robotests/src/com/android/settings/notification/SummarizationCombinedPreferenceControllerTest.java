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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Flags;
import android.content.Context;
import android.os.UserHandle;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreference;

import com.android.settingslib.widget.MainSwitchPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SummarizationCombinedPreferenceControllerTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final String PREFERENCE_KEY = "preference_key";

    private Context mContext;
    SummarizationCombinedPreferenceController mController;
    @Mock
    NotificationBackend mBackend;

    @Mock
    private PreferenceCategory mPrefCategory;

    private PreferenceCategory mExcludedAppsPrefCategory;
    private MainSwitchPreference mGlobalSwitch;
    private SwitchPreference mWorkSwitch;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mSetFlagsRule.enableFlags(Flags.FLAG_NM_SUMMARIZATION, Flags.FLAG_NM_SUMMARIZATION_UI);
        when(mBackend.isNotificationSummarizationSupported()).thenReturn(true);
        mController = new SummarizationCombinedPreferenceController(mContext, PREFERENCE_KEY,
                mBackend);

        // preference category/controller initiation
        mGlobalSwitch = new MainSwitchPreference(mContext);
        when(mPrefCategory.findPreference(
                BundleCombinedPreferenceController.GLOBAL_KEY)).thenReturn(mGlobalSwitch);
        mWorkSwitch = new SwitchPreference(mContext);
        when(mPrefCategory.findPreference(
                BundleCombinedPreferenceController.WORK_PREF_KEY)).thenReturn(mWorkSwitch);
        mExcludedAppsPrefCategory = new PreferenceCategory(mContext);
        when(mPrefCategory.findPreference(
                SummarizationCombinedPreferenceController.EXCLUDED_APPS_CATEGORY_KEY)).thenReturn(
                mExcludedAppsPrefCategory);

        mController.updateState(mPrefCategory);
    }

    @Test
    public void isAvailable_flagEnabledNasSupports_shouldReturnTrue() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_flagEnabledNasDoesNotSupport_shouldReturnFalse() {
        when(mBackend.isNotificationSummarizationSupported()).thenReturn(false);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_flagDisabledNasSupports_shouldReturnFalse() {
        mSetFlagsRule.disableFlags(Flags.FLAG_NM_SUMMARIZATION);
        mSetFlagsRule.disableFlags(Flags.FLAG_NM_SUMMARIZATION_UI);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void updateState_mainSwitch() {
        when(mBackend.isNotificationSummarizationEnabled(anyInt())).thenReturn(true);
        mController.updateState(mPrefCategory);
        assertThat(mGlobalSwitch.isChecked()).isTrue();

        when(mBackend.isNotificationSummarizationEnabled(anyInt())).thenReturn(false);
        mController.updateState(mPrefCategory);
        assertThat(mGlobalSwitch.isChecked()).isFalse();
    }

    @Test
    public void updateState_workSwitch_notVisibleWhenNoManagedProfile() {
        when(mBackend.isNotificationSummarizationEnabled(anyInt())).thenReturn(true);
        mController.setManagedProfile(null);
        mController.updateState(mPrefCategory);

        // The controller + main switch should be functional, but the work switch should not be
        // visible
        assertThat(mController.isAvailable()).isTrue();
        assertThat(mGlobalSwitch.isChecked()).isTrue();
        assertThat(mWorkSwitch.isVisible()).isFalse();
    }

    @Test
    public void updateState_hasManagedProfile_reflectsSettings() {
        // setup: has a work profile with summarization enabled
        mController.setManagedProfile(new UserHandle(12345));
        when(mBackend.isNotificationSummarizationEnabled(anyInt())).thenReturn(true);

        // initial state, since managed profile wasn't there yet when setUp() was called
        mController.updateState(mPrefCategory);
        assertThat(mWorkSwitch.isChecked()).isTrue();

        // main setting disabled: work profile switch should not be visible
        when(mBackend.isNotificationSummarizationEnabled(mContext.getUserId())).thenReturn(false);
        mController.updateState(mPrefCategory);
        assertThat(mWorkSwitch.isVisible()).isFalse();

        // main setting enabled again; work profile setting should be both visible & checked
        when(mBackend.isNotificationSummarizationEnabled(mContext.getUserId())).thenReturn(true);
        mController.updateState(mPrefCategory);
        assertThat(mWorkSwitch.isVisible()).isTrue();
        assertThat(mWorkSwitch.isChecked()).isTrue();

        // now disable work profile: should be visible & un-checked
        when(mBackend.isNotificationSummarizationEnabled(12345)).thenReturn(false);
        mController.updateState(mPrefCategory);
        assertThat(mWorkSwitch.isVisible()).isTrue();
        assertThat(mWorkSwitch.isChecked()).isFalse();
    }

    @Test
    public void toggleMainSwitch_updatesBackendAndWorkSwitch() {
        // set up work profile so we can check work switch state
        mController.setManagedProfile(new UserHandle(12345));
        when(mBackend.isNotificationSummarizationEnabled(12345)).thenReturn(true);
        mController.updateState(mPrefCategory);

        // simulate switching the switch off. we also have to set the backend to behave accordingly
        when(mBackend.isNotificationSummarizationEnabled(anyInt())).thenReturn(false);
        mGlobalSwitch.getOnPreferenceChangeListener().onPreferenceChange(mGlobalSwitch, false);

        // expect a backend call to turn off summarization for main user, and also for the work
        // switch to become invisible once the backend reflects the new state
        verify(mBackend, times(1)).setNotificationSummarizationEnabled(mContext.getUserId(), false);
        assertThat(mWorkSwitch.isVisible()).isFalse();
        assertThat(mExcludedAppsPrefCategory.isVisible()).isFalse();

        // now turn it back on
        when(mBackend.isNotificationSummarizationEnabled(anyInt())).thenReturn(true);
        mGlobalSwitch.getOnPreferenceChangeListener().onPreferenceChange(mGlobalSwitch, true);
        verify(mBackend, times(1)).setNotificationSummarizationEnabled(mContext.getUserId(), true);
        assertThat(mWorkSwitch.isVisible()).isTrue();
        assertThat(mExcludedAppsPrefCategory.isVisible()).isTrue();
    }

    @Test
    public void toggleWorkSwitch_updatesBackend() {
        // set up work profile
        mController.setManagedProfile(new UserHandle(12345));
        mController.updateState(mPrefCategory);

        // turn on
        mWorkSwitch.getOnPreferenceChangeListener().onPreferenceChange(mWorkSwitch, true);
        verify(mBackend).setNotificationSummarizationEnabled(12345, true);

        // turn off
        mWorkSwitch.getOnPreferenceChangeListener().onPreferenceChange(mWorkSwitch, false);
        verify(mBackend).setNotificationSummarizationEnabled(12345, false);

        // should have no effect on the main switch
        verify(mBackend, never()).setNotificationSummarizationEnabled(eq(mContext.getUserId()),
                anyBoolean());
    }
}
