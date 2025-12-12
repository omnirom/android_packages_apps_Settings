/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.biometrics.combination;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.app.admin.DevicePolicyManager;
import android.app.admin.EnforcingAdmin;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.face.FaceManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.UserManager;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.ActiveUnlockTestUtils;
import com.android.settings.testutils.shadow.ShadowDeviceConfig;
import com.android.settings.testutils.shadow.ShadowRestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreference;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowDeviceConfig.class, ShadowRestrictedLockUtilsInternal.class})
public class BiometricFingerprintStatusPreferenceControllerTest {

    @Rule public final MockitoRule mMocks = MockitoJUnit.rule();

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock private UserManager mUserManager;
    @Mock private PackageManager mPackageManager;
    @Mock private FingerprintManager mFingerprintManager;
    @Mock private FaceManager mFaceManager;

    private Context mContext;
    private RestrictedPreference mPreference;
    private BiometricFingerprintStatusPreferenceController mController;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)).thenReturn(true);
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_FACE)).thenReturn(true);
        shadowOf((Application) ApplicationProvider.getApplicationContext())
                .setSystemService(Context.FINGERPRINT_SERVICE, mFingerprintManager);
        shadowOf((Application) ApplicationProvider.getApplicationContext())
                .setSystemService(Context.FACE_SERVICE, mFaceManager);
        shadowOf((Application) ApplicationProvider.getApplicationContext())
                .setSystemService(Context.USER_SERVICE, mUserManager);
        when(mUserManager.getProfileIdsWithDisabled(anyInt())).thenReturn(new int[] {1234});
        mPreference = new RestrictedPreference(mContext);
        mController = new BiometricFingerprintStatusPreferenceController(mContext, "preferenceKey");
    }

    @After
    public void tearDown() {
        ActiveUnlockTestUtils.disable(mContext);
        ShadowRestrictedLockUtilsInternal.reset();
    }

    @Test
    public void onlyFingerprintEnabled_preferenceNotVisible() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(false);

        mController.updateState(mPreference);

        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void onlyFingerprintAndActiveUnlockEnabled_preferenceVisible() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(false);
        ActiveUnlockTestUtils.enable(mContext);

        mController.updateState(mPreference);

        assertThat(mPreference.isVisible()).isTrue();
    }

    @Test
    public void faceAndFingerprintEnabled_preferenceVisible() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFaceManager.isHardwareDetected()).thenReturn(true);

        mController.updateState(mPreference);

        assertThat(mPreference.isVisible()).isTrue();
    }


    @Test
    public void fingerprintDisabled_whenAdminAndNoFingerprintsEnrolled() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.hasEnrolledFingerprints(anyInt())).thenReturn(false);

        ShadowRestrictedLockUtilsInternal
                .setKeyguardDisabledFeatures(DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT);

        final RestrictedPreference restrictedPreference = mock(RestrictedPreference.class);
        mController.updateState(restrictedPreference);

        verify(restrictedPreference).setDisabledByAdmin((RestrictedLockUtils.EnforcedAdmin) any());
    }


    @Test
    @EnableFlags({android.app.admin.flags.Flags.FLAG_POLICY_TRANSPARENCY_REFACTOR_ENABLED,
            android.app.admin.flags.Flags.FLAG_SET_KEYGUARD_DISABLED_FEATURES_COEXISTENCE})
    public void fingerprintDisabled_whenDpmRefactorEnabled_whenAdminAndNoFingerprintsEnrolled() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.hasEnrolledFingerprints(anyInt())).thenReturn(false);

        ShadowRestrictedLockUtilsInternal
                .setKeyguardDisabledFeatures(DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT);

        final RestrictedPreference restrictedPreference = mock(RestrictedPreference.class);
        mController.updateState(restrictedPreference);

        verify(restrictedPreference).setDisabledByAdmin((EnforcingAdmin) any());
    }

    @Test
    public void fingerprintNotDisabled_whenAdminAndFingerprintsEnrolled() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.hasEnrolledFingerprints(anyInt())).thenReturn(true);

        ShadowRestrictedLockUtilsInternal
                .setKeyguardDisabledFeatures(DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT);

        final RestrictedPreference restrictedPreference = mock(RestrictedPreference.class);
        mController.updateState(restrictedPreference);

        verify(restrictedPreference, never()).setDisabledByAdmin(
                (RestrictedLockUtils.EnforcedAdmin) any());
        verify(restrictedPreference).setEnabled(true);
    }

    @Test
    @EnableFlags({android.app.admin.flags.Flags.FLAG_POLICY_TRANSPARENCY_REFACTOR_ENABLED,
            android.app.admin.flags.Flags.FLAG_SET_KEYGUARD_DISABLED_FEATURES_COEXISTENCE})
    public void fingerprintNotDisabled_whenDpmRefactorEnabled_whenAdminAndFingerprintsEnrolled() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.hasEnrolledFingerprints(anyInt())).thenReturn(true);

        ShadowRestrictedLockUtilsInternal
                .setKeyguardDisabledFeatures(DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT);

        final RestrictedPreference restrictedPreference = mock(RestrictedPreference.class);
        mController.updateState(restrictedPreference);

        verify(restrictedPreference, never()).setDisabledByAdmin((EnforcingAdmin) any());
        verify(restrictedPreference).setEnabled(true);
    }

    @Test
    public void fingerprintNotDisabled_whenNoAdmin() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);

        final RestrictedPreference restrictedPreference = mock(RestrictedPreference.class);
        mController.updateState(restrictedPreference);

        verify(restrictedPreference, never()).setDisabledByAdmin(
                (RestrictedLockUtils.EnforcedAdmin) any());
        verify(restrictedPreference).setEnabled(true);
    }
}
