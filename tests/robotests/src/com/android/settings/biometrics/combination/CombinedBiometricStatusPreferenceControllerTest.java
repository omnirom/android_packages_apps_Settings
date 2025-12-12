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

package com.android.settings.biometrics.combination;

import static android.app.admin.UnknownAuthority.UNKNOWN_AUTHORITY;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.app.admin.EnforcingAdmin;
import android.app.admin.flags.Flags;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.face.FaceManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
public class CombinedBiometricStatusPreferenceControllerTest {

    private static final String TEST_PREF_KEY = "foo";

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock
    private LockPatternUtils mLockPatternUtils;
    @Mock
    private FingerprintManager mFingerprintManager;
    @Mock
    private FaceManager mFaceManager;
    @Mock
    private UserManager mUm;
    @Mock
    private PackageManager mPackageManager;

    private FakeFeatureFactory mFeatureFactory;
    private Context mContext;
    private CombinedBiometricStatusPreferenceController mController;
    private Preference mPreference;
    private Lifecycle mLifecycle;
    private LifecycleOwner mLifecycleOwner;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)).thenReturn(true);
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_FACE)).thenReturn(true);
        shadowOf((Application) ApplicationProvider.getApplicationContext())
                .setSystemService(Context.FINGERPRINT_SERVICE, mFingerprintManager);
        shadowOf((Application) ApplicationProvider.getApplicationContext())
                .setSystemService(Context.FACE_SERVICE, mFaceManager);
        shadowOf((Application) ApplicationProvider.getApplicationContext())
                .setSystemService(Context.USER_SERVICE, mUm);
        mPreference = new Preference(mContext);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        when(mFeatureFactory.securityFeatureProvider.getLockPatternUtils(mContext))
                .thenReturn(mLockPatternUtils);
        when(mUm.getProfileIdsWithDisabled(anyInt())).thenReturn(new int[] {1234});
        mController = new CombinedBiometricStatusPreferenceController(
                mContext, TEST_PREF_KEY, mLifecycle);
    }

    @Test
    @DisableFlags(Flags.FLAG_POLICY_TRANSPARENCY_REFACTOR_ENABLED)
    public void updateState_parentalConsentRequired_preferenceDisabled() {
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);

        RestrictedPreference restrictedPreference = mock(RestrictedPreference.class);
        RestrictedLockUtils.EnforcedAdmin admin = mock(RestrictedLockUtils.EnforcedAdmin.class);

        mController.mPreference = restrictedPreference;
        mController.updateStateInternal(admin);
        verify(restrictedPreference).setDisabledByAdmin(eq(admin));

        mController.updateStateInternal((RestrictedLockUtils.EnforcedAdmin) null);
        verify(restrictedPreference)
                .setDisabledByAdmin(eq((RestrictedLockUtils.EnforcedAdmin) null));
    }

    @Test
    @EnableFlags({Flags.FLAG_POLICY_TRANSPARENCY_REFACTOR_ENABLED,
            Flags.FLAG_SET_KEYGUARD_DISABLED_FEATURES_COEXISTENCE})
    public void updateState_dpmFlagEnabled_parentalConsentRequired_preferenceDisabled() {
        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        RestrictedPreference restrictedPreference = mock(RestrictedPreference.class);
        EnforcingAdmin admin = new EnforcingAdmin("package", UNKNOWN_AUTHORITY, UserHandle.CURRENT);

        mController.mPreference = restrictedPreference;
        mController.updateStateInternal(admin);
        verify(restrictedPreference).setDisabledByAdmin(eq(admin));

        mController.updateStateInternal((EnforcingAdmin) null);
        verify(restrictedPreference).setDisabledByAdmin(eq((EnforcingAdmin) null));
    }
}
