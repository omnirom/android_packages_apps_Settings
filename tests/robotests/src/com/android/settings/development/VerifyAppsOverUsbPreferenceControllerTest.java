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
package com.android.settings.development;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.UserManager;
import android.provider.Settings;
import android.provider.Settings.Global;

import androidx.preference.PreferenceScreen;

import com.android.settingslib.RestrictedSwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class VerifyAppsOverUsbPreferenceControllerTest {

    @Mock
    private PackageManager mPackageManager;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private RestrictedSwitchPreference mPreference;

    private Context mContext;
    private VerifyAppsOverUsbPreferenceController mController;

    /** Convenience class for setting global int settings. */
    class GlobalSetter {
        public GlobalSetter set(String setting, int value) {
            Global.putInt(mContext.getContentResolver(), setting, value);
            return this;
        }
    }

    private final GlobalSetter mGlobals = new GlobalSetter();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        when(mScreen.findPreference(anyString())).thenReturn(mPreference);
        mController = new VerifyAppsOverUsbPreferenceController(mContext);
        ReflectionHelpers.setField(mController, "mPackageManager", mPackageManager);
        mController.displayPreference(mScreen);
    }

    private void setupVerifyBroadcastReceivers(boolean nonEmpty) {
        final List<ResolveInfo> resolveInfos = nonEmpty
                ? Collections.singletonList(mock(ResolveInfo.class))
                : Collections.emptyList();
        when(mPackageManager.queryBroadcastReceivers(any(), anyInt()))
                .thenReturn(resolveInfos);
    }

    @Test
    public void updateState_settingEnabled_preferenceShouldBeChecked() {
        setupVerifyBroadcastReceivers(true);
        mGlobals.set(Global.ADB_ENABLED, 1 /* setting enabled */)
                .set(Global.PACKAGE_VERIFIER_INCLUDE_ADB, 1 /* setting enabled */);
        mController.updateState(mPreference);
        verify(mPreference).setChecked(true);
    }

    @Test
    public void updateState_settingDisabled_preferenceShouldNotBeChecked() {
        setupVerifyBroadcastReceivers(true);
        mGlobals.set(Global.ADB_ENABLED, 1 /* setting enabled */)
                .set(Global.PACKAGE_VERIFIER_INCLUDE_ADB, 0 /* setting disabled */);
        mController.updateState(mPreference);
        verify(mPreference).setChecked(false);
    }

    @Test
    public void updateState_adbDisabled_preferenceShouldNotBeChecked() {
        setupVerifyBroadcastReceivers(true);
        mGlobals.set(Global.ADB_ENABLED, 0 /* setting disabled */)
                .set(Global.PACKAGE_VERIFIER_INCLUDE_ADB, 1 /* setting enabled */);
        mController.updateState(mPreference);
        verify(mPreference).setChecked(false);
    }

    @Test
    public void updateState_noBroadcastReceivers_preferenceShouldNotBeChecked() {
        setupVerifyBroadcastReceivers(false);
        mGlobals.set(Global.ADB_ENABLED, 1 /* setting enabled */)
                .set(Global.PACKAGE_VERIFIER_INCLUDE_ADB, 1 /* setting enabled */);
        mController.updateState(mPreference);
        verify(mPreference).setChecked(false);
    }

    @Test
    public void updateState_restrictedByAdmin_preferenceShouldBeDisabled() {
        setupVerifyBroadcastReceivers(true);
        mGlobals.set(Global.ADB_ENABLED, 1 /* setting enabled */)
                .set(Global.PACKAGE_VERIFIER_INCLUDE_ADB, 1 /* setting enabled */);
        mController.updateState(mPreference);
        verify(mPreference).checkRestrictionAndSetDisabled(eq(UserManager.ENSURE_VERIFY_APPS),
                anyInt());
    }

    @Test
    public void isAvailable_verifierNotVisible_shouldReturnFalse() {
        setupVerifyBroadcastReceivers(true);
        mGlobals.set(Global.PACKAGE_VERIFIER_SETTING_VISIBLE, 0 /* setting disabled */);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_verifierVisible_shouldReturnTrue() {
        setupVerifyBroadcastReceivers(true);
        mGlobals.set(Global.PACKAGE_VERIFIER_SETTING_VISIBLE, 1 /* setting enabled */);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void onPreferenceChange_settingEnabled_shouldEnableUsbVerify() {
        mController.onPreferenceChange(mPreference, true /* new value */);

        final int mode = Settings.Global.getInt(mContext.getContentResolver(),
                android.provider.Settings.Global.PACKAGE_VERIFIER_INCLUDE_ADB, -1 /* default */);

        assertThat(mode).isEqualTo(VerifyAppsOverUsbPreferenceController.SETTING_VALUE_ON);
    }

    @Test
    public void onPreferenceChange_settingDisabled_shouldDisableUsbVerify() {
        mController.onPreferenceChange(mPreference, false /* new value */);

        final int mode = Settings.Global.getInt(mContext.getContentResolver(),
                android.provider.Settings.Global.PACKAGE_VERIFIER_INCLUDE_ADB, -1 /* default */);

        assertThat(mode).isEqualTo(VerifyAppsOverUsbPreferenceController.SETTING_VALUE_OFF);
    }
}
