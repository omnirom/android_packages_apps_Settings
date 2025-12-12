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

package com.android.settings.connecteddevice.audiosharing;

import static com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast.ACTION_LE_AUDIO_SHARING_DEVICE_CONNECTED;
import static com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast.ACTION_LE_AUDIO_SHARING_STATE_CHANGE;
import static com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast.BROADCAST_STATE_OFF;
import static com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast.BROADCAST_STATE_ON;
import static com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast.EXTRA_BLUETOOTH_DEVICE;
import static com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast.EXTRA_LE_AUDIO_SHARING_STATE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothCsipSetCoordinator;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.bluetooth.BluetoothStatusCodes;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import com.android.settings.bluetooth.Utils;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.R;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.flags.Flags;

import com.google.common.collect.ImmutableList;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowApplication;

import java.util.List;
import java.util.stream.Collectors;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothAdapter.class, ShadowBluetoothUtils.class})
public class AudioSharingReceiverTest {
    private static final String ACTION_LE_AUDIO_SHARING_STOP =
            "com.android.settings.action.BLUETOOTH_LE_AUDIO_SHARING_STOP";
    private static final String ACTION_LE_AUDIO_SHARING_ADD_SOURCE =
            "com.android.settings.action.BLUETOOTH_LE_AUDIO_SHARING_ADD_SOURCE";
    private static final String ACTION_LE_AUDIO_SHARING_CANCEL_NOTIF =
            "com.android.settings.action.BLUETOOTH_LE_AUDIO_SHARING_CANCEL_NOTIF";
    private static final String EXTRA_NOTIF_ID = "NOTIF_ID";
    private static final String TEST_DEVICE_NAME = "test";

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private Context mContext;
    private ShadowApplication mShadowApplication;
    private ShadowBluetoothAdapter mShadowBluetoothAdapter;
    private LocalBluetoothManager mLocalBluetoothManager;
    private FakeFeatureFactory mFeatureFactory;
    @Mock private LocalBluetoothProfileManager mLocalBtProfileManager;
    @Mock private LocalBluetoothLeBroadcast mBroadcast;
    @Mock private LocalBluetoothLeBroadcastAssistant mAssistant;
    @Mock private LocalBluetoothManager mLocalBtManager;
    @Mock private BluetoothDevice mDevice;
    @Mock private NotificationManager mNm;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.getApplication());
        mShadowApplication = Shadow.extract(mContext);
        mShadowApplication.setSystemService(Context.NOTIFICATION_SERVICE, mNm);
        mShadowBluetoothAdapter = Shadow.extract(BluetoothAdapter.getDefaultAdapter());
        mShadowBluetoothAdapter.setEnabled(true);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastAssistantSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBtManager;
        mLocalBluetoothManager = Utils.getLocalBtManager(mContext);
        when(mLocalBluetoothManager.getProfileManager()).thenReturn(mLocalBtProfileManager);
        when(mLocalBtProfileManager.getLeAudioBroadcastProfile()).thenReturn(mBroadcast);
        when(mLocalBtProfileManager.getLeAudioBroadcastAssistantProfile()).thenReturn(mAssistant);
        when(mDevice.getAlias()).thenReturn(TEST_DEVICE_NAME);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
    }

    @After
    public void tearDown() {
        ShadowBluetoothUtils.reset();
    }

    @Test
    public void broadcastReceiver_isRegistered() {
        List<ShadowApplication.Wrapper> registeredReceivers =
                mShadowApplication.getRegisteredReceivers();

        int matchedCount =
                registeredReceivers.stream()
                        .filter(
                                receiver ->
                                        AudioSharingReceiver.class
                                                .getSimpleName()
                                                .equals(
                                                        receiver.broadcastReceiver
                                                                .getClass()
                                                                .getSimpleName()))
                        .collect(Collectors.toList())
                        .size();
        assertThat(matchedCount).isEqualTo(1);
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void broadcastReceiver_receiveAudioSharingStateOn_flagOff_doNothing() {
        Intent intent = new Intent(ACTION_LE_AUDIO_SHARING_STATE_CHANGE);
        intent.setPackage(mContext.getPackageName());
        intent.putExtra(EXTRA_LE_AUDIO_SHARING_STATE, BROADCAST_STATE_ON);
        AudioSharingReceiver audioSharingReceiver = getAudioSharingReceiver(intent);
        audioSharingReceiver.onReceive(mContext, intent);

        verifyNoInteractions(mNm);
        verifyNoInteractions(mFeatureFactory.metricsFeatureProvider);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void broadcastReceiver_receiveAudioSharingStateOn_broadcastDisabled_doNothing() {
        mShadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED);

        Intent intent = new Intent(ACTION_LE_AUDIO_SHARING_STATE_CHANGE);
        intent.setPackage(mContext.getPackageName());
        intent.putExtra(EXTRA_LE_AUDIO_SHARING_STATE, BROADCAST_STATE_ON);
        AudioSharingReceiver audioSharingReceiver = getAudioSharingReceiver(intent);
        audioSharingReceiver.onReceive(mContext, intent);

        verifyNoInteractions(mNm);
        verifyNoInteractions(mFeatureFactory.metricsFeatureProvider);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void broadcastReceiver_receiveAudioSharingStateChangeIntentNoState_doNothing() {
        Intent intent = new Intent(ACTION_LE_AUDIO_SHARING_STATE_CHANGE);
        intent.setPackage(mContext.getPackageName());
        AudioSharingReceiver audioSharingReceiver = getAudioSharingReceiver(intent);
        audioSharingReceiver.onReceive(mContext, intent);

        verifyNoInteractions(mNm);
        verifyNoInteractions(mFeatureFactory.metricsFeatureProvider);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void broadcastReceiver_receiveAudioSharingStateOn_broadcastEnabled_showNotification() {
        Intent intent = new Intent(ACTION_LE_AUDIO_SHARING_STATE_CHANGE);
        intent.setPackage(mContext.getPackageName());
        intent.putExtra(EXTRA_LE_AUDIO_SHARING_STATE, BROADCAST_STATE_ON);
        AudioSharingReceiver audioSharingReceiver = getAudioSharingReceiver(intent);
        audioSharingReceiver.onReceive(mContext, intent);

        verify(mNm).notify(eq(R.drawable.ic_bt_le_audio_sharing), any(Notification.class));
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(mContext, SettingsEnums.ACTION_SHOW_AUDIO_SHARING_NOTIFICATION);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void
            broadcastReceiver_receiveAudioSharingStateOff_broadcastDisabled_cancelNotification() {
        mShadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED);

        Intent intent = new Intent(ACTION_LE_AUDIO_SHARING_STATE_CHANGE);
        intent.setPackage(mContext.getPackageName());
        intent.putExtra(EXTRA_LE_AUDIO_SHARING_STATE, BROADCAST_STATE_OFF);
        AudioSharingReceiver audioSharingReceiver = getAudioSharingReceiver(intent);
        audioSharingReceiver.onReceive(mContext, intent);

        verify(mNm).cancel(R.drawable.ic_bt_le_audio_sharing);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        mContext,
                        SettingsEnums.ACTION_CANCEL_AUDIO_SHARING_NOTIFICATION,
                        ACTION_LE_AUDIO_SHARING_STATE_CHANGE);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void
            broadcastReceiver_receiveAudioSharingStateOff_broadcastEnabled_cancelNotification() {
        Intent intent = new Intent(ACTION_LE_AUDIO_SHARING_STATE_CHANGE);
        intent.setPackage(mContext.getPackageName());
        intent.putExtra(EXTRA_LE_AUDIO_SHARING_STATE, BROADCAST_STATE_OFF);
        AudioSharingReceiver audioSharingReceiver = getAudioSharingReceiver(intent);
        audioSharingReceiver.onReceive(mContext, intent);

        verify(mNm).cancel(R.drawable.ic_bt_le_audio_sharing);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        mContext,
                        SettingsEnums.ACTION_CANCEL_AUDIO_SHARING_NOTIFICATION,
                        ACTION_LE_AUDIO_SHARING_STATE_CHANGE);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void broadcastReceiver_receiveAudioSharingStop_broadcastDisabled_cancelNotification() {
        mShadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED);

        Intent intent = new Intent(ACTION_LE_AUDIO_SHARING_STOP);
        intent.setPackage(mContext.getPackageName());
        AudioSharingReceiver audioSharingReceiver = getAudioSharingReceiver(intent);
        audioSharingReceiver.onReceive(mContext, intent);

        verifyNoInteractions(mBroadcast);
        verify(mNm).cancel(R.drawable.ic_bt_le_audio_sharing);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        mContext,
                        SettingsEnums.ACTION_CANCEL_AUDIO_SHARING_NOTIFICATION,
                        ACTION_LE_AUDIO_SHARING_STOP);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void broadcastReceiver_receiveAudioSharingStop_notInBroadcast_cancelNotification() {
        when(mBroadcast.isEnabled(null)).thenReturn(false);
        int broadcastId = 1;
        when(mBroadcast.getLatestBroadcastId()).thenReturn(broadcastId);

        Intent intent = new Intent(ACTION_LE_AUDIO_SHARING_STOP);
        intent.setPackage(mContext.getPackageName());
        AudioSharingReceiver audioSharingReceiver = getAudioSharingReceiver(intent);
        audioSharingReceiver.onReceive(mContext, intent);

        verify(mBroadcast, never()).stopBroadcast(broadcastId);
        verify(mNm).cancel(R.drawable.ic_bt_le_audio_sharing);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        mContext,
                        SettingsEnums.ACTION_CANCEL_AUDIO_SHARING_NOTIFICATION,
                        ACTION_LE_AUDIO_SHARING_STOP);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void broadcastReceiver_receiveAudioSharingStop_inBroadcast_stopBroadcast() {
        when(mBroadcast.isEnabled(null)).thenReturn(true);
        int broadcastId = 1;
        when(mBroadcast.getLatestBroadcastId()).thenReturn(broadcastId);

        Intent intent = new Intent(ACTION_LE_AUDIO_SHARING_STOP);
        intent.setPackage(mContext.getPackageName());
        AudioSharingReceiver audioSharingReceiver = getAudioSharingReceiver(intent);
        audioSharingReceiver.onReceive(mContext, intent);

        verify(mBroadcast).stopBroadcast(broadcastId);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(mContext, SettingsEnums.ACTION_STOP_AUDIO_SHARING_FROM_NOTIFICATION);
        verify(mNm, never()).cancel(R.drawable.ic_bt_le_audio_sharing);
        verify(mFeatureFactory.metricsFeatureProvider, never())
                .action(
                        mContext,
                        SettingsEnums.ACTION_CANCEL_AUDIO_SHARING_NOTIFICATION,
                        ACTION_LE_AUDIO_SHARING_STOP);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void broadcastReceiver_receiveAudioSharingDeviceConnected_broadcastDisabled_doNothing() {
        mShadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED);

        setAppInForeground(false);
        Intent intent = new Intent(ACTION_LE_AUDIO_SHARING_DEVICE_CONNECTED);
        intent.setPackage(mContext.getPackageName());
        intent.putExtra(EXTRA_BLUETOOTH_DEVICE, mDevice);
        AudioSharingReceiver audioSharingReceiver = getAudioSharingReceiver(intent);
        audioSharingReceiver.onReceive(mContext, intent);

        verify(mNm, never())
                .notify(
                        eq(com.android.settings.R.string.share_audio_notification_title),
                        any(Notification.class));
        verifyNoInteractions(mFeatureFactory.metricsFeatureProvider);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void broadcastReceiver_receiveAudioSharingDeviceConnected_nullArg_doNothing() {
        setAppInForeground(false);
        Intent intent = new Intent(ACTION_LE_AUDIO_SHARING_DEVICE_CONNECTED);
        intent.setPackage(mContext.getPackageName());
        AudioSharingReceiver audioSharingReceiver = getAudioSharingReceiver(intent);
        audioSharingReceiver.onReceive(mContext, intent);

        verify(mNm, never())
                .notify(
                        eq(com.android.settings.R.string.share_audio_notification_title),
                        any(Notification.class));
        verifyNoInteractions(mFeatureFactory.metricsFeatureProvider);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void broadcastReceiver_receiveAudioSharingDeviceConnected_showDialog() {
        setAppInForeground(true);
        Intent intent = new Intent(ACTION_LE_AUDIO_SHARING_DEVICE_CONNECTED);
        intent.setPackage(mContext.getPackageName());
        intent.putExtra(EXTRA_BLUETOOTH_DEVICE, mDevice);
        AudioSharingReceiver audioSharingReceiver = getAudioSharingReceiver(intent);
        audioSharingReceiver.onReceive(mContext, intent);

        verify(mNm, never())
                .notify(
                        eq(com.android.settings.R.string.share_audio_notification_title),
                        any(Notification.class));
        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mContext).startActivity(intentCaptor.capture());
        assertThat(intentCaptor.getValue().getComponent().getClassName())
                .isEqualTo(AudioSharingJoinHandlerActivity.class.getName());
        assertThat(
                        intentCaptor
                                .getValue()
                                .getParcelableExtra(EXTRA_BLUETOOTH_DEVICE, BluetoothDevice.class))
                .isEqualTo(mDevice);
        verifyNoInteractions(mFeatureFactory.metricsFeatureProvider);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void broadcastReceiver_receiveAudioSharingDeviceConnected_notInBroadcast_noNotif() {
        setAppInForeground(false);
        when(mBroadcast.isEnabled(null)).thenReturn(false);

        Intent intent = new Intent(ACTION_LE_AUDIO_SHARING_DEVICE_CONNECTED);
        intent.setPackage(mContext.getPackageName());
        intent.putExtra(EXTRA_BLUETOOTH_DEVICE, mDevice);
        AudioSharingReceiver audioSharingReceiver = getAudioSharingReceiver(intent);
        audioSharingReceiver.onReceive(mContext, intent);

        verify(mNm, never())
                .notify(
                        eq(com.android.settings.R.string.share_audio_notification_title),
                        any(Notification.class));
        verifyNoInteractions(mFeatureFactory.metricsFeatureProvider);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void broadcastReceiver_receiveAudioSharingDeviceConnected_invalidGroupId_noNotif() {
        setAppInForeground(false);
        when(mBroadcast.isEnabled(null)).thenReturn(true);
        CachedBluetoothDeviceManager deviceManager = mock(CachedBluetoothDeviceManager.class);
        when(mLocalBluetoothManager.getCachedDeviceManager()).thenReturn(deviceManager);
        CachedBluetoothDevice cachedDevice = mock(CachedBluetoothDevice.class);
        when(deviceManager.findDevice(mDevice)).thenReturn(cachedDevice);
        when(cachedDevice.getDevice()).thenReturn(mDevice);
        when(cachedDevice.getGroupId()).thenReturn(BluetoothCsipSetCoordinator.GROUP_ID_INVALID);
        when(mAssistant.getAllConnectedDevices()).thenReturn(ImmutableList.of(mDevice));

        Intent intent = new Intent(ACTION_LE_AUDIO_SHARING_DEVICE_CONNECTED);
        intent.setPackage(mContext.getPackageName());
        intent.putExtra(EXTRA_BLUETOOTH_DEVICE, mDevice);
        AudioSharingReceiver audioSharingReceiver = getAudioSharingReceiver(intent);
        audioSharingReceiver.onReceive(mContext, intent);

        verify(mNm, never())
                .notify(
                        eq(com.android.settings.R.string.share_audio_notification_title),
                        any(Notification.class));
        verifyNoInteractions(mFeatureFactory.metricsFeatureProvider);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void broadcastReceiver_receiveAudioSharingDeviceConnected_alreadyTwoSinks_noNotif() {
        setAppInForeground(false);
        when(mBroadcast.isEnabled(null)).thenReturn(true);
        when(mBroadcast.getLatestBroadcastId()).thenReturn(1);
        CachedBluetoothDeviceManager deviceManager = mock(CachedBluetoothDeviceManager.class);
        when(mLocalBluetoothManager.getCachedDeviceManager()).thenReturn(deviceManager);
        CachedBluetoothDevice cachedDevice1 = mock(CachedBluetoothDevice.class);
        when(deviceManager.findDevice(mDevice)).thenReturn(cachedDevice1);
        BluetoothDevice device2 = mock(BluetoothDevice.class);
        CachedBluetoothDevice cachedDevice2 = mock(CachedBluetoothDevice.class);
        when(deviceManager.findDevice(device2)).thenReturn(cachedDevice2);
        when(cachedDevice1.getGroupId()).thenReturn(1);
        when(cachedDevice1.getDevice()).thenReturn(mDevice);
        when(cachedDevice1.getName()).thenReturn(TEST_DEVICE_NAME);
        when(cachedDevice2.getGroupId()).thenReturn(2);
        when(cachedDevice2.getDevice()).thenReturn(device2);
        when(cachedDevice2.getName()).thenReturn(TEST_DEVICE_NAME);
        when(mAssistant.getAllConnectedDevices()).thenReturn(ImmutableList.of(mDevice, device2));
        BluetoothLeBroadcastReceiveState state = mock(BluetoothLeBroadcastReceiveState.class);
        when(state.getBroadcastId()).thenReturn(1);
        when(mAssistant.getAllSources(any())).thenReturn(ImmutableList.of(state));

        Intent intent = new Intent(ACTION_LE_AUDIO_SHARING_DEVICE_CONNECTED);
        intent.setPackage(mContext.getPackageName());
        intent.putExtra(EXTRA_BLUETOOTH_DEVICE, mDevice);
        AudioSharingReceiver audioSharingReceiver = getAudioSharingReceiver(intent);
        audioSharingReceiver.onReceive(mContext, intent);

        verify(mNm, never())
                .notify(
                        eq(com.android.settings.R.string.share_audio_notification_title),
                        any(Notification.class));
        verifyNoInteractions(mFeatureFactory.metricsFeatureProvider);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void broadcastReceiver_receiveAudioSharingDeviceConnected_alreadyHasSource_noNotif() {
        setAppInForeground(false);
        when(mBroadcast.isEnabled(null)).thenReturn(true);
        when(mBroadcast.getLatestBroadcastId()).thenReturn(1);
        CachedBluetoothDeviceManager deviceManager = mock(CachedBluetoothDeviceManager.class);
        when(mLocalBluetoothManager.getCachedDeviceManager()).thenReturn(deviceManager);
        CachedBluetoothDevice cachedDevice = mock(CachedBluetoothDevice.class);
        when(deviceManager.findDevice(mDevice)).thenReturn(cachedDevice);
        when(cachedDevice.getGroupId()).thenReturn(1);
        when(cachedDevice.getDevice()).thenReturn(mDevice);
        when(mAssistant.getAllConnectedDevices()).thenReturn(ImmutableList.of(mDevice));
        BluetoothLeBroadcastReceiveState state = mock(BluetoothLeBroadcastReceiveState.class);
        when(state.getBroadcastId()).thenReturn(1);
        when(mAssistant.getAllSources(mDevice)).thenReturn(ImmutableList.of(state));

        Intent intent = new Intent(ACTION_LE_AUDIO_SHARING_DEVICE_CONNECTED);
        intent.setPackage(mContext.getPackageName());
        intent.putExtra(EXTRA_BLUETOOTH_DEVICE, mDevice);
        AudioSharingReceiver audioSharingReceiver = getAudioSharingReceiver(intent);
        audioSharingReceiver.onReceive(mContext, intent);

        verify(mNm, never())
                .notify(
                        eq(com.android.settings.R.string.share_audio_notification_title),
                        any(Notification.class));
        verifyNoInteractions(mFeatureFactory.metricsFeatureProvider);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void broadcastReceiver_receiveAudioSharingDeviceConnected_showNotification() {
        setAppInForeground(false);
        when(mBroadcast.isEnabled(null)).thenReturn(true);
        when(mBroadcast.getLatestBroadcastId()).thenReturn(1);
        BluetoothLeBroadcastMetadata metadata = mock(BluetoothLeBroadcastMetadata.class);
        when(mBroadcast.getLatestBluetoothLeBroadcastMetadata()).thenReturn(metadata);
        CachedBluetoothDeviceManager deviceManager = mock(CachedBluetoothDeviceManager.class);
        when(mLocalBluetoothManager.getCachedDeviceManager()).thenReturn(deviceManager);
        CachedBluetoothDevice cachedDevice1 = mock(CachedBluetoothDevice.class);
        when(deviceManager.findDevice(mDevice)).thenReturn(cachedDevice1);
        BluetoothDevice device2 = mock(BluetoothDevice.class);
        CachedBluetoothDevice cachedDevice2 = mock(CachedBluetoothDevice.class);
        when(deviceManager.findDevice(device2)).thenReturn(cachedDevice2);
        when(cachedDevice1.getGroupId()).thenReturn(1);
        when(cachedDevice1.getDevice()).thenReturn(mDevice);
        when(cachedDevice2.getGroupId()).thenReturn(2);
        when(cachedDevice2.getDevice()).thenReturn(device2);
        when(mAssistant.getAllConnectedDevices()).thenReturn(ImmutableList.of(mDevice, device2));
        BluetoothLeBroadcastReceiveState state = mock(BluetoothLeBroadcastReceiveState.class);
        when(state.getBroadcastId()).thenReturn(1);
        when(mAssistant.getAllSources(device2)).thenReturn(ImmutableList.of(state));

        Intent intent = new Intent(ACTION_LE_AUDIO_SHARING_DEVICE_CONNECTED);
        intent.setPackage(mContext.getPackageName());
        intent.putExtra(EXTRA_BLUETOOTH_DEVICE, mDevice);
        AudioSharingReceiver audioSharingReceiver = getAudioSharingReceiver(intent);
        audioSharingReceiver.onReceive(mContext, intent);

        verify(mContext, never()).startActivity(any());
        verify(mNm)
                .notify(
                        eq(com.android.settings.R.string.share_audio_notification_title),
                        any(Notification.class));
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(mContext, SettingsEnums.ACTION_SHOW_ADD_SOURCE_NOTIFICATION);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void broadcastReceiver_receiveAudioSharingAddSource_broadcastDisabled_cancelNotif() {
        mShadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.ERROR_BLUETOOTH_NOT_ENABLED);

        Intent intent = new Intent(ACTION_LE_AUDIO_SHARING_ADD_SOURCE);
        intent.setPackage(mContext.getPackageName());
        intent.putExtra(EXTRA_BLUETOOTH_DEVICE, mDevice);
        AudioSharingReceiver audioSharingReceiver = getAudioSharingReceiver(intent);
        audioSharingReceiver.onReceive(mContext, intent);

        verify(mAssistant, never())
                .addSource(eq(mDevice), any(BluetoothLeBroadcastMetadata.class), anyBoolean());
        verify(mNm).cancel(com.android.settings.R.string.share_audio_notification_title);
        verifyNoInteractions(mFeatureFactory.metricsFeatureProvider);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void broadcastReceiver_receiveAudioSharingAddSource_nullArg_cancelNotif() {
        Intent intent = new Intent(ACTION_LE_AUDIO_SHARING_ADD_SOURCE);
        intent.setPackage(mContext.getPackageName());
        AudioSharingReceiver audioSharingReceiver = getAudioSharingReceiver(intent);
        audioSharingReceiver.onReceive(mContext, intent);

        verify(mAssistant, never())
                .addSource(
                        any(BluetoothDevice.class),
                        any(BluetoothLeBroadcastMetadata.class),
                        anyBoolean());
        verify(mNm).cancel(com.android.settings.R.string.share_audio_notification_title);
        verifyNoInteractions(mFeatureFactory.metricsFeatureProvider);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void broadcastReceiver_receiveAudioSharingAddSource_notInBroadcast_cancelNotif() {
        when(mBroadcast.isEnabled(null)).thenReturn(false);

        Intent intent = new Intent(ACTION_LE_AUDIO_SHARING_ADD_SOURCE);
        intent.setPackage(mContext.getPackageName());
        intent.putExtra(EXTRA_BLUETOOTH_DEVICE, mDevice);
        AudioSharingReceiver audioSharingReceiver = getAudioSharingReceiver(intent);
        audioSharingReceiver.onReceive(mContext, intent);

        verify(mAssistant, never())
                .addSource(eq(mDevice), any(BluetoothLeBroadcastMetadata.class), anyBoolean());
        verify(mNm).cancel(com.android.settings.R.string.share_audio_notification_title);
        verifyNoInteractions(mFeatureFactory.metricsFeatureProvider);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void broadcastReceiver_receiveAudioSharingAddSource_notConnected_cancelNotif() {
        when(mBroadcast.isEnabled(null)).thenReturn(true);
        when(mAssistant.getAllConnectedDevices()).thenReturn(ImmutableList.of());

        Intent intent = new Intent(ACTION_LE_AUDIO_SHARING_ADD_SOURCE);
        intent.setPackage(mContext.getPackageName());
        intent.putExtra(EXTRA_BLUETOOTH_DEVICE, mDevice);
        AudioSharingReceiver audioSharingReceiver = getAudioSharingReceiver(intent);
        audioSharingReceiver.onReceive(mContext, intent);

        verify(mAssistant, never())
                .addSource(eq(mDevice), any(BluetoothLeBroadcastMetadata.class), anyBoolean());
        verify(mNm).cancel(com.android.settings.R.string.share_audio_notification_title);
        verifyNoInteractions(mFeatureFactory.metricsFeatureProvider);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void broadcastReceiver_receiveAudioSharingAddSource_invalidGroupId_cancelNotif() {
        when(mBroadcast.isEnabled(null)).thenReturn(true);
        CachedBluetoothDeviceManager deviceManager = mock(CachedBluetoothDeviceManager.class);
        when(mLocalBluetoothManager.getCachedDeviceManager()).thenReturn(deviceManager);
        CachedBluetoothDevice cachedDevice = mock(CachedBluetoothDevice.class);
        when(deviceManager.findDevice(mDevice)).thenReturn(cachedDevice);
        when(cachedDevice.getDevice()).thenReturn(mDevice);
        when(cachedDevice.getGroupId()).thenReturn(BluetoothCsipSetCoordinator.GROUP_ID_INVALID);
        when(mAssistant.getAllConnectedDevices()).thenReturn(ImmutableList.of(mDevice));

        Intent intent = new Intent(ACTION_LE_AUDIO_SHARING_ADD_SOURCE);
        intent.setPackage(mContext.getPackageName());
        intent.putExtra(EXTRA_BLUETOOTH_DEVICE, mDevice);
        AudioSharingReceiver audioSharingReceiver = getAudioSharingReceiver(intent);
        audioSharingReceiver.onReceive(mContext, intent);

        verify(mAssistant, never())
                .addSource(eq(mDevice), any(BluetoothLeBroadcastMetadata.class), anyBoolean());
        verify(mNm).cancel(com.android.settings.R.string.share_audio_notification_title);
        verifyNoInteractions(mFeatureFactory.metricsFeatureProvider);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void broadcastReceiver_receiveAudioSharingAddSource_alreadyTwoSinks_cancelNotif() {
        when(mBroadcast.isEnabled(null)).thenReturn(true);
        when(mBroadcast.getLatestBroadcastId()).thenReturn(1);
        CachedBluetoothDeviceManager deviceManager = mock(CachedBluetoothDeviceManager.class);
        when(mLocalBluetoothManager.getCachedDeviceManager()).thenReturn(deviceManager);
        CachedBluetoothDevice cachedDevice1 = mock(CachedBluetoothDevice.class);
        when(deviceManager.findDevice(mDevice)).thenReturn(cachedDevice1);
        BluetoothDevice device2 = mock(BluetoothDevice.class);
        CachedBluetoothDevice cachedDevice2 = mock(CachedBluetoothDevice.class);
        when(deviceManager.findDevice(device2)).thenReturn(cachedDevice2);
        when(cachedDevice1.getGroupId()).thenReturn(1);
        when(cachedDevice1.getDevice()).thenReturn(mDevice);
        when(cachedDevice1.getName()).thenReturn(TEST_DEVICE_NAME);
        when(cachedDevice2.getGroupId()).thenReturn(2);
        when(cachedDevice2.getDevice()).thenReturn(device2);
        when(cachedDevice2.getName()).thenReturn(TEST_DEVICE_NAME);
        when(mAssistant.getAllConnectedDevices()).thenReturn(ImmutableList.of(mDevice, device2));
        BluetoothLeBroadcastReceiveState state = mock(BluetoothLeBroadcastReceiveState.class);
        when(state.getBroadcastId()).thenReturn(1);
        when(mAssistant.getAllSources(any())).thenReturn(ImmutableList.of(state));

        Intent intent = new Intent(ACTION_LE_AUDIO_SHARING_ADD_SOURCE);
        intent.setPackage(mContext.getPackageName());
        intent.putExtra(EXTRA_BLUETOOTH_DEVICE, mDevice);
        AudioSharingReceiver audioSharingReceiver = getAudioSharingReceiver(intent);
        audioSharingReceiver.onReceive(mContext, intent);

        verify(mAssistant, never())
                .addSource(eq(mDevice), any(BluetoothLeBroadcastMetadata.class), anyBoolean());
        verify(mNm).cancel(com.android.settings.R.string.share_audio_notification_title);
        verifyNoInteractions(mFeatureFactory.metricsFeatureProvider);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void broadcastReceiver_receiveAudioSharingAddSource_alreadyHasSource_cancelNotif() {
        when(mBroadcast.isEnabled(null)).thenReturn(true);
        when(mBroadcast.getLatestBroadcastId()).thenReturn(1);
        CachedBluetoothDeviceManager deviceManager = mock(CachedBluetoothDeviceManager.class);
        when(mLocalBluetoothManager.getCachedDeviceManager()).thenReturn(deviceManager);
        CachedBluetoothDevice cachedDevice = mock(CachedBluetoothDevice.class);
        when(deviceManager.findDevice(mDevice)).thenReturn(cachedDevice);
        when(cachedDevice.getGroupId()).thenReturn(1);
        when(cachedDevice.getDevice()).thenReturn(mDevice);
        when(mAssistant.getAllConnectedDevices()).thenReturn(ImmutableList.of(mDevice));
        BluetoothLeBroadcastReceiveState state = mock(BluetoothLeBroadcastReceiveState.class);
        when(state.getBroadcastId()).thenReturn(1);
        when(mAssistant.getAllSources(mDevice)).thenReturn(ImmutableList.of(state));

        Intent intent = new Intent(ACTION_LE_AUDIO_SHARING_ADD_SOURCE);
        intent.setPackage(mContext.getPackageName());
        intent.putExtra(EXTRA_BLUETOOTH_DEVICE, mDevice);
        AudioSharingReceiver audioSharingReceiver = getAudioSharingReceiver(intent);
        audioSharingReceiver.onReceive(mContext, intent);

        verify(mAssistant, never())
                .addSource(eq(mDevice), any(BluetoothLeBroadcastMetadata.class), anyBoolean());
        verify(mNm).cancel(com.android.settings.R.string.share_audio_notification_title);
        verifyNoInteractions(mFeatureFactory.metricsFeatureProvider);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void broadcastReceiver_receiveAudioSharingAddSource_addSource() {
        when(mBroadcast.isEnabled(null)).thenReturn(true);
        when(mBroadcast.getLatestBroadcastId()).thenReturn(1);
        BluetoothLeBroadcastMetadata metadata = mock(BluetoothLeBroadcastMetadata.class);
        when(mBroadcast.getLatestBluetoothLeBroadcastMetadata()).thenReturn(metadata);
        CachedBluetoothDeviceManager deviceManager = mock(CachedBluetoothDeviceManager.class);
        when(mLocalBluetoothManager.getCachedDeviceManager()).thenReturn(deviceManager);
        CachedBluetoothDevice cachedDevice1 = mock(CachedBluetoothDevice.class);
        when(deviceManager.findDevice(mDevice)).thenReturn(cachedDevice1);
        BluetoothDevice device2 = mock(BluetoothDevice.class);
        CachedBluetoothDevice cachedDevice2 = mock(CachedBluetoothDevice.class);
        when(deviceManager.findDevice(device2)).thenReturn(cachedDevice2);
        when(cachedDevice1.getGroupId()).thenReturn(1);
        when(cachedDevice1.getDevice()).thenReturn(mDevice);
        when(cachedDevice2.getGroupId()).thenReturn(2);
        when(cachedDevice2.getDevice()).thenReturn(device2);
        when(mAssistant.getAllConnectedDevices()).thenReturn(ImmutableList.of(mDevice, device2));
        BluetoothLeBroadcastReceiveState state = mock(BluetoothLeBroadcastReceiveState.class);
        when(state.getBroadcastId()).thenReturn(1);
        when(mAssistant.getAllSources(device2)).thenReturn(ImmutableList.of(state));

        Intent intent = new Intent(ACTION_LE_AUDIO_SHARING_ADD_SOURCE);
        intent.setPackage(mContext.getPackageName());
        intent.putExtra(EXTRA_BLUETOOTH_DEVICE, mDevice);
        AudioSharingReceiver audioSharingReceiver = getAudioSharingReceiver(intent);
        audioSharingReceiver.onReceive(mContext, intent);

        verify(mAssistant).addSource(mDevice, metadata, /* isGroupOp= */ false);
        verify(mNm).cancel(com.android.settings.R.string.share_audio_notification_title);
        verify(mFeatureFactory.metricsFeatureProvider, never())
                .action(mContext, SettingsEnums.ACTION_CANCEL_ADD_SOURCE_NOTIFICATION);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        SettingsEnums.ACTION_SHOW_ADD_SOURCE_NOTIFICATION,
                        SettingsEnums.ACTION_AUDIO_SHARING_ADD_SOURCE,
                        SettingsEnums.ACTION_SHOW_ADD_SOURCE_NOTIFICATION,
                        AudioSharingUtils.buildAddSourceEventData(
                                        SettingsEnums.ACTION_SHOW_ADD_SOURCE_NOTIFICATION,
                                        /* userTriggered= */ false)
                                .toString(),
                        /* changedPreferenceIntValue */ 0);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void broadcastReceiver_receiveAudioSharingCancelNotif_cancel() {
        Intent intent = new Intent(ACTION_LE_AUDIO_SHARING_CANCEL_NOTIF);
        intent.setPackage(mContext.getPackageName());
        intent.putExtra(
                EXTRA_NOTIF_ID, com.android.settings.R.string.share_audio_notification_title);
        AudioSharingReceiver audioSharingReceiver = getAudioSharingReceiver(intent);
        audioSharingReceiver.onReceive(mContext, intent);

        verify(mNm).cancel(com.android.settings.R.string.share_audio_notification_title);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(mContext, SettingsEnums.ACTION_CANCEL_ADD_SOURCE_NOTIFICATION);
    }

    private AudioSharingReceiver getAudioSharingReceiver(Intent intent) {
        assertThat(mShadowApplication.hasReceiverForIntent(intent)).isTrue();
        List<BroadcastReceiver> receiversForIntent =
                mShadowApplication.getReceiversForIntent(intent);
        assertThat(receiversForIntent).hasSize(1);
        BroadcastReceiver broadcastReceiver = receiversForIntent.get(0);
        assertThat(broadcastReceiver).isInstanceOf(AudioSharingReceiver.class);
        return (AudioSharingReceiver) broadcastReceiver;
    }

    private void setAppInForeground(boolean foreground) {
        ActivityManager activityManager = mock(ActivityManager.class);
        when(mContext.getSystemService(ActivityManager.class)).thenReturn(activityManager);
        when(activityManager.getPackageImportance(mContext.getPackageName()))
                .thenReturn(
                        foreground
                                ? ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                                : ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE);
        PackageManager packageManager = mock(PackageManager.class);
        when(mContext.getPackageManager()).thenReturn(packageManager);
        when(packageManager.checkPermission(
                        Manifest.permission.PACKAGE_USAGE_STATS, mContext.getPackageName()))
                .thenReturn(PackageManager.PERMISSION_GRANTED);
    }
}
