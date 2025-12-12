/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.biometrics;

import static android.hardware.biometrics.BiometricAuthenticator.TYPE_FACE;
import static android.hardware.biometrics.BiometricAuthenticator.TYPE_FINGERPRINT;
import static android.hardware.biometrics.BiometricAuthenticator.TYPE_IRIS;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.app.admin.EnforcingAdmin;
import android.app.supervision.SupervisionManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;

import com.android.settingslib.RestrictedLockUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.Collection;

import platform.test.runner.parameterized.Parameter;
import platform.test.runner.parameterized.ParameterizedAndroidJunit4;
import platform.test.runner.parameterized.Parameters;

@RunWith(ParameterizedAndroidJunit4.class)
public class ParentalControlsUtilsTest {
    @Rule
    public final CheckFlagsRule checkFlags = DeviceFlagsValueProvider.createCheckFlagsRule();
    @Rule
    public final MockitoRule mocks = MockitoJUnit.rule();

    @Parameter(0)
    public int modality;
    @Parameter(1)
    public int keyguardDisabledFeature;

    /**
     * @return a collection of test parameters
     */
    @Parameters(name = "modality={0}, keyguardDisabledFeature={1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[][]{{TYPE_FINGERPRINT, DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT},
                        {TYPE_FACE, DevicePolicyManager.KEYGUARD_DISABLE_FACE},
                        {TYPE_IRIS, DevicePolicyManager.KEYGUARD_DISABLE_IRIS}});
    }

    private Context mContext;
    @Mock
    private DevicePolicyManager mDpm;
    @Mock
    private SupervisionManager mSm;

    private final ComponentName mSupervisionComponent = new ComponentName("pkg", "cls");

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(DevicePolicyManager.class)).thenReturn(mDpm);
        when(mContext.getSystemService(SupervisionManager.class)).thenReturn(mSm);
    }

    /**
     * Helper that sets the appropriate mocks and testing behavior for legacy behaviour of
     * supervision with PO/DO admin.
     */
    private void setUpMockSupervisionLegacy(@Nullable ComponentName supervisionComponentName,
            int keyguardDisabledFlags) {
        when(mDpm.getProfileOwnerOrDeviceOwnerSupervisionComponent(
                any(UserHandle.class))).thenReturn(supervisionComponentName);
        when(mDpm.getKeyguardDisabledFeatures(eq(supervisionComponentName))).thenReturn(
                keyguardDisabledFlags);
    }

    /**
     * Helper that sets the appropriate mocks and testing behavior with SupervisionManager.
     */
    private void setUpMockSupervision(boolean supervisionEnabled, int keyguardDisabledFlags) {
        when(mDpm.getKeyguardDisabledFeatures(eq(null))).thenReturn(keyguardDisabledFlags);
        when(mSm.isSupervisionEnabledForUser(anyInt())).thenReturn(supervisionEnabled);
        when(mSm.getActiveSupervisionAppPackage()).thenReturn(
                supervisionEnabled ? mSupervisionComponent.getPackageName() : null);
    }

    @Test
    @RequiresFlagsDisabled(android.app.supervision.flags.Flags.FLAG_DEPRECATE_DPM_SUPERVISION_APIS)
    public void testEnforcedAdmin_whenDpmDisablesBiometricsAndSupervisionComponentExists() {
        setUpMockSupervisionLegacy(mSupervisionComponent, keyguardDisabledFeature);

        RestrictedLockUtils.EnforcedAdmin admin =
                ParentalControlsUtils.parentConsentRequiredInternal(mContext, modality,
                        new UserHandle(UserHandle.myUserId()));

        assertNotNull(admin);
        assertEquals(UserManager.DISALLOW_BIOMETRIC, admin.enforcedRestriction);
        assertEquals(mSupervisionComponent, admin.component);
    }

    @Test
    @RequiresFlagsEnabled(android.app.supervision.flags.Flags.FLAG_DEPRECATE_DPM_SUPERVISION_APIS)
    public void testEnforcedAdmin_whenDpmDisablesBiometricsAndSupervisionIsEnabled() {
        setUpMockSupervision(/* supervisionEnabled= */ true, keyguardDisabledFeature);

        RestrictedLockUtils.EnforcedAdmin admin =
                ParentalControlsUtils.parentConsentRequiredInternal(mContext, modality,
                        new UserHandle(UserHandle.myUserId()));

        assertNotNull(admin);
        assertEquals(UserManager.DISALLOW_BIOMETRIC, admin.enforcedRestriction);
        assertNull(admin.component);
    }

    @Test
    @RequiresFlagsDisabled(android.app.supervision.flags.Flags.FLAG_DEPRECATE_DPM_SUPERVISION_APIS)
    public void testNoEnforcedAdmin_whenNoSupervisionComponent() {
        // Even if DPM flag exists, returns null EnforcedAdmin when no supervision component exists
        setUpMockSupervisionLegacy(/*supervisionComponentName=*/null, keyguardDisabledFeature);

        RestrictedLockUtils.EnforcedAdmin admin =
                ParentalControlsUtils.parentConsentRequiredInternal(mContext, modality,
                        new UserHandle(UserHandle.myUserId()));
        assertNull(admin);
    }

    @Test
    @RequiresFlagsEnabled(android.app.supervision.flags.Flags.FLAG_DEPRECATE_DPM_SUPERVISION_APIS)
    public void testNoEnforcedAdmin_whenSupervisionIsDisabled() {
        setUpMockSupervision(/* supervisionEnabled= */ false, keyguardDisabledFeature);

        RestrictedLockUtils.EnforcedAdmin admin =
                ParentalControlsUtils.parentConsentRequiredInternal(mContext, modality,
                        new UserHandle(UserHandle.myUserId()));

        assertNull(admin);
    }

    @Test
    @RequiresFlagsDisabled(android.app.supervision.flags.Flags.FLAG_DEPRECATE_DPM_SUPERVISION_APIS)
    public void testEnforcingAdmin_whenDpmDisablesBiometricsAndSupervisionComponentExists() {
        setUpMockSupervisionLegacy(mSupervisionComponent, keyguardDisabledFeature);

        EnforcingAdmin admin = ParentalControlsUtils.getParentalSupervisionAdminInternal(mContext,
                modality, new UserHandle(UserHandle.myUserId()));
        assertNotNull(admin);
        assertEquals(mSupervisionComponent, admin.getComponentName());
    }

    @Test
    @RequiresFlagsEnabled(android.app.supervision.flags.Flags.FLAG_DEPRECATE_DPM_SUPERVISION_APIS)
    public void testEnforcingAdmin_whenDpmDisablesBiometricsAndSupervisionIsEnabled() {
        setUpMockSupervision(/* supervisionEnabled= */true, keyguardDisabledFeature);

        EnforcingAdmin admin = ParentalControlsUtils.getParentalSupervisionAdminInternal(mContext,
                modality, new UserHandle(UserHandle.myUserId()));
        assertNotNull(admin);
        assertNull(admin.getComponentName());
    }

    @Test
    @RequiresFlagsDisabled(android.app.supervision.flags.Flags.FLAG_DEPRECATE_DPM_SUPERVISION_APIS)
    public void testNoEnforcingAdmin_whenNoSupervisionComponent() {
        // Even if DPM flag exists, returns null EnforcedAdmin when no supervision component exists
        setUpMockSupervisionLegacy(/*supervisionComponentName=*/null, keyguardDisabledFeature);

        EnforcingAdmin admin = ParentalControlsUtils.getParentalSupervisionAdminInternal(mContext,
                modality, new UserHandle(UserHandle.myUserId()));

        assertNull(admin);
    }

    @Test
    @RequiresFlagsEnabled(android.app.supervision.flags.Flags.FLAG_DEPRECATE_DPM_SUPERVISION_APIS)
    public void testNoEnforcingAdmin_whenSupervisionIsDisabled() {
        setUpMockSupervision(/* supervisionEnabled= */false, keyguardDisabledFeature);

        EnforcingAdmin admin = ParentalControlsUtils.getParentalSupervisionAdminInternal(mContext,
                modality, new UserHandle(UserHandle.myUserId()));

        assertNull(admin);
    }
}
