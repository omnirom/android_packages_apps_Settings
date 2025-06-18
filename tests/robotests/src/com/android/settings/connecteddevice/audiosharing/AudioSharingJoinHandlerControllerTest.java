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

package com.android.settings.connecteddevice.audiosharing;

import static com.android.settings.core.BasePreferenceController.AVAILABLE_UNSEARCHABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;
import static com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast.EXTRA_BLUETOOTH_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastAssistant;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothStatusCodes;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.bluetooth.Utils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settings.testutils.shadow.ShadowFragment;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.BluetoothEventManager;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LeAudioProfile;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.flags.Flags;

import com.google.common.collect.ImmutableList;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

import java.util.concurrent.Executor;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowBluetoothAdapter.class,
        ShadowBluetoothUtils.class,
        ShadowFragment.class,
})
public class AudioSharingJoinHandlerControllerTest {
    private static final String PREF_KEY = "audio_sharing_join_handler";

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Spy
    Context mContext = ApplicationProvider.getApplicationContext();
    private Lifecycle mLifecycle;
    private LifecycleOwner mLifecycleOwner;
    @Mock
    private LocalBluetoothManager mLocalBtManager;
    @Mock private BluetoothEventManager mEventManager;
    @Mock private LocalBluetoothProfileManager mProfileManager;
    @Mock private CachedBluetoothDeviceManager mDeviceManager;
    @Mock private LocalBluetoothLeBroadcastAssistant mAssistant;
    @Mock private PreferenceScreen mScreen;
    @Mock private DashboardFragment mFragment;
    @Mock private FragmentActivity mActivity;
    @Mock private AudioSharingDialogHandler mDialogHandler;
    private AudioSharingJoinHandlerController mController;

    @Before
    public void setUp() {
        ShadowBluetoothAdapter shadowBluetoothAdapter =
                Shadow.extract(BluetoothAdapter.getDefaultAdapter());
        shadowBluetoothAdapter.setEnabled(true);
        shadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        shadowBluetoothAdapter.setIsLeAudioBroadcastAssistantSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBtManager;
        mLocalBtManager = Utils.getLocalBtManager(mContext);
        when(mLocalBtManager.getEventManager()).thenReturn(mEventManager);
        when(mLocalBtManager.getProfileManager()).thenReturn(mProfileManager);
        when(mLocalBtManager.getCachedDeviceManager()).thenReturn(mDeviceManager);
        when(mProfileManager.getLeAudioBroadcastAssistantProfile()).thenReturn(mAssistant);
        mController = new AudioSharingJoinHandlerController(mContext, PREF_KEY);
        doReturn(mActivity).when(mFragment).getActivity();
        mController.init(mFragment);
        mController.setDialogHandler(mDialogHandler);
    }

    @After
    public void tearDown() {
        ShadowBluetoothUtils.reset();
    }

    @Test
    @DisableFlags(Flags.FLAG_PROMOTE_AUDIO_SHARING_FOR_SECOND_AUTO_CONNECTED_LEA_DEVICE)
    public void onStart_flagOff_doNothing() {
        mController.onStart(mLifecycleOwner);
        verify(mEventManager, never()).registerCallback(any(BluetoothCallback.class));
        verify(mDialogHandler, never()).registerCallbacks(any(Executor.class));
        verify(mAssistant, never())
                .registerServiceCallBack(
                        any(Executor.class), any(BluetoothLeBroadcastAssistant.Callback.class));
    }

    @Test
    @EnableFlags({Flags.FLAG_ENABLE_LE_AUDIO_SHARING,
            Flags.FLAG_PROMOTE_AUDIO_SHARING_FOR_SECOND_AUTO_CONNECTED_LEA_DEVICE})
    public void onStart_flagOn_registerCallbacks() {
        mController.onStart(mLifecycleOwner);
        verify(mEventManager).registerCallback(any(BluetoothCallback.class));
        verify(mDialogHandler).registerCallbacks(any(Executor.class));
        verify(mAssistant)
                .registerServiceCallBack(
                        any(Executor.class), any(BluetoothLeBroadcastAssistant.Callback.class));
    }

    @Test
    @DisableFlags(Flags.FLAG_PROMOTE_AUDIO_SHARING_FOR_SECOND_AUTO_CONNECTED_LEA_DEVICE)
    public void onStop_flagOff_doNothing() {
        mController.onStop(mLifecycleOwner);
        verify(mEventManager, never()).unregisterCallback(any(BluetoothCallback.class));
        verify(mDialogHandler, never()).unregisterCallbacks();
        verify(mAssistant, never())
                .unregisterServiceCallBack(any(BluetoothLeBroadcastAssistant.Callback.class));
    }

    @Test
    @EnableFlags({Flags.FLAG_ENABLE_LE_AUDIO_SHARING,
            Flags.FLAG_PROMOTE_AUDIO_SHARING_FOR_SECOND_AUTO_CONNECTED_LEA_DEVICE})
    public void onStop_flagOn_unregisterCallbacks() {
        mController.onStop(mLifecycleOwner);
        verify(mEventManager).unregisterCallback(any(BluetoothCallback.class));
        verify(mDialogHandler).unregisterCallbacks();
        verify(mAssistant)
                .unregisterServiceCallBack(any(BluetoothLeBroadcastAssistant.Callback.class));
    }

    @Test
    @EnableFlags({Flags.FLAG_PROMOTE_AUDIO_SHARING_FOR_SECOND_AUTO_CONNECTED_LEA_DEVICE,
            Flags.FLAG_ENABLE_LE_AUDIO_SHARING})
    public void getAvailabilityStatus_flagOn() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE_UNSEARCHABLE);
    }

    @Test
    @DisableFlags(Flags.FLAG_PROMOTE_AUDIO_SHARING_FOR_SECOND_AUTO_CONNECTED_LEA_DEVICE)
    public void getAvailabilityStatus_flagOff() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getPreferenceKey_returnsCorrectKey() {
        assertThat(mController.getPreferenceKey()).isEqualTo(PREF_KEY);
    }

    @Test
    public void getSliceHighlightMenuRes_returnsZero() {
        assertThat(mController.getSliceHighlightMenuRes()).isEqualTo(0);
    }

    @Test
    @EnableFlags({Flags.FLAG_ENABLE_LE_AUDIO_SHARING,
            Flags.FLAG_PROMOTE_AUDIO_SHARING_FOR_SECOND_AUTO_CONNECTED_LEA_DEVICE})
    public void displayPreference_flagOn_updateDeviceList() {
        mController.displayPreference(mScreen);

    }

    @Test
    public void onProfileConnectionStateChanged_notDisconnectedProfile_doNothing() {
        CachedBluetoothDevice cachedDevice = mock(CachedBluetoothDevice.class);

        mController.onProfileConnectionStateChanged(
                cachedDevice, BluetoothAdapter.STATE_CONNECTED,
                BluetoothProfile.LE_AUDIO_BROADCAST_ASSISTANT);
        verifyNoInteractions(mDialogHandler);
    }

    @Test
    public void onProfileConnectionStateChanged_leaDeviceDisconnected_closeOpeningDialogsForIt() {
        // Test when LEA device LE_AUDIO_BROADCAST_ASSISTANT disconnected.
        BluetoothDevice device = mock(BluetoothDevice.class);
        CachedBluetoothDevice cachedDevice = mock(CachedBluetoothDevice.class);
        LeAudioProfile profile = mock(LeAudioProfile.class);
        when(profile.isEnabled(device)).thenReturn(true);
        when(cachedDevice.getProfiles()).thenReturn(ImmutableList.of(profile));
        when(cachedDevice.isConnected()).thenReturn(true);
        when(cachedDevice.getDevice()).thenReturn(device);

        mController.onProfileConnectionStateChanged(
                cachedDevice,
                BluetoothAdapter.STATE_DISCONNECTED,
                BluetoothProfile.LE_AUDIO_BROADCAST_ASSISTANT);
        verify(mDialogHandler).closeOpeningDialogsForLeaDevice(cachedDevice);
    }

    @Test
    public void
            onProfileConnectionStateChanged_classicDeviceDisconnected_closeOpeningDialogsForIt() {
        // Test when classic device totally disconnected
        BluetoothDevice device = mock(BluetoothDevice.class);
        CachedBluetoothDevice cachedDevice = mock(CachedBluetoothDevice.class);
        LeAudioProfile profile = mock(LeAudioProfile.class);
        when(profile.isEnabled(device)).thenReturn(false);
        when(cachedDevice.getProfiles()).thenReturn(ImmutableList.of(profile));
        when(cachedDevice.isConnected()).thenReturn(false);
        when(cachedDevice.getDevice()).thenReturn(device);

        mController.onProfileConnectionStateChanged(
                cachedDevice, BluetoothAdapter.STATE_DISCONNECTED, BluetoothProfile.A2DP);
        verify(mDialogHandler).closeOpeningDialogsForNonLeaDevice(cachedDevice);
    }

    @Test
    @EnableFlags({Flags.FLAG_ENABLE_LE_AUDIO_SHARING,
            Flags.FLAG_PROMOTE_AUDIO_SHARING_FOR_SECOND_AUTO_CONNECTED_LEA_DEVICE})
    public void handleDeviceConnectedFromIntent_noDevice_doNothing() {
        Intent intent = new Intent();
        doReturn(intent).when(mActivity).getIntent();
        mController.displayPreference(mScreen);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mDeviceManager, never()).findDevice(any(BluetoothDevice.class));
        verify(mDialogHandler, never())
                .handleDeviceConnected(any(CachedBluetoothDevice.class), anyBoolean());
        verify(mActivity).finish();
    }

    @Test
    @EnableFlags({Flags.FLAG_ENABLE_LE_AUDIO_SHARING,
            Flags.FLAG_PROMOTE_AUDIO_SHARING_FOR_SECOND_AUTO_CONNECTED_LEA_DEVICE})
    public void handleDeviceClickFromIntent_handle() {
        CachedBluetoothDevice cachedDevice = mock(CachedBluetoothDevice.class);
        BluetoothDevice device = mock(BluetoothDevice.class);
        when(mDeviceManager.findDevice(device)).thenReturn(cachedDevice);
        Intent intent = new Intent();
        intent.putExtra(EXTRA_BLUETOOTH_DEVICE, device);
        doReturn(intent).when(mActivity).getIntent();
        when(mDialogHandler.handleDeviceConnected(any(), anyBoolean())).thenReturn(true);
        mController.displayPreference(mScreen);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mDialogHandler).handleDeviceConnected(cachedDevice, /* userTriggered = */ false);
        verify(mActivity, never()).finish();
    }

    @Test
    @EnableFlags({Flags.FLAG_ENABLE_LE_AUDIO_SHARING,
            Flags.FLAG_PROMOTE_AUDIO_SHARING_FOR_SECOND_AUTO_CONNECTED_LEA_DEVICE})
    public void handleDeviceClickFromIntent_noDialogToShow_finish() {
        CachedBluetoothDevice cachedDevice = mock(CachedBluetoothDevice.class);
        BluetoothDevice device = mock(BluetoothDevice.class);
        when(mDeviceManager.findDevice(device)).thenReturn(cachedDevice);
        Intent intent = new Intent();
        intent.putExtra(EXTRA_BLUETOOTH_DEVICE, device);
        doReturn(intent).when(mActivity).getIntent();
        when(mDialogHandler.handleDeviceConnected(any(), anyBoolean())).thenReturn(false);
        mController.displayPreference(mScreen);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mDialogHandler).handleDeviceConnected(cachedDevice, /* userTriggered = */ false);
        verify(mActivity).finish();
    }

    @Test
    public void testBluetoothLeBroadcastAssistantCallbacks_closeOpeningDialogsForSourceAdded() {
        CachedBluetoothDevice cachedDevice = mock(CachedBluetoothDevice.class);
        BluetoothDevice device = mock(BluetoothDevice.class);
        when(mDeviceManager.findDevice(device)).thenReturn(cachedDevice);
        // onSourceAdded will dismiss stale dialogs
        mController.mAssistantCallback.onSourceAdded(device, /* sourceId= */
                1, /* reason= */ 1);

        verify(mDialogHandler).closeOpeningDialogsForLeaDevice(cachedDevice);
    }

    @Test
    public void testBluetoothLeBroadcastAssistantCallbacks_doNothing() {
        BluetoothDevice device = mock(BluetoothDevice.class);
        mController.mAssistantCallback.onSearchStarted(/* reason= */ 1);
        mController.mAssistantCallback.onSearchStartFailed(/* reason= */ 1);
        mController.mAssistantCallback.onSearchStopped(/* reason= */ 1);
        mController.mAssistantCallback.onSearchStopFailed(/* reason= */ 1);
        BluetoothLeBroadcastReceiveState state = mock(BluetoothLeBroadcastReceiveState.class);
        mController.mAssistantCallback.onReceiveStateChanged(device, /* sourceId= */ 1, state);
        mController.mAssistantCallback.onSourceModified(device, /* sourceId= */ 1, /* reason= */ 1);
        mController.mAssistantCallback.onSourceModifyFailed(device, /* sourceId= */ 1, /* reason= */
                1);
        BluetoothLeBroadcastMetadata metadata = mock(BluetoothLeBroadcastMetadata.class);
        mController.mAssistantCallback.onSourceFound(metadata);
        mController.mAssistantCallback.onSourceLost(/* broadcastId= */ 1);
        shadowOf(Looper.getMainLooper()).idle();

        // Above callbacks won't dismiss stale dialogs
        verifyNoInteractions(mDialogHandler);
    }

    @Test
    public void onBluetoothStateChanged_stateOn_doNothing() {
        mController.onBluetoothStateChanged(BluetoothAdapter.STATE_ON);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mActivity, never()).finish();
    }

    @Test
    public void onBluetoothStateChanged_stateOff_finish() {
        mController.onBluetoothStateChanged(BluetoothAdapter.STATE_OFF);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mActivity).finish();
    }
}
