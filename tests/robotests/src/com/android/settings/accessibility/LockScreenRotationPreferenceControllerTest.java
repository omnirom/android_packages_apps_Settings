/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.accessibility;

import static com.android.settings.testutils.DeviceStateAutoRotateSettingTestUtils.setDeviceStateRotationLockEnabled;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.hardware.devicestate.DeviceStateManager;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.SwitchPreference;

import com.android.internal.view.RotationPolicy;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.testutils.shadow.ShadowRotationPolicy;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowSystemSettings.class,
})
public class LockScreenRotationPreferenceControllerTest {
    @Mock
    private Resources mResources;
    @Mock
    private DeviceStateManager mDeviceStateManager;
    private Context mContext;
    private SwitchPreference mPreference;
    private LockScreenRotationPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = Mockito.spy(RuntimeEnvironment.application);
        mPreference = new SwitchPreference(mContext);
        when(mContext.getResources()).thenReturn(mResources);
        doReturn(mDeviceStateManager).when(mContext).getSystemService(DeviceStateManager.class);

        mController = new LockScreenRotationPreferenceController(mContext, "lock_screen");
    }

    @Test
    @Config(shadows = {ShadowRotationPolicy.class})
    public void getAvailabilityStatus_supportedRotation_shouldReturnAvailable() {
        ShadowRotationPolicy.setRotationSupported(true /* supported */);
        setDeviceStateRotationLockEnabled(/* enable= */ false, mResources, mDeviceStateManager);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }

    @Test
    @Config(shadows = {ShadowRotationPolicy.class})
    public void getAvailabilityStatus_deviceStateRotationEnabled_returnsUnsupported() {
        ShadowRotationPolicy.setRotationSupported(true /* supported */);
        setDeviceStateRotationLockEnabled(/* enable= */ true, mResources, mDeviceStateManager);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    @Config(shadows = {ShadowRotationPolicy.class})
    public void getAvailabilityStatus_unsupportedRotation_shouldReturnUnsupportedOnDevice() {
        ShadowRotationPolicy.setRotationSupported(false /* supported */);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    @Config(shadows = {ShadowRotationPolicy.class})
    public void setChecked_enabled() {
        mController.setChecked(true /* isChecked */);

        assertThat(mController.isChecked()).isTrue();
        assertThat(RotationPolicy.isRotationLocked(mContext)).isFalse();
    }

    @Test
    @Config(shadows = {ShadowRotationPolicy.class})
    public void setChecked_disabled() {
        mController.setChecked(false /* isChecked */);

        assertThat(mController.isChecked()).isFalse();
        assertThat(RotationPolicy.isRotationLocked(mContext)).isTrue();
    }

    @Test
    public void updateState_settingIsOn_shouldTurnOnToggle() {
        Settings.System.putIntForUser(mContext.getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION, 1, UserHandle.USER_CURRENT);

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void updateState_settingIsOff_shouldTurnOffToggle() {
        Settings.System.putIntForUser(mContext.getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION, 0, UserHandle.USER_CURRENT);

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isFalse();
    }
}
