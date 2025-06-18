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

package com.android.settings.connecteddevice.audiosharing.audiostreams;

import static com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast.ACTION_LE_AUDIO_PRIVATE_BROADCAST_RECEIVED;
import static com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast.EXTRA_PRIVATE_BROADCAST_RECEIVE_DATA;
import static com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant.LocalBluetoothLeBroadcastSourceState.STREAMING;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothStatusCodes;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.PrivateBroadcastReceiveData;
import com.android.settingslib.flags.Flags;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowApplication;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothAdapter.class, ShadowBluetoothUtils.class})
public class PrivateBroadcastReceiverTest {
    private static final String DEVICE = "00:A1:A1:A1:A1:A1";
    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    private Context mContext;
    private ShadowApplication mShadowApplication;
    private ShadowBluetoothAdapter mShadowBluetoothAdapter;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.getApplication());
        mShadowApplication = Shadow.extract(mContext);
        mShadowBluetoothAdapter = Shadow.extract(BluetoothAdapter.getDefaultAdapter());
        mShadowBluetoothAdapter.setEnabled(true);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastAssistantSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
    }

    @After
    public void tearDown() {
        ShadowBluetoothUtils.reset();
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void flagOff_doNothing() {
        Intent intent = new Intent(ACTION_LE_AUDIO_PRIVATE_BROADCAST_RECEIVED);
        PrivateBroadcastReceiver receiver = getPrivateBroadcastReceiver(intent);
        receiver.onReceive(mContext, intent);

        verify(mContext, never()).startService(any());
    }

    @Test
    @EnableFlags({Flags.FLAG_ENABLE_LE_AUDIO_SHARING,
            Flags.FLAG_AUDIO_STREAM_MEDIA_SERVICE_BY_RECEIVE_STATE})
    public void receiveDataNotValid_doNothing() {
        Intent intent = new Intent(ACTION_LE_AUDIO_PRIVATE_BROADCAST_RECEIVED);
        intent.putExtra(EXTRA_PRIVATE_BROADCAST_RECEIVE_DATA,
                new PrivateBroadcastReceiveData(/* sink= */ null, /* sourceId= */
                        0, /* broadcastId= */ 0, /* programInfo= */ "", /* state= */ STREAMING));
        PrivateBroadcastReceiver receiver = getPrivateBroadcastReceiver(intent);
        receiver.onReceive(mContext, intent);

        verify(mContext, never()).startService(any());
    }

    @Test
    @EnableFlags({Flags.FLAG_ENABLE_LE_AUDIO_SHARING,
            Flags.FLAG_AUDIO_STREAM_MEDIA_SERVICE_BY_RECEIVE_STATE})
    public void receiveDataValid_startService() {
        Intent intent = new Intent(ACTION_LE_AUDIO_PRIVATE_BROADCAST_RECEIVED);
        intent.putExtra(EXTRA_PRIVATE_BROADCAST_RECEIVE_DATA,
                new PrivateBroadcastReceiveData(
                        /* sink= */ BluetoothAdapter.getDefaultAdapter().getRemoteDevice(DEVICE),
                        /* sourceId= */ 0,
                        /* broadcastId= */ 0,
                        /* programInfo= */ "",
                        /* state= */ STREAMING));
        PrivateBroadcastReceiver receiver = getPrivateBroadcastReceiver(intent);
        receiver.onReceive(mContext, intent);

        verify(mContext).startService(any());
    }

    private PrivateBroadcastReceiver getPrivateBroadcastReceiver(Intent intent) {
        assertThat(mShadowApplication.hasReceiverForIntent(intent)).isTrue();
        List<BroadcastReceiver> receiversForIntent =
                mShadowApplication.getReceiversForIntent(intent);
        assertThat(receiversForIntent).hasSize(1);
        BroadcastReceiver broadcastReceiver = receiversForIntent.getFirst();
        assertThat(broadcastReceiver).isInstanceOf(PrivateBroadcastReceiver.class);
        return (PrivateBroadcastReceiver) broadcastReceiver;
    }
}
