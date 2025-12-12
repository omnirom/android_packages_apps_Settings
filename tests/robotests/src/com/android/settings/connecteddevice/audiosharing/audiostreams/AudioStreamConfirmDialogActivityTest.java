/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.connecteddevice.audiosharing.audiostreams;

import static com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamConfirmDialogActivity.isBroadcastScheme;
import static com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamsDashboardFragment.KEY_BROADCAST_METADATA;
import static com.android.settingslib.bluetooth.BluetoothBroadcastUtils.SCHEME_BT_BROADCAST_METADATA;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothStatusCodes;
import android.content.Intent;
import android.platform.test.flag.junit.SetFlagsRule;

import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.LeAudioProfile;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.bluetooth.VolumeControlProfile;
import com.android.settingslib.flags.Flags;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowBluetoothAdapter.class,
            ShadowBluetoothUtils.class,
        })
public class AudioStreamConfirmDialogActivityTest {
    @Rule public final MockitoRule mocks = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Mock private LocalBluetoothManager mLocalBluetoothManager;
    @Mock private LocalBluetoothProfileManager mLocalBluetoothProfileManager;
    @Mock private LeAudioProfile mLeAudio;
    @Mock private LocalBluetoothLeBroadcast mBroadcast;
    @Mock private LocalBluetoothLeBroadcastAssistant mAssistant;
    @Mock private VolumeControlProfile mVolumeControl;
    private final Intent mValidIntent = getValidIntent();
    private ShadowBluetoothAdapter mShadowBluetoothAdapter;
    private AudioStreamConfirmDialogActivity mActivity;

    @Before
    public void setUp() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING);
        mShadowBluetoothAdapter = Shadow.extract(BluetoothAdapter.getDefaultAdapter());
        mShadowBluetoothAdapter.setEnabled(true);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastAssistantSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBluetoothManager;
        when(mLocalBluetoothManager.getProfileManager()).thenReturn(mLocalBluetoothProfileManager);
        when(mLocalBluetoothProfileManager.getLeAudioProfile()).thenReturn(mLeAudio);
        when(mLocalBluetoothProfileManager.getLeAudioBroadcastProfile()).thenReturn(mBroadcast);
        when(mLocalBluetoothProfileManager.getLeAudioBroadcastAssistantProfile())
                .thenReturn(mAssistant);
        when(mLocalBluetoothProfileManager.getVolumeControlProfile()).thenReturn(mVolumeControl);
        when(mLeAudio.isProfileReady()).thenReturn(true);
        when(mBroadcast.isProfileReady()).thenReturn(true);
        when(mAssistant.isProfileReady()).thenReturn(true);
        when(mVolumeControl.isProfileReady()).thenReturn(true);
    }

    @After
    public void tearDown() {
        ShadowBluetoothUtils.reset();
    }

    @Test
    public void isValidFragment_returnsTrue() {
        mActivity =
                Robolectric.buildActivity(AudioStreamConfirmDialogActivity.class, mValidIntent)
                        .setup()
                        .get();
        assertThat(mActivity.isValidFragment(AudioStreamConfirmDialog.class.getName())).isTrue();
    }

    @Test
    public void isValidFragment_returnsFalse() {
        mActivity =
                Robolectric.buildActivity(AudioStreamConfirmDialogActivity.class, mValidIntent)
                        .setup()
                        .get();
        assertThat(mActivity.isValidFragment("")).isFalse();
    }

    @Test
    public void isToolbarEnabled_returnsFalse() {
        mActivity =
                Robolectric.buildActivity(AudioStreamConfirmDialogActivity.class, mValidIntent)
                        .setup()
                        .get();
        assertThat(mActivity.isToolbarEnabled()).isFalse();
    }

    @Test
    public void setupActivity_serviceNotReady_registerCallback() {
        when(mBroadcast.isProfileReady()).thenReturn(false);
        mActivity =
                Robolectric.buildActivity(AudioStreamConfirmDialogActivity.class, mValidIntent)
                        .setup()
                        .get();

        verify(mLocalBluetoothProfileManager).addServiceListener(any());
    }

    @Test
    public void setupActivity_serviceNotReady_registerCallback_onServiceCallback() {
        when(mBroadcast.isProfileReady()).thenReturn(false);
        mActivity =
                Robolectric.buildActivity(AudioStreamConfirmDialogActivity.class, mValidIntent)
                        .setup()
                        .get();

        verify(mLocalBluetoothProfileManager).addServiceListener(any());

        when(mBroadcast.isProfileReady()).thenReturn(true);
        mActivity.onServiceConnected();
        verify(mLocalBluetoothProfileManager).removeServiceListener(any());

        mActivity.onServiceDisconnected();
        // Do nothing.
    }

    @Test
    public void setupActivity_serviceReady_doNothing() {
        mActivity =
                Robolectric.buildActivity(AudioStreamConfirmDialogActivity.class, mValidIntent)
                        .setup()
                        .get();

        verify(mLocalBluetoothProfileManager, never()).addServiceListener(any());
    }

    @Test
    public void setupActivity_serviceNotReady_bluetoothOff_doNothing() {
        when(mAssistant.isProfileReady()).thenReturn(false);
        mShadowBluetoothAdapter.setEnabled(false);
        mActivity =
                Robolectric.buildActivity(AudioStreamConfirmDialogActivity.class, mValidIntent)
                        .setup()
                        .get();

        verify(mLocalBluetoothProfileManager, never()).addServiceListener(any());
    }

    @Test
    public void onStop_unregisterCallback() {
        mActivity =
                Robolectric.buildActivity(AudioStreamConfirmDialogActivity.class, mValidIntent)
                        .setup()
                        .get();
        mActivity.onStop();

        verify(mLocalBluetoothProfileManager).removeServiceListener(any());
    }

    @Test
    public void isBroadcastScheme_nullIntent_returnsFalse() {
        assertThat(isBroadcastScheme(null)).isFalse();
    }

    @Test
    public void isBroadcastScheme_intentWithNullMetadataAndNullData_returnsFalse() {
        assertThat(isBroadcastScheme(new Intent())).isFalse();
    }

    @Test
    public void isBroadcastScheme_intentWithEmptyMetadataAndNullData_returnsFalse() {
        Intent intent = new Intent();
        intent.putExtra(KEY_BROADCAST_METADATA, "");
        assertThat(isBroadcastScheme(intent)).isFalse();
    }

    @Test
    public void isBroadcastScheme_intentWithNonMatchingMetadata_returnsFalse() {
        Intent intent = new Intent();
        intent.putExtra(KEY_BROADCAST_METADATA, "NON_MATCHING_METADATA");
        assertThat(isBroadcastScheme(intent)).isFalse();
    }

    @Test
    public void isBroadcastScheme_intentWithMatchingMetadata_returnsTrue() {
        Intent intent = new Intent();
        intent.putExtra(KEY_BROADCAST_METADATA, SCHEME_BT_BROADCAST_METADATA + "_some_suffix");
        assertThat(isBroadcastScheme(intent)).isTrue();
    }

    @Test
    public void isBroadcastScheme_intentWithEmptyDataAndNoMetadata_returnsFalse() {
        Intent intent = new Intent();
        intent.setData(android.net.Uri.parse(""));
        assertThat(isBroadcastScheme(intent)).isFalse();
    }

    @Test
    public void isBroadcastScheme_intentWithNonMatchingData_returnsFalse() {
        Intent intent = new Intent();
        intent.setData(android.net.Uri.parse("NON_MATCHING_DATA_SCHEME"));
        assertThat(isBroadcastScheme(intent)).isFalse();
    }

    @Test
    public void isBroadcastScheme_intentWithMatchingData_returnsTrue() {
        Intent intent = new Intent();
        intent.setData(android.net.Uri.parse(SCHEME_BT_BROADCAST_METADATA));
        assertThat(isBroadcastScheme(intent)).isTrue();
    }

    private Intent getValidIntent() {
        Intent intent = new Intent();
        intent.putExtra(KEY_BROADCAST_METADATA, SCHEME_BT_BROADCAST_METADATA + "_some_suffix");
        return intent;
    }
}
