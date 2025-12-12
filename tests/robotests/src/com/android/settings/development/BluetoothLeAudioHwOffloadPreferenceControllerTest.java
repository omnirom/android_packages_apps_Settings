/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static android.bluetooth.BluetoothStatusCodes.FEATURE_SUPPORTED;

import static com.android.settings.development.BluetoothA2dpHwOffloadPreferenceController
        .A2DP_OFFLOAD_DISABLED_PROPERTY;
import static com.android.settings.development.BluetoothA2dpHwOffloadPreferenceController
        .A2DP_OFFLOAD_SUPPORTED_PROPERTY;
import static com.android.settings.development.BluetoothLeAudioHwOffloadPreferenceController
        .LE_AUDIO_OFFLOAD_DISABLED_PROPERTY;
import static com.android.settings.development.BluetoothLeAudioHwOffloadPreferenceController
        .LE_AUDIO_OFFLOAD_SUPPORTED_PROPERTY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.SystemProperties;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class BluetoothLeAudioHwOffloadPreferenceControllerTest {

    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private DevelopmentSettingsDashboardFragment mFragment;
    @Mock
    private BluetoothAdapter mBluetoothAdapter;

    private Context mContext;
    private SwitchPreference mPreference;
    private BluetoothLeAudioHwOffloadPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mPreference = new SwitchPreference(mContext);
        mController = spy(new BluetoothLeAudioHwOffloadPreferenceController(mContext, mFragment));
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
            .thenReturn(mPreference);
        mController.displayPreference(mPreferenceScreen);
        mController.mBluetoothAdapter = mBluetoothAdapter;
        when(mBluetoothAdapter.isLeAudioSupported())
            .thenReturn(FEATURE_SUPPORTED);
    }

    @Test
    public void onLeAudioHwDialogConfirmedAsLeAudioOffloadDisabled_shouldChangeProperty() {
        SystemProperties.set(LE_AUDIO_OFFLOAD_DISABLED_PROPERTY, Boolean.toString(false));
        mController.mChanged = true;

        mController.onRebootDialogConfirmed();
        final boolean mode = SystemProperties.getBoolean(LE_AUDIO_OFFLOAD_DISABLED_PROPERTY, false);
        assertThat(mode).isTrue();
    }

    @Test
    public void onLeAudioHwDialogConfirmedAsLeAudioOffloadEnabled_shouldChangeProperty() {
        SystemProperties.set(LE_AUDIO_OFFLOAD_DISABLED_PROPERTY, Boolean.toString(true));
        mController.mChanged = true;

        mController.onRebootDialogConfirmed();
        final boolean mode2 = SystemProperties.getBoolean(
                LE_AUDIO_OFFLOAD_DISABLED_PROPERTY, true);
        assertThat(mode2).isFalse();
    }

    @Test
    public void onLeAudioHwDialogCanceled_shouldNotChangeProperty() {
        SystemProperties.set(LE_AUDIO_OFFLOAD_DISABLED_PROPERTY, Boolean.toString(false));
        mController.mChanged = true;

        mController.onRebootDialogCanceled();
        final boolean mode = SystemProperties.getBoolean(LE_AUDIO_OFFLOAD_DISABLED_PROPERTY, false);
        assertThat(mode).isFalse();
    }

    @Test
    public void asA2dpOffloadDisabled_shouldNotSwitchLeAudioOffloadStatus() {
        SystemProperties.set(LE_AUDIO_OFFLOAD_SUPPORTED_PROPERTY, Boolean.toString(true));
        SystemProperties.set(A2DP_OFFLOAD_DISABLED_PROPERTY, Boolean.toString(true));

        SystemProperties.set(LE_AUDIO_OFFLOAD_DISABLED_PROPERTY, Boolean.toString(false));
        mController.updateState(null);
        boolean leAueioDisabled =
                SystemProperties.getBoolean(LE_AUDIO_OFFLOAD_DISABLED_PROPERTY, false);
        assertThat(leAueioDisabled).isFalse();

        SystemProperties.set(LE_AUDIO_OFFLOAD_DISABLED_PROPERTY, Boolean.toString(true));
        mController.updateState(null);
        leAueioDisabled = SystemProperties.getBoolean(LE_AUDIO_OFFLOAD_DISABLED_PROPERTY, false);
        assertThat(leAueioDisabled).isTrue();
    }

    @Test
    public void updateState_leAudioOffloadSupported_propertyDisabled_setCheckedTrue() {
        // LE Audio and A2DP offload are supported and enabled.
        SystemProperties.set(LE_AUDIO_OFFLOAD_SUPPORTED_PROPERTY, Boolean.toString(true));
        SystemProperties.set(A2DP_OFFLOAD_DISABLED_PROPERTY, Boolean.toString(false));
        // LE Audio offload is disabled by the property.
        SystemProperties.set(LE_AUDIO_OFFLOAD_DISABLED_PROPERTY, Boolean.toString(true));

        mController.updateState(mPreference);

        // The preference should be enabled and checked (to indicate it's disabled).
        assertThat(mPreference.isEnabled()).isTrue();
        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void updateState_leAudioOffloadSupported_propertyEnabled_setCheckedFalse() {
        // LE Audio and A2DP offload are supported and enabled.
        SystemProperties.set(LE_AUDIO_OFFLOAD_SUPPORTED_PROPERTY, Boolean.toString(true));
        SystemProperties.set(A2DP_OFFLOAD_DISABLED_PROPERTY, Boolean.toString(false));
        // LE Audio offload is enabled by the property.
        SystemProperties.set(LE_AUDIO_OFFLOAD_DISABLED_PROPERTY, Boolean.toString(false));

        mController.updateState(mPreference);

        // The preference should be enabled and unchecked.
        assertThat(mPreference.isEnabled()).isTrue();
        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void updateState_leAudioOffloadSupported_propertyUnset_setCheckedFalse() {
        // LE Audio and A2DP offload are supported and enabled.
        SystemProperties.set(LE_AUDIO_OFFLOAD_SUPPORTED_PROPERTY, Boolean.toString(true));
        SystemProperties.set(A2DP_OFFLOAD_DISABLED_PROPERTY, Boolean.toString(false));
        // LE Audio offload property is not set, so it should default to false (enabled).
        SystemProperties.set(LE_AUDIO_OFFLOAD_DISABLED_PROPERTY, null);

        mController.updateState(mPreference);

        // The preference should be enabled and unchecked, reflecting the new default.
        assertThat(mPreference.isEnabled()).isTrue();
        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void updateState_leAudioOffloadNotSupported_setDisabledAndChecked() {
        // LE Audio offload is not supported.
        SystemProperties.set(LE_AUDIO_OFFLOAD_SUPPORTED_PROPERTY, Boolean.toString(false));
        SystemProperties.set(A2DP_OFFLOAD_DISABLED_PROPERTY, Boolean.toString(false));
        SystemProperties.set(LE_AUDIO_OFFLOAD_DISABLED_PROPERTY, Boolean.toString(false));

        mController.updateState(mPreference);

        // The preference should be disabled and checked.
        assertThat(mPreference.isEnabled()).isFalse();
        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void updateState_a2dpOffloadDisabled_setDisabledAndChecked() {
        // A2DP offload is disabled, which also disables LE audio offload control.
        SystemProperties.set(LE_AUDIO_OFFLOAD_SUPPORTED_PROPERTY, Boolean.toString(true));
        SystemProperties.set(A2DP_OFFLOAD_DISABLED_PROPERTY, Boolean.toString(true));
        SystemProperties.set(LE_AUDIO_OFFLOAD_DISABLED_PROPERTY, Boolean.toString(false));

        mController.updateState(mPreference);

        // The preference should be disabled and checked.
        assertThat(mPreference.isEnabled()).isFalse();
        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void asDisableDeveloperOption_ResetLEOffloadBasedOnA2dpLeAudioOffloadSupported() {
        SystemProperties.set(LE_AUDIO_OFFLOAD_SUPPORTED_PROPERTY, Boolean.toString(true));
        SystemProperties.set(A2DP_OFFLOAD_SUPPORTED_PROPERTY, Boolean.toString(true));

        SystemProperties.set(
                LE_AUDIO_OFFLOAD_DISABLED_PROPERTY, Boolean.toString(true));
        mController.onDeveloperOptionsSwitchDisabled();
        boolean leAueioDisabled =
                SystemProperties.getBoolean(LE_AUDIO_OFFLOAD_DISABLED_PROPERTY, false);
        assertThat(leAueioDisabled).isFalse();
    }

    @Test
    public void isDefaultValue_offloadNotSupported_shouldReturnTrue() {
        // LE Audio offload is not supported.
        SystemProperties.set(LE_AUDIO_OFFLOAD_SUPPORTED_PROPERTY, Boolean.toString(false));
        SystemProperties.set(LE_AUDIO_OFFLOAD_DISABLED_PROPERTY, Boolean.toString(false));

        assertThat(mController.isDefaultValue()).isTrue();
    }

    @Test
    public void isDefaultValue_offloadSupportedAndEnabled_shouldReturnTrue() {
        // Offload is supported
        SystemProperties.set(LE_AUDIO_OFFLOAD_SUPPORTED_PROPERTY, Boolean.toString(true));
        // LE Audio offload is NOT disabled (i.e., it's enabled), which is the default state.
        SystemProperties.set(LE_AUDIO_OFFLOAD_DISABLED_PROPERTY, Boolean.toString(false));

        assertThat(mController.isDefaultValue()).isTrue();
    }

    @Test
    public void isDefaultValue_offloadSupportedAndDisabled_shouldReturnFalse() {
        // Offload is supported
        SystemProperties.set(LE_AUDIO_OFFLOAD_SUPPORTED_PROPERTY, Boolean.toString(true));
        // LE Audio offload is disabled, which is NOT the default state.
        SystemProperties.set(LE_AUDIO_OFFLOAD_DISABLED_PROPERTY, Boolean.toString(true));

        assertThat(mController.isDefaultValue()).isFalse();
    }
}
