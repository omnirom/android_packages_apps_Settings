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

package com.android.settings.safetycenter;

import static android.safetycenter.SafetyEvent.SAFETY_EVENT_TYPE_SOURCE_STATE_CHANGED;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.hardware.fingerprint.FingerprintManager;
import android.os.UserHandle;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.safetycenter.SafetyEvent;
import android.safetycenter.SafetySourceData;
import android.safetycenter.SafetySourceStatus;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.biometrics.activeunlock.ActiveUnlockStatusUtils;
import com.android.settings.flags.Flags;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.ResourcesUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class WearSafetySourceTest {

    private static final ComponentName COMPONENT_NAME = new ComponentName("package", "class");
    private static final UserHandle USER_HANDLE = new UserHandle(UserHandle.myUserId());
    private static final SafetyEvent EVENT_SOURCE_STATE_CHANGED =
            new SafetyEvent.Builder(SAFETY_EVENT_TYPE_SOURCE_STATE_CHANGED).build();
    public static final String TARGET = "com.active.unlock.target";
    public static final String PROVIDER = "com.active.unlock.provider";
    public static final String TARGET_SETTING = "active_unlock_target";
    public static final String PROVIDER_SETTING = "active_unlock_provider";
    public static final String SUMMARY = "Wear Summary";

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    private Context mApplicationContext;

    @Mock private PackageManager mPackageManager;
    @Mock private DevicePolicyManager mDevicePolicyManager;
    @Mock private FingerprintManager mFingerprintManager;
    @Mock private LockPatternUtils mLockPatternUtils;
    @Mock private SafetyCenterManagerWrapper mSafetyCenterManagerWrapper;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mApplicationContext = spy(ApplicationProvider.getApplicationContext());
        when(mApplicationContext.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)).thenReturn(true);
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_FACE)).thenReturn(true);
        when(mDevicePolicyManager.getProfileOwnerOrDeviceOwnerSupervisionComponent(USER_HANDLE))
                .thenReturn(COMPONENT_NAME);
        when(mApplicationContext.getSystemService(Context.FINGERPRINT_SERVICE))
                .thenReturn(mFingerprintManager);
        when(mApplicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE))
                .thenReturn(mDevicePolicyManager);
        FakeFeatureFactory featureFactory = FakeFeatureFactory.setupForTest();
        when(featureFactory.securityFeatureProvider.getLockPatternUtils(mApplicationContext))
                .thenReturn(mLockPatternUtils);
        doReturn(true).when(mLockPatternUtils).isSecure(anyInt());
        SafetyCenterManagerWrapper.sInstance = mSafetyCenterManagerWrapper;
    }

    @After
    public void tearDown() {
        SafetyCenterManagerWrapper.sInstance = null;
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_BIOMETRICS_ONBOARDING_EDUCATION)
    public void setSafetyData_whenSafetyCenterIsDisabled_doesNotSetData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(false);

        WearSafetySource.setSafetySourceData(
                mApplicationContext, EVENT_SOURCE_STATE_CHANGED);

        verify(mSafetyCenterManagerWrapper, never())
                .setSafetySourceData(any(), any(), any(), any());
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_BIOMETRICS_ONBOARDING_EDUCATION)
    public void setSafetySourceData_whenSeparateBiometricsFlagOff_setsNullData() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);

        WearSafetySource.setSafetySourceData(
                mApplicationContext, EVENT_SOURCE_STATE_CHANGED);

        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(), eq(WearSafetySource.SAFETY_SOURCE_ID), eq(null), any());
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_BIOMETRICS_ONBOARDING_EDUCATION)
    public void setSafetySourceData_whenSafetyCenterIsEnabled_activeUnlockDisabled_setsNullData() {
        disableActiveUnlock(mApplicationContext);
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);

        WearSafetySource.setSafetySourceData(
                mApplicationContext, EVENT_SOURCE_STATE_CHANGED);

        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(), eq(WearSafetySource.SAFETY_SOURCE_ID), eq(null), any());
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_BIOMETRICS_ONBOARDING_EDUCATION)
    public void setSafetySourceData_setsDataWithCorrectSafetyEvent() {
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);

        WearSafetySource.setSafetySourceData(
                mApplicationContext, EVENT_SOURCE_STATE_CHANGED);

        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(any(), any(), any(), eq(EVENT_SOURCE_STATE_CHANGED));
    }


    @Test
    @RequiresFlagsEnabled(Flags.FLAG_BIOMETRICS_ONBOARDING_EDUCATION)
    public void setSafetySourceData_withWearEnabled_whenWearEnrolled_setsData() {
        enableActiveUnlock(mApplicationContext);
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.hasEnrolledFingerprints(anyInt())).thenReturn(false);

        WearSafetySource.setHasEnrolledForTesting(true);
        WearSafetySource.setSummaryForTesting(SUMMARY);

        WearSafetySource.setSafetySourceData(
                mApplicationContext, EVENT_SOURCE_STATE_CHANGED);

        assertSafetySourceEnabledDataSet(
                ResourcesUtils.getResourcesString(mApplicationContext,
                "security_settings_activeunlock"),
                SUMMARY);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_BIOMETRICS_ONBOARDING_EDUCATION)
    public void setSafetySourceData_withWearEnabled_whenWearNotEnrolled_setsData() {
        enableActiveUnlock(mApplicationContext);
        when(mSafetyCenterManagerWrapper.isEnabled(mApplicationContext)).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.hasEnrolledFingerprints(anyInt())).thenReturn(false);
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(COMPONENT_NAME)).thenReturn(0);

        WearSafetySource.setHasEnrolledForTesting(false);
        WearSafetySource.setSummaryForTesting(SUMMARY);

        WearSafetySource.setSafetySourceData(
                mApplicationContext, EVENT_SOURCE_STATE_CHANGED);

        assertSafetySourceDisabledDataSet(
                ResourcesUtils.getResourcesString(mApplicationContext,
                "security_settings_activeunlock"),
                SUMMARY);
    }

    private static void disableActiveUnlock(Context context) {
        DeviceConfig.setProperty(
                DeviceConfig.NAMESPACE_REMOTE_AUTH,
                ActiveUnlockStatusUtils.CONFIG_FLAG_NAME,
                /* value= */ null,
                /* makeDefault=*/ false);
        Settings.Secure.putString(context.getContentResolver(), TARGET_SETTING, null);
        Settings.Secure.putString(context.getContentResolver(), PROVIDER_SETTING, null);
    }

    private static void enableActiveUnlock(Context context) {
        Settings.Secure.putString(
                context.getContentResolver(), TARGET_SETTING, TARGET);
        Settings.Secure.putString(
                context.getContentResolver(), PROVIDER_SETTING, PROVIDER);

        PackageManager packageManager = context.getPackageManager();
        ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.flags = ApplicationInfo.FLAG_SYSTEM;

        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = new ActivityInfo();
        resolveInfo.activityInfo.applicationInfo = applicationInfo;
        when(packageManager.resolveActivity(any(), anyInt())).thenReturn(resolveInfo);

        ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.authority = PROVIDER;
        providerInfo.applicationInfo = applicationInfo;
        when(packageManager.resolveContentProvider(anyString(), any())).thenReturn(providerInfo);

        DeviceConfig.setProperty(
                DeviceConfig.NAMESPACE_REMOTE_AUTH,
                ActiveUnlockStatusUtils.CONFIG_FLAG_NAME,
                "unlock_intent_layout",
                false /* makeDefault */);
    }

    private void assertSafetySourceDisabledDataSet(String expectedTitle, String expectedSummary) {
        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(),
                        eq(WearSafetySource.SAFETY_SOURCE_ID),
                        captor.capture(),
                        any());
        SafetySourceData safetySourceData = captor.getValue();
        SafetySourceStatus safetySourceStatus = safetySourceData.getStatus();

        assertThat(safetySourceStatus.getTitle().toString()).isEqualTo(expectedTitle);
        assertThat(safetySourceStatus.getSummary().toString()).isEqualTo(expectedSummary);
        assertThat(safetySourceStatus.isEnabled()).isTrue();
        assertThat(safetySourceStatus.getSeverityLevel())
                .isEqualTo(SafetySourceData.SEVERITY_LEVEL_UNSPECIFIED);

        Intent clickIntent = safetySourceStatus.getPendingIntent().getIntent();
        assertThat(clickIntent).isNotNull();
        assertThat(clickIntent.getAction()).isEqualTo(TARGET);
    }

    private void assertSafetySourceEnabledDataSet(
            String expectedTitle, String expectedSummary) {
        ArgumentCaptor<SafetySourceData> captor = ArgumentCaptor.forClass(SafetySourceData.class);
        verify(mSafetyCenterManagerWrapper)
                .setSafetySourceData(
                        any(),
                        eq(WearSafetySource.SAFETY_SOURCE_ID),
                        captor.capture(),
                        any());
        SafetySourceData safetySourceData = captor.getValue();
        SafetySourceStatus safetySourceStatus = safetySourceData.getStatus();

        assertThat(safetySourceStatus.getTitle().toString()).isEqualTo(expectedTitle);
        assertThat(safetySourceStatus.getSummary().toString()).isEqualTo(expectedSummary);
        assertThat(safetySourceStatus.isEnabled()).isTrue();
        assertThat(safetySourceStatus.getSeverityLevel())
                .isEqualTo(SafetySourceData.SEVERITY_LEVEL_INFORMATION);
        Intent clickIntent = safetySourceStatus.getPendingIntent().getIntent();
        assertThat(clickIntent).isNotNull();
    }
}
